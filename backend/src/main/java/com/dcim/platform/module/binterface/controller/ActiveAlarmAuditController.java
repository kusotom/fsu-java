package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.binterface.dto.ActiveAlarmAuditRecordResponse;
import com.dcim.platform.module.binterface.dto.ActiveAlarmAuditStatusResponse;
import com.dcim.platform.module.binterface.entity.ActiveAlarmAuditRecordEntity;
import com.dcim.platform.module.binterface.service.ActiveAlarmAuditRecordService;
import com.dcim.platform.module.binterface.service.ActiveAlarmAuditStatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 活动告警审计查询 Controller (BIF-P4-022, BIF-P4-023)。
 *
 * <p>只读接口，不触发审计、不访问 FSU、不执行 SET、不修改 alarm_record。</p>
 */
@RestController
@RequestMapping("/api/binterface/active-alarm-audit")
public class ActiveAlarmAuditController {

    private final ActiveAlarmAuditStatusService statusService;
    private final ActiveAlarmAuditRecordService recordService;

    public ActiveAlarmAuditController(ActiveAlarmAuditStatusService statusService,
                                       ActiveAlarmAuditRecordService recordService) {
        this.statusService = statusService;
        this.recordService = recordService;
    }

    @GetMapping("/status")
    public ApiResponse<ActiveAlarmAuditStatusResponse> status() {
        return ApiResponse.success(statusService.getStatus());
    }

    @GetMapping("/latest")
    public ApiResponse<ActiveAlarmAuditRecordResponse> latest(
            @RequestParam(value = "fsuCode", required = false) String fsuCode) {
        Optional<ActiveAlarmAuditRecordEntity> record;
        if (fsuCode != null && !fsuCode.trim().isEmpty()) {
            record = recordService.findLatestByFsuCode(fsuCode.trim());
        } else {
            record = recordService.findLatest();
        }
        return record.map(ActiveAlarmAuditRecordResponse::from)
                .map(ApiResponse::success)
                .orElse(ApiResponse.success(null));
    }

    @GetMapping("/history")
    public ApiResponse<List<ActiveAlarmAuditRecordResponse>> history(
            @RequestParam(value = "fsuCode", required = false) String fsuCode,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // cap size to avoid large fetches
        int cappedSize = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, cappedSize);
        Page<ActiveAlarmAuditRecordEntity> result;
        if (fsuCode != null && !fsuCode.trim().isEmpty()) {
            result = recordService.findHistory(fsuCode.trim(), pageable);
        } else {
            result = recordService.findHistory(null, pageable);
        }
        List<ActiveAlarmAuditRecordResponse> list = result.getContent().stream()
                .map(ActiveAlarmAuditRecordResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(list);
    }
}
