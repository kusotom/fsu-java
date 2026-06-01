package com.dcim.platform.common.security.audit;

import com.dcim.platform.common.security.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * BE-AUTH-P0-001: 审计日志服务.
 * 异步记录操作审计事件。当前为同步写入，生产环境可改为 @Async + 队列.
 */
@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** 记录权限拒绝 */
    public void logPermissionDenied(Long userId, String username, String resourcePath,
                                    String permissionCode, String reason) {
        save(userId, username, "PERMISSION_DENIED", "api", resourcePath,
                null, null, permissionCode, false, reason);
    }

    /** 记录 raw XML 查看 */
    public void logRawXmlAccess(Long messageLogId, String fsuCode) {
        RequestContext ctx = RequestContext.getCurrent();
        save(ctx != null ? ctx.getUserId() : null, ctx != null ? ctx.getUsername() : null,
                "RAW_XML_VIEW", "message_log", String.valueOf(messageLogId),
                null, fsuCode, "protocol:raw:view", true, null);
    }

    /** 记录 raw XML 下载 */
    public void logRawXmlDownload(Long messageLogId, String fsuCode) {
        RequestContext ctx = RequestContext.getCurrent();
        save(ctx != null ? ctx.getUserId() : null, ctx != null ? ctx.getUsername() : null,
                "RAW_XML_DOWNLOAD", "message_log", String.valueOf(messageLogId),
                null, fsuCode, "protocol:raw:download", true, null);
    }

    /** 记录 run-once 执行 */
    public void logRunOnce(String commandCode, String fsuCode, boolean success, String reason) {
        RequestContext ctx = RequestContext.getCurrent();
        save(ctx != null ? ctx.getUserId() : null, ctx != null ? ctx.getUsername() : null,
                "RUN_ONCE", "run_once", commandCode,
                null, fsuCode, "protocol:runonce:readonly", success, reason);
    }

    /** 记录 SET 类命令拦截 */
    public void logSetCommandBlocked(String commandCode, String fsuCode, String reason) {
        RequestContext ctx = RequestContext.getCurrent();
        save(ctx != null ? ctx.getUserId() : null, ctx != null ? ctx.getUsername() : null,
                "SET_COMMAND_BLOCKED", "set_command", commandCode,
                null, fsuCode, "protocol:set", false, reason);
    }

    private void save(Long userId, String username, String action, String resourceType,
                      String resourceId, Long stationId, String fsuCode,
                      String permissionCode, boolean allowed, String reason) {
        try {
            AuditLogEntity e = AuditLogEntity.create(userId, username, null,
                    action, resourceType, resourceId,
                    stationId, fsuCode, permissionCode, allowed, reason, null);
            repository.save(e);
        } catch (Exception ex) {
            log.warn("审计日志写入失败: action={}, reason={}", action, ex.getMessage());
        }
    }
}
