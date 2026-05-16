package com.dcim.platform.module.resource.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.resource.entity.SiteEntity;
import com.dcim.platform.module.resource.service.SiteService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService service;

    public SiteController(SiteService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<SiteEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<SiteEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    public ApiResponse<SiteEntity> create(@RequestBody SiteEntity entity) {
        return ApiResponse.success(service.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<SiteEntity> update(@PathVariable Long id, @RequestBody SiteEntity entity) {
        return ApiResponse.success(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ApiResponse.success();
    }
}
