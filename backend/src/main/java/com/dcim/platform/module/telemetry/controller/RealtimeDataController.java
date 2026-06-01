package com.dcim.platform.module.telemetry.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.binterface.dto.BInterfaceFrontendDtos.RealtimePointDto;
import com.dcim.platform.module.telemetry.service.RealtimeDataService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/telemetry/realtime")
@RequirePermission(Permissions.REALTIME_VIEW)
public class RealtimeDataController {

    private final RealtimeDataService service;

    public RealtimeDataController(RealtimeDataService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<RealtimePointDto>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<RealtimePointDto> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
