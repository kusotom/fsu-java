package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.common.security.audit.AuditLogService;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.service.BInterfaceMessageLogQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * B接口报文日志查询与清理接口。
 *
 * 查询 BASE: /api/b-interface/message-logs
 */
@RestController
@RequestMapping("/api/b-interface/message-logs")
@RequirePermission(Permissions.PROTOCOL_RAW_VIEW)
public class BInterfaceMessageLogController {

    private static final Logger log = LoggerFactory.getLogger(BInterfaceMessageLogController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final BInterfaceMessageLogQueryService queryService;
    private final BInterfaceMessageLogService logService;
    private final AuditLogService auditLogService;
    private final DataScopeService dataScopeService;

    public BInterfaceMessageLogController(BInterfaceMessageLogQueryService queryService,
                                           BInterfaceMessageLogService logService,
                                           AuditLogService auditLogService,
                                           DataScopeService dataScopeService) {
        this.queryService = queryService;
        this.logService = logService;
        this.auditLogService = auditLogService;
        this.dataScopeService = dataScopeService;
    }

    // ===== 旧接口（保持兼容） =====

    /**
     * 查询全部报文日志（不分页，保留向后兼容）。
     *
     * @deprecated 建议使用 GET /query 分页接口
     */
    @Deprecated
    @GetMapping
    public ApiResponse<List<BInterfaceMessageLogEntity>> list() {
        return ApiResponse.success(queryService.list());
    }

    /**
     * 按 ID 查询单条报文日志。记录 raw XML 查看审计 + scope 校验.
     */
    @GetMapping("/{id}")
    public ApiResponse<BInterfaceMessageLogEntity> getById(@PathVariable Long id) {
        BInterfaceMessageLogEntity entity = queryService.getById(id);
        if (entity == null) return ApiResponse.success(null);
        // BE-AUTH-P0-FIX-002: scope 校验
        if (!canAccessRawXmlRecord(entity)) {
            auditLogService.logPermissionDenied(
                com.dcim.platform.common.security.RequestContext.getCurrent() != null ?
                    com.dcim.platform.common.security.RequestContext.getCurrent().getUserId() : null,
                com.dcim.platform.common.security.RequestContext.getCurrent() != null ?
                    com.dcim.platform.common.security.RequestContext.getCurrent().getUsername() : null,
                "/api/b-interface/message-logs/" + id, "protocol:raw:view",
                "fsuScope不匹配: fsuCode=" + entity.getFsuCode());
            return ApiResponse.fail(403, "无权访问此报文: fsuScope不匹配");
        }
        auditLogService.logRawXmlAccess(id, entity.getFsuCode());
        return ApiResponse.success(entity);
    }

    /** BE-AUTH-P0-FIX-002: 校验当前用户是否有权访问此 raw XML 记录.
     *  empty scope → default-deny; null fsuCode → default-deny for non-super_admin. */
    private boolean canAccessRawXmlRecord(BInterfaceMessageLogEntity entity) {
        com.dcim.platform.common.security.RequestContext ctx =
            com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null) return false;

        // super_admin 可查看全部 (包括 null fsuCode)
        if (ctx.isSuperAdmin()) return true;

        // null fsuCode 的敏感记录: 非 super_admin 默认拒绝
        if (entity.getFsuCode() == null) return false;

        // admin-like (admin/platform_admin) 可查看全部有 fsuCode 的记录
        if (ctx.isAdminLike()) return true;

        java.util.Set<String> allowed = dataScopeService.getAllowedFsuCodes();
        // allowed == null → DataScope 判定为 admin-like 全部可见 (已在上面处理)
        if (allowed == null) return true;
        // BE-AUTH-P0-FIX-002: empty scope = default-deny
        if (allowed.isEmpty()) return false;
        return allowed.contains(entity.getFsuCode());
    }

    /**
     * 下载 raw XML 报文。需额外 protocol:raw:download 权限 (AND raw:view).
     */
    @GetMapping("/{id}/download")
    @RequirePermission(value = {Permissions.PROTOCOL_RAW_VIEW, Permissions.PROTOCOL_RAW_DOWNLOAD}, requireAll = true)
    public ResponseEntity<ByteArrayResource> downloadXml(@PathVariable Long id) {
        BInterfaceMessageLogEntity entity = queryService.getById(id);
        if (entity == null || entity.getRawMessage() == null) {
            return ResponseEntity.notFound().build();
        }
        // BE-AUTH-P0-FIX-002: scope 校验
        if (!canAccessRawXmlRecord(entity)) {
            auditLogService.logPermissionDenied(
                com.dcim.platform.common.security.RequestContext.getCurrent() != null ?
                    com.dcim.platform.common.security.RequestContext.getCurrent().getUserId() : null,
                com.dcim.platform.common.security.RequestContext.getCurrent() != null ?
                    com.dcim.platform.common.security.RequestContext.getCurrent().getUsername() : null,
                "/api/b-interface/message-logs/" + id + "/download", "protocol:raw:download",
                "fsuScope不匹配: fsuCode=" + entity.getFsuCode());
            return ResponseEntity.status(403).build();
        }
        auditLogService.logRawXmlDownload(id, entity.getFsuCode());
        byte[] bytes = entity.getRawMessage().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(bytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=message-" + id + ".xml")
                .contentType(MediaType.APPLICATION_XML)
                .body(resource);
    }

    // ===== LANDING-010: 分页查询 =====

    /**
     * 分页多条件查询报文日志。
     *
     * @param direction   方向过滤（INBOUND/OUTBOUND），可选
     * @param command     命令过滤（LOGIN/HEARTBEAT/...），可选
     * @param fsuCode     FSU 编码过滤，可选
     * @param messageType 消息类型过滤（SOAP/XML/RAW），可选
     * @param page        页码（默认 0）
     * @param size        每页大小（默认 20，最大 100）
     */
    @GetMapping("/query")
    public ApiResponse<Page<BInterfaceMessageLogEntity>> query(
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String command,
            @RequestParam(required = false) String fsuCode,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        int p = page != null ? page : DEFAULT_PAGE;
        int s = size != null ? Math.min(size, MAX_SIZE) : DEFAULT_SIZE;

        var pageable = PageRequest.of(p, s);
        Page<BInterfaceMessageLogEntity> result = logService.query(direction, command, fsuCode, messageType, pageable);
        return ApiResponse.success(result);
    }

    // ===== LANDING-010: 日志清理 =====

    /**
     * 按天数清理历史报文日志（必须显式调用）。
     *
     * 仅删除 binterface_message_log 表记录，不影响任何业务数据。
     *
     * @param olderThanDays 保留天数（必须 > 0）
     * @return 删除的记录数
     */
    @DeleteMapping("/cleanup")
    @RequirePermission(Permissions.PROTOCOL_RAW_CLEANUP)
    public ApiResponse<Long> cleanupByDays(
            @RequestParam(required = false, defaultValue = "0") int olderThanDays) {

        if (olderThanDays <= 0) {
            return ApiResponse.fail("days 必须大于 0，当前值: " + olderThanDays);
        }

        try {
            long deleted = logService.cleanOlderThanDays(olderThanDays);
            log.info("报文日志清理: 删除 {} 条记录 (olderThanDays={})", deleted, olderThanDays);
            return ApiResponse.success(deleted);
        } catch (Exception e) {
            log.error("报文日志清理失败 olderThanDays={}", olderThanDays, e);
            return ApiResponse.fail("清理失败: " + e.getMessage());
        }
    }
}
