package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * SUREADY 注册状态验证服务 (2024 标准, Code=103)。
 *
 * <p>BIF-P4-017: FSU 发送 SUREADY 通知 SC 注册准备就绪。
 * SC 验证 SUID 并更新在线/注册状态。</p>
 */
@Service
public class SureadyService {

    private static final Logger log = LoggerFactory.getLogger(SureadyService.class);

    private final FsuDeviceRepository fsuDeviceRepository;
    private final BInterfaceFsuStatusRepository fsuStatusRepository;

    public SureadyService(FsuDeviceRepository fsuDeviceRepository,
                           BInterfaceFsuStatusRepository fsuStatusRepository) {
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.fsuStatusRepository = fsuStatusRepository;
    }

    public SureadyResult execute(String suid) {
        if (suid == null || suid.trim().isEmpty())
            return SureadyResult.fail("2001", "缺少 SUID");
        suid = suid.trim();

        Optional<FsuDeviceEntity> devOpt = fsuDeviceRepository.findByFsuCode(suid);
        if (devOpt.isEmpty()) {
            log.warn("SUREADY SUID 不存在: {}", suid);
            return SureadyResult.fail("SUID_ERROR", "SUID 不存在: " + suid, suid);
        }

        try {
            // 更新 FSU 设备状态
            FsuDeviceEntity dev = devOpt.get();
            if (!"ONLINE".equals(dev.getStatus())) {
                dev.setStatus("ONLINE");
                dev.setLastOnlineTime(LocalDateTime.now());
                fsuDeviceRepository.save(dev);
            }

            // 更新 BInterface 状态
            Optional<BInterfaceFsuStatusEntity> sOpt = fsuStatusRepository.findByFsuCode(suid);
            BInterfaceFsuStatusEntity status;
            if (sOpt.isPresent()) {
                status = sOpt.get();
                status.setOnlineStatus("ONLINE");
                status.setLoginStatus("LOGIN");
                status.setLastHeartbeat(LocalDateTime.now());
            } else {
                status = new BInterfaceFsuStatusEntity();
                status.setFsuId(dev.getId());
                status.setFsuCode(suid);
                status.setOnlineStatus("ONLINE");
                status.setLoginStatus("LOGIN");
                status.setLastHeartbeat(LocalDateTime.now());
            }
            fsuStatusRepository.save(status);

            log.info("SUREADY 成功: suid={}", suid);
            return SureadyResult.ready(suid);
        } catch (Exception e) {
            log.error("SUREADY 状态更新失败: suid={}", suid, e);
            return SureadyResult.fail("5001", "状态更新失败", suid);
        }
    }
}
