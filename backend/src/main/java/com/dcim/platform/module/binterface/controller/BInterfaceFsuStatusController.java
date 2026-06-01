package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.service.BInterfaceFsuStatusQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/b-interface/fsu-status")
@RequirePermission(Permissions.FSU_VIEW)
public class BInterfaceFsuStatusController {

    private final BInterfaceFsuStatusQueryService service;

    public BInterfaceFsuStatusController(BInterfaceFsuStatusQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<BInterfaceFsuStatusEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<BInterfaceFsuStatusEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
