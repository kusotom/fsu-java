package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.common.security.audit.AuditLogService;
import com.dcim.platform.module.binterface.dto.runonce.ReadOnlyRunOnceDtos.*;
import com.dcim.platform.module.binterface.service.BInterface2016ReadOnlyRunOnceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * FAST-REALDATA-001: B接口2016 只读 run-once Controller。
 *
 * 提供人工触发的 GET_FSUINFO / GET_DATA probe 端点。
 * 所有端点要求 operator + reason + confirmReadOnly=true。
 */
@RestController
@RequestMapping("/api/b-interface/2016/read-only")
@RequirePermission(Permissions.PROTOCOL_RUNONCE_READONLY)
public class BInterface2016ReadOnlyRunOnceController {

    private static final Logger log = LoggerFactory.getLogger(BInterface2016ReadOnlyRunOnceController.class);

    private final BInterface2016ReadOnlyRunOnceService runOnceService;
    private final AuditLogService auditLogService;

    public BInterface2016ReadOnlyRunOnceController(BInterface2016ReadOnlyRunOnceService runOnceService,
                                                    AuditLogService auditLogService) {
        this.runOnceService = runOnceService;
        this.auditLogService = auditLogService;
    }

    /**
     * 人工触发 GET_FSUINFO run-once。
     * 使用 B接口2016 Code=1701 查询 FSU CPU/MEM 运行状态。
     */
    @PostMapping("/fsu-info/run-once")
    public ResponseEntity<ApiResponse<FsuInfoRunOnceResponse>> runFsuInfoOnce(
            @RequestBody FsuInfoRunOnceRequest request) {
        log.info("GET_FSUINFO run-once: operator={}, fsuCode={}, reason={}",
                request.getOperator(), request.getFsuCode(), request.getReason());

        try {
            FsuInfoRunOnceResponse resp = runOnceService.runFsuInfoOnce(request);
            auditLogService.logRunOnce("GET_FSUINFO", request.getFsuCode(), resp.isSuccess(), resp.getResultDesc());
            return ResponseEntity.ok(ApiResponse.success(resp));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("GET_FSUINFO run-once 参数校验失败: {}", e.getMessage());
            FsuInfoRunOnceResponse err = new FsuInfoRunOnceResponse();
            err.setSuccess(false);
            err.setResultCode("VALIDATION_FAILED");
            err.setResultDesc(e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, e.getMessage()));
        } catch (Exception e) {
            log.error("GET_FSUINFO run-once 异常", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(500, "服务异常: " + e.getMessage()));
        }
    }

    /**
     * 人工触发 GET_DATA probe（带标准字典 SignalID 推测）。
     * 使用 B接口2016 Code=401，DeviceType 从 DeviceID 推断。
     */
    @PostMapping("/get-data/probe")
    public ResponseEntity<ApiResponse<GetDataProbeResponse>> runGetDataProbe(
            @RequestBody GetDataProbeRequest request) {
        log.info("GET_DATA probe: operator={}, fsuCode={}, devices={}, reason={}",
                request.getOperator(), request.getFsuCode(),
                request.getDeviceIds() != null ? request.getDeviceIds().size() : 0,
                request.getReason());

        try {
            GetDataProbeResponse resp = runOnceService.runGetDataProbe(request);
            auditLogService.logRunOnce("GET_DATA", request.getFsuCode(), resp.isSuccess(), resp.getResultDesc());
            return ResponseEntity.ok(ApiResponse.success(resp));
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("GET_DATA probe 参数校验失败: {}", e.getMessage());
            GetDataProbeResponse err = new GetDataProbeResponse();
            err.setSuccess(false);
            err.setFsuCode(request.getFsuCode());
            err.setResultCode("VALIDATION_FAILED");
            err.setResultDesc(e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.fail(400, e.getMessage()));
        } catch (Exception e) {
            log.error("GET_DATA probe 异常", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(500, "服务异常: " + e.getMessage()));
        }
    }
}
