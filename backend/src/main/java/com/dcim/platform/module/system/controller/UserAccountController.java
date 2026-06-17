package com.dcim.platform.module.system.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.module.system.entity.UserAccountEntity;
import com.dcim.platform.module.system.service.UserAccountService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/system/users")
@RequirePermission(Permissions.USER_VIEW)
public class UserAccountController {

    private final UserAccountService service;

    public UserAccountController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<UserAccountEntity>> list() {
        return ApiResponse.success(service.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<UserAccountEntity> getById(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @PostMapping
    @RequirePermission(Permissions.USER_CREATE)
    public ApiResponse<UserAccountEntity> create(@RequestBody UserAccountEntity entity) {
        return ApiResponse.success(service.create(entity));
    }

    @PutMapping("/{id}")
    @RequirePermission(Permissions.USER_UPDATE)
    public ApiResponse<UserAccountEntity> update(@PathVariable Long id, @RequestBody UserAccountEntity entity) {
        return ApiResponse.success(service.update(id, entity));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(Permissions.USER_DELETE)
    public ApiResponse<Void> delete(@PathVariable Long id) {
        service.deleteById(id);
        return ApiResponse.success();
    }
}
