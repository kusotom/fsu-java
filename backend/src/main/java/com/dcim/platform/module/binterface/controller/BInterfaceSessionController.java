package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.service.BInterfaceSessionQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/b-interface/sessions")
@RequirePermission(Permissions.FSU_VIEW)
public class BInterfaceSessionController {

    private final BInterfaceSessionQueryService service;

    public BInterfaceSessionController(BInterfaceSessionQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<BInterfaceSessionEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<BInterfaceSessionEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
