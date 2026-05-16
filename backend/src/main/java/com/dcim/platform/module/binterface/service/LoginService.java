package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * FSU 登录注册与 Session 管理服务。
 *
 * 职责：
 * <ul>
 *   <li>校验 FSU 标识（基于 fsu_device 表）</li>
 *   <li>建立登录 Session</li>
 *   <li>更新 FSU 在线状态</li>
 *   <li>提供 Session 查询（供后续 HEARTBEAT 等命令复用）</li>
 * </ul>
 *
 * <b>线程安全：</b>依赖 JPA Repository，由容器保证事务安全。
 *
 * @see LoginResult
 * @see LoginCommandHandler
 */
@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    /** Session 过期秒数（协议默认值：3600 秒） */
    public static final int DEFAULT_EXPIRE_SECONDS = 3600;

    private final FsuDeviceRepository fsuDeviceRepository;

    private final BInterfaceFsuStatusRepository fsuStatusRepository;

    private final BInterfaceSessionRepository sessionRepository;

    public LoginService(FsuDeviceRepository fsuDeviceRepository,
                        BInterfaceFsuStatusRepository fsuStatusRepository,
                        BInterfaceSessionRepository sessionRepository) {
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.fsuStatusRepository = fsuStatusRepository;
        this.sessionRepository = sessionRepository;
    }

    /**
     * FSU 登录处理。
     *
     * @param fsuCode    FSU 编码（来自 Info.FSUCode）
     * @param remoteAddr 请求来源地址（可选，用于记录）
     * @return 登录结果
     */
    @Transactional
    public LoginResult login(String fsuCode, String remoteAddr) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            log.warn("LOGIN 失败: FSUCode 为空");
            return LoginResult.fail("2001", "缺少 FSUCode");
        }

        fsuCode = fsuCode.trim();

        // 查找 FSU 设备
        Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepository.findByFsuCode(fsuCode);
        if (fsuOpt.isEmpty()) {
            log.warn("LOGIN 失败: FSU 未注册 fsuCode={}", fsuCode);
            return LoginResult.fail("1002", "FSU 未注册: " + fsuCode);
        }

        FsuDeviceEntity fsu = fsuOpt.get();
        Long fsuId = fsu.getId();

        try {
            // 生成 Session
            String sessionId = generateSessionId(fsuCode);
            LocalDateTime now = LocalDateTime.now();

            // 保存 Session
            BInterfaceSessionEntity session = createSession(fsuId, fsuCode, sessionId, remoteAddr, now);
            sessionRepository.save(session);

            // 更新 FSU 在线状态
            updateFsuStatus(fsuId, fsuCode, sessionId, now);

            // 更新 FSU 设备注册时间
            updateFsuDevice(fsu, now);

            log.info("LOGIN 成功: fsuCode={}, sessionId={}", fsuCode, sessionId);

            return LoginResult.success(fsuCode, sessionId, now.atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.error("LOGIN 处理异常: fsuCode={}", fsuCode, e);
            return LoginResult.fail("5001", "登录处理异常: " + e.getMessage());
        }
    }

    /**
     * 查询 FSU 的当前状态。
     */
    public Optional<BInterfaceFsuStatusEntity> getStatus(String fsuCode) {
        return fsuStatusRepository.findByFsuCode(fsuCode);
    }

    /**
     * 检查 FSU 是否已登录。
     */
    public boolean isLoggedIn(String fsuCode) {
        return fsuStatusRepository.findByFsuCode(fsuCode)
                .map(s -> "LOGIN".equals(s.getLoginStatus()))
                .orElse(false);
    }

    /**
     * 查询活动 Session。
     */
    public Optional<BInterfaceSessionEntity> getActiveSession(String fsuCode) {
        return fsuDeviceRepository.findByFsuCode(fsuCode)
                .flatMap(fsu -> sessionRepository.findByFsuIdAndStatus(fsu.getId(), "ACTIVE"));
    }

    /**
     * 更新 FSU 最后活跃时间（供 HEARTBEAT 后续调用）。
     */
    @Transactional
    public void updateLastSeen(String fsuCode) {
        LocalDateTime now = LocalDateTime.now();
        fsuStatusRepository.findByFsuCode(fsuCode).ifPresent(status -> {
            status.setLastHeartbeat(now);
            status.setOnlineStatus("ONLINE");
            status.setUpdatedAt(now);
            fsuStatusRepository.save(status);
        });

        fsuDeviceRepository.findByFsuCode(fsuCode).ifPresent(fsu -> {
            fsu.setLastOnlineTime(now);
            fsu.setUpdatedAt(now);
            fsuDeviceRepository.save(fsu);
        });
    }

    /**
     * 清除 Session（登出）。
     */
    @Transactional
    public void clearSession(String fsuCode) {
        LocalDateTime now = LocalDateTime.now();
        fsuDeviceRepository.findByFsuCode(fsuCode).ifPresent(fsu -> {
            sessionRepository.findByFsuIdAndStatus(fsu.getId(), "ACTIVE").ifPresent(session -> {
                session.setStatus("LOGOUT");
                session.setLogoutTime(now);
                session.setUpdatedAt(now);
                sessionRepository.save(session);
            });
        });

        fsuStatusRepository.findByFsuCode(fsuCode).ifPresent(status -> {
            status.setLoginStatus("LOGOUT");
            status.setOnlineStatus("OFFLINE");
            status.setUpdatedAt(now);
            fsuStatusRepository.save(status);
        });
    }

    // ==================== 内部方法 ====================

    private String generateSessionId(String fsuCode) {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "SESSION-" + fsuCode + "-" + uuid;
    }

    private BInterfaceSessionEntity createSession(Long fsuId, String fsuCode,
                                                   String sessionId, String remoteAddr,
                                                   LocalDateTime now) {
        BInterfaceSessionEntity session = new BInterfaceSessionEntity();
        session.setFsuId(fsuId);
        session.setFsuCode(fsuCode);
        session.setSessionId(sessionId);
        session.setLoginTime(now);
        session.setLastActiveTime(now);
        session.setStatus("ACTIVE");
        session.setRemoteAddr(remoteAddr);
        session.setExpireSeconds(DEFAULT_EXPIRE_SECONDS);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        return session;
    }

    private void updateFsuStatus(Long fsuId, String fsuCode, String sessionId, LocalDateTime now) {
        Optional<BInterfaceFsuStatusEntity> existing = fsuStatusRepository.findByFsuCode(fsuCode);
        BInterfaceFsuStatusEntity status = existing.orElseGet(() -> {
            BInterfaceFsuStatusEntity s = new BInterfaceFsuStatusEntity();
            s.setFsuId(fsuId);
            s.setFsuCode(fsuCode);
            s.setCreatedAt(now);
            return s;
        });
        status.setLoginStatus("LOGIN");
        status.setOnlineStatus("ONLINE");
        status.setLastLoginTime(now);
        status.setSessionId(sessionId);
        status.setHeartbeatMissCount(0);
        status.setUpdatedAt(now);
        fsuStatusRepository.save(status);
    }

    private void updateFsuDevice(FsuDeviceEntity fsu, LocalDateTime now) {
        if (fsu.getRegisterTime() == null) {
            fsu.setRegisterTime(now);
        }
        fsu.setLastOnlineTime(now);
        fsu.setUpdatedAt(now);
        fsuDeviceRepository.save(fsu);
    }
}
