package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.service.BInterfaceMessageLogQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * B接口报文日志查询与清理接口。
 *
 * 查询 BASE: /api/b-interface/message-logs
 */
@RestController
@RequestMapping("/api/b-interface/message-logs")
public class BInterfaceMessageLogController {

    private static final Logger log = LoggerFactory.getLogger(BInterfaceMessageLogController.class);

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final BInterfaceMessageLogQueryService queryService;
    private final BInterfaceMessageLogService logService;

    public BInterfaceMessageLogController(BInterfaceMessageLogQueryService queryService,
                                           BInterfaceMessageLogService logService) {
        this.queryService = queryService;
        this.logService = logService;
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
     * 按 ID 查询单条报文日志。
     */
    @GetMapping("/{id}")
    public ApiResponse<BInterfaceMessageLogEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(queryService.getById(id));
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
