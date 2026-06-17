package com.dcim.platform.module.resource.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.service.FsuDeviceService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/fsu-devices")
@RequirePermission(Permissions.FSU_VIEW)
public class FsuDeviceController {

    private final FsuDeviceService service;

    public FsuDeviceController(FsuDeviceService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<FsuDeviceEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<FsuDeviceEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    @RequirePermission(Permissions.FSU_CREATE)
    public ApiResponse<FsuDeviceEntity> create(@RequestBody FsuDeviceEntity entity) {
        return ApiResponse.success(service.create(entity));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permissions.FSU_UPDATE)
    public ApiResponse<FsuDeviceEntity> update(@PathVariable Long id, @RequestBody FsuDeviceEntity entity) {
        return ApiResponse.success(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permissions.FSU_DELETE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ApiResponse.success();
    }
}
