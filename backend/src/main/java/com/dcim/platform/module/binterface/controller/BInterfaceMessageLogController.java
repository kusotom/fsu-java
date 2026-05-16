package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.service.BInterfaceMessageLogQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/b-interface/message-logs")
public class BInterfaceMessageLogController {

    private final BInterfaceMessageLogQueryService service;

    public BInterfaceMessageLogController(BInterfaceMessageLogQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<BInterfaceMessageLogEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<BInterfaceMessageLogEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
