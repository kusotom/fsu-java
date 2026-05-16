package com.dcim.platform.module.telemetry.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.service.RealtimeDataService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/telemetry/realtime")
public class RealtimeDataController {

    private final RealtimeDataService service;

    public RealtimeDataController(RealtimeDataService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<RealtimeDataEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<RealtimeDataEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
