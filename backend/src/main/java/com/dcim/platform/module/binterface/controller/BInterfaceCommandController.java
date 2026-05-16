package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.module.binterface.entity.BInterfaceCommandEntity;
import com.dcim.platform.module.binterface.service.BInterfaceCommandService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/b-interface/commands")
public class BInterfaceCommandController {

    private final BInterfaceCommandService service;

    public BInterfaceCommandController(BInterfaceCommandService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<BInterfaceCommandEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<BInterfaceCommandEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    // 不提供 safeEnabled 更新接口
}
