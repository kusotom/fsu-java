package com.dcim.platform.module.alarm.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.service.AlarmRecordService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alarms")
public class AlarmRecordController {

    private final AlarmRecordService service;

    public AlarmRecordController(AlarmRecordService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<AlarmRecordEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<AlarmRecordEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @GetMapping("/status/{status}")
    public ApiResponse<List<AlarmRecordEntity>> listByStatus(@PathVariable String status) {
        return ApiResponse.success(service.listByStatus(status));
    }
}
