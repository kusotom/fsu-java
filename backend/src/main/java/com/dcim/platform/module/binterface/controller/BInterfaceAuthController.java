package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto;
import com.dcim.platform.module.binterface.service.BInterfaceFsuRegistrationContextService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * BIF2016-AUTH-002: FSU 注册认证只读查询 Controller。
 *
 * 提供 FSU LOGIN 注册上下文的只读查询端点。
 * 不访问真实 FSU，不写数据库。
 */
@RestController
@RequestMapping("/api/b-interface/2016/auth")
@RequirePermission(Permissions.FSU_VIEW)
public class BInterfaceAuthController {

    private static final Logger log = LoggerFactory.getLogger(BInterfaceAuthController.class);

    private final BInterfaceFsuRegistrationContextService contextService;

    public BInterfaceAuthController(BInterfaceFsuRegistrationContextService contextService) {
        this.contextService = contextService;
    }

    /**
     * 查询 FSU 注册上下文（只读，来自 message log + session/status）。
     */
    @GetMapping("/registration-context/{fsuCode}")
    public ResponseEntity<ApiResponse<BInterfaceFsuRegistrationContextDto>> getRegistrationContext(
            @PathVariable String fsuCode) {
        log.info("查询注册上下文: fsuCode={}", fsuCode);

        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail(400, "fsuCode 不能为空"));
        }

        try {
            BInterfaceFsuRegistrationContextDto ctx = contextService.buildContext(fsuCode.trim());
            return ResponseEntity.ok(ApiResponse.success(ctx));
        } catch (Exception e) {
            log.error("查询注册上下文异常: fsuCode={}", fsuCode, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail(500, "查询异常: " + e.getMessage()));
        }
    }
}
