package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.binterface.entity.BInterfaceCallRecordEntity;
import com.dcim.platform.module.binterface.service.BInterfaceCallRecordQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/b-interface/call-records")
public class BInterfaceCallRecordController {

    private final BInterfaceCallRecordQueryService service;

    public BInterfaceCallRecordController(BInterfaceCallRecordQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<BInterfaceCallRecordEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<BInterfaceCallRecordEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
