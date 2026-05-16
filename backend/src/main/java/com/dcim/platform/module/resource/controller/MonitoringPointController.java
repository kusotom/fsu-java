package com.dcim.platform.module.resource.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.service.MonitoringPointService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/monitoring-points")
public class MonitoringPointController {

    private final MonitoringPointService service;

    public MonitoringPointController(MonitoringPointService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<MonitoringPointEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<MonitoringPointEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    public ApiResponse<MonitoringPointEntity> create(@RequestBody MonitoringPointEntity entity) {
        return ApiResponse.success(service.create(entity));
    }

    @PutMapping("/{id}")
    public ApiResponse<MonitoringPointEntity> update(@PathVariable Long id, @RequestBody MonitoringPointEntity entity) {
        return ApiResponse.success(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ApiResponse.success();
    }
}
