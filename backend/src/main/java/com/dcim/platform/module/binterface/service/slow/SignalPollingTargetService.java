package com.dcim.platform.module.binterface.service.slow;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 慢数据轮询 Signal 目标配置服务。
 *
 * <p>基于已有 {@link MonitoringPointEntity} 查询某个 FSU 下需要轮询的 SignalID 列表。
 * 通过 fsuCode → fsuId → MonitoringPoint 映射完成配置读取。</p>
 *
 * <p>当前复用 monitoring_point 表的 pointCode 作为 SignalID。仅返回 status=ACTIVE 的点位。
 * 后续可扩展为独立配置表。</p>
 */
@Service
public class SignalPollingTargetService {

    private static final Logger log = LoggerFactory.getLogger(SignalPollingTargetService.class);

    private final FsuDeviceRepository fsuDeviceRepository;
    private final MonitoringPointRepository monitoringPointRepository;

    public SignalPollingTargetService(FsuDeviceRepository fsuDeviceRepository,
                                      MonitoringPointRepository monitoringPointRepository) {
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.monitoringPointRepository = monitoringPointRepository;
    }

    /**
     * 查询指定 FSU 下指定命令类型的轮询目标列表。
     *
     * @param fsuCode     FSU 编码
     * @param commandType 命令类型（GET_DATA 或 GET_THRESHOLD）
     * @return 非 null 的 target 列表，FSU 不存在或无配置时返回空列表
     */
    public List<SignalPollingTarget> findTargets(String fsuCode, BInterfacePkType commandType) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return List.of();
        }
        if (commandType != BInterfacePkType.GET_DATA && commandType != BInterfacePkType.GET_THRESHOLD) {
            return List.of();
        }

        try {
            Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepository.findByFsuCode(fsuCode.trim());
            if (fsuOpt.isEmpty()) {
                log.debug("轮询目标: FSU {} 不存在", fsuCode);
                return List.of();
            }

            Long fsuId = fsuOpt.get().getId();
            List<MonitoringPointEntity> points = monitoringPointRepository.findByFsuId(fsuId);
            if (points.isEmpty()) {
                return List.of();
            }

            return points.stream()
                    .filter(p -> "ACTIVE".equals(p.getStatus()))
                    .map(p -> new SignalPollingTarget(
                            fsuCode,
                            String.valueOf(p.getFsuId()),
                            p.getPointCode(),
                            commandType,
                            "ACTIVE".equals(p.getStatus())))
                    .toList();
        } catch (Exception e) {
            log.error("查询轮询目标异常: fsuCode={}, commandType={}", fsuCode, commandType, e);
            return List.of();
        }
    }

    /**
     * 判断指定 FSU 是否有指定命令类型的轮询目标。
     */
    public boolean hasTargets(String fsuCode, BInterfacePkType commandType) {
        return !findTargets(fsuCode, commandType).isEmpty();
    }
}
