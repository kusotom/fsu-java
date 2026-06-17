package com.dcim.platform.module.telemetry.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.telemetry.entity.HistoryDataEntity;
import com.dcim.platform.module.telemetry.service.HistoryDataService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/telemetry/history")
@RequirePermission(Permissions.REALTIME_VIEW)
public class HistoryDataController {

    private final HistoryDataService service;

    public HistoryDataController(HistoryDataService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<HistoryDataEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<HistoryDataEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @GetMapping("/query")
    public ApiResponse<List<HistoryDataEntity>> query(
            @RequestParam Long pointId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ApiResponse.success(service.queryByPoint(pointId, start, end));
    }
}
