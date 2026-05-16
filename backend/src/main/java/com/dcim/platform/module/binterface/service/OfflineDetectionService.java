package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 离线检测服务。
 *
 * <p>定时扫描所有 onlineStatus=ONLINE 的 FSU，检查 lastHeartbeat 是否超过
 * 心跳超时阈值。若超时，将 onlineStatus 置为 OFFLINE，loginStatus 置为 LOGOUT，
 * 清除对应的 ACTIVE Session，并递增 heartbeatMissCount。</p>
 *
 * <p>线程安全：依赖 JPA Repository，由容器保证事务安全。</p>
 *
 * @see BInterfaceFsuStatusEntity
 * @see LoginService#clearSession(String)
 */
@Service
public class OfflineDetectionService {

    private static final Logger log = LoggerFactory.getLogger(OfflineDetectionService.class);

    private final BInterfaceFsuStatusRepository fsuStatusRepository;

    private final BInterfaceSessionRepository sessionRepository;

    public OfflineDetectionService(BInterfaceFsuStatusRepository fsuStatusRepository,
                                    BInterfaceSessionRepository sessionRepository) {
        this.fsuStatusRepository = fsuStatusRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * 执行一次离线扫描。
     *
     * @param now                   当前时间
     * @param heartbeatTimeoutSecs 心跳超时秒数
     * @return 扫描结果
     */
    @Transactional
    public OfflineDetectionResult scanOfflineFsu(LocalDateTime now, long heartbeatTimeoutSecs) {
        List<BInterfaceFsuStatusEntity> onlineStatuses = fsuStatusRepository.findByOnlineStatus("ONLINE");
        if (onlineStatuses.isEmpty()) {
            log.debug("离线扫描: 无 ONLINE 状态的 FSU");
            return OfflineDetectionResult.empty();
        }

        List<String> offlineFsuCodes = new ArrayList<>();
        List<String> onlineFsuCodes = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        LocalDateTime cutoff = now.minusSeconds(heartbeatTimeoutSecs);

        for (BInterfaceFsuStatusEntity status : onlineStatuses) {
            try {
                if (status.getLastHeartbeat() == null) {
                    // 无心跳记录（可能刚刚通过 LOGIN 建立状态但尚未有心跳）
                    // 仍视为超时，置为 OFFLINE
                    setOffline(status);
                    offlineFsuCodes.add(status.getFsuCode());
                    log.info("离线扫描: FSU={} 无心跳记录，置为 OFFLINE", status.getFsuCode());
                } else if (status.getLastHeartbeat().isBefore(cutoff)) {
                    setOffline(status);
                    offlineFsuCodes.add(status.getFsuCode());
                    log.info("离线扫描: FSU={} 心跳超时 lastHeartbeat={}，置为 OFFLINE",
                            status.getFsuCode(), status.getLastHeartbeat());
                } else {
                    onlineFsuCodes.add(status.getFsuCode());
                }
            } catch (Exception e) {
                log.error("离线扫描处理异常: FSU={}", status.getFsuCode(), e);
                errors.add(status.getFsuCode() + ": " + e.getMessage());
            }
        }

        if (errors.isEmpty()) {
            return OfflineDetectionResult.completed(
                    onlineStatuses.size(), offlineFsuCodes, onlineFsuCodes);
        }
        return OfflineDetectionResult.completedWithErrors(
                onlineStatuses.size(), offlineFsuCodes, onlineFsuCodes, errors);
    }

    /**
     * 将单个 FSU 置为离线状态。
     * 清除 ACTIVE session、将 onlineStatus 置为 OFFLINE、loginStatus 置为 LOGOUT。
     */
    private void setOffline(BInterfaceFsuStatusEntity status) {
        LocalDateTime now = LocalDateTime.now();

        // 清除 ACTIVE session
        sessionRepository.findByFsuIdAndStatus(status.getFsuId(), "ACTIVE").ifPresent(session -> {
            session.setStatus("LOGOUT");
            session.setLogoutTime(now);
            session.setUpdatedAt(now);
            sessionRepository.save(session);
        });

        // 更新 FSU 状态
        status.setLoginStatus("LOGOUT");
        status.setOnlineStatus("OFFLINE");
        if (status.getHeartbeatMissCount() == null) {
            status.setHeartbeatMissCount(1);
        } else {
            status.setHeartbeatMissCount(status.getHeartbeatMissCount() + 1);
        }
        status.setUpdatedAt(now);
        fsuStatusRepository.save(status);
    }
}
