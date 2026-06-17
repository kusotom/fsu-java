package com.dcim.platform.module.telemetry.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.telemetry.entity.DeviceHeartbeatEntity;
import com.dcim.platform.module.telemetry.service.DeviceHeartbeatService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/telemetry/heartbeats")
@RequirePermission(Permissions.REALTIME_VIEW)
public class DeviceHeartbeatController {

    private final DeviceHeartbeatService service;

    public DeviceHeartbeatController(DeviceHeartbeatService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<DeviceHeartbeatEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceHeartbeatEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
