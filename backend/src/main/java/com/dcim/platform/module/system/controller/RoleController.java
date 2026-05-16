package com.dcim.platform.module.system.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.system.entity.RoleEntity;
import com.dcim.platform.module.system.service.RoleService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
public class RoleController {

    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<RoleEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    public ApiResponse<RoleEntity> create(@RequestBody RoleEntity entity) {
        return ApiResponse.success(service.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoleEntity> update(@PathVariable Long id, @RequestBody RoleEntity entity) {
        return ApiResponse.success(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ApiResponse.success();
    }
}
