package com.dcim.platform.module.resource.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.resource.entity.CabinetEntity;
import com.dcim.platform.module.resource.service.CabinetService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/cabinets")
@RequirePermission(Permissions.FSU_VIEW)
public class CabinetController {

    private final CabinetService service;

    public CabinetController(CabinetService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CabinetEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<CabinetEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    @RequirePermission(Permissions.FSU_CREATE)
    public ApiResponse<CabinetEntity> create(@RequestBody CabinetEntity entity) {
        return ApiResponse.success(service.create(entity));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permissions.FSU_UPDATE)
    public ApiResponse<CabinetEntity> update(@PathVariable Long id, @RequestBody CabinetEntity entity) {
        return ApiResponse.success(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permissions.FSU_DELETE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ApiResponse.success();
    }
}
