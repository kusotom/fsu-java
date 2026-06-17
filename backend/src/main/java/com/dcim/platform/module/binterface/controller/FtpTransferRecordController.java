package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.binterface.ftp.FtpTransferRecordEntity;
import com.dcim.platform.module.binterface.service.FtpTransferRecordQueryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/b-interface/ftp-records")
@RequirePermission(Permissions.FSU_VIEW)
public class FtpTransferRecordController {

    private final FtpTransferRecordQueryService service;

    public FtpTransferRecordController(FtpTransferRecordQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<FtpTransferRecordEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<FtpTransferRecordEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }
}
