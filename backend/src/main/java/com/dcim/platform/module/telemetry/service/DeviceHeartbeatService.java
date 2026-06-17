package com.dcim.platform.module.telemetry.service;

import com.dcim.platform.module.telemetry.entity.DeviceHeartbeatEntity;
import com.dcim.platform.module.telemetry.repository.DeviceHeartbeatRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DeviceHeartbeatService {

    private final DeviceHeartbeatRepository repository;
    private final DataScopeService dataScopeService;

    public DeviceHeartbeatService(DeviceHeartbeatRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<DeviceHeartbeatEntity> list() {
        List<DeviceHeartbeatEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, e -> String.valueOf(e.getFsuId()));
    }

    public DeviceHeartbeatEntity getById(Long id) {
        DeviceHeartbeatEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("DeviceHeartbeatEntity not found: " + id));
        checkScopeForEntity(entity, "fsuCode", "DeviceHeartbeatEntity");
        return entity;
    }

    /** BE-AUTH-P0-FIX-002: scope 校验 — empty scope = default-deny */
    private void checkScopeForEntity(DeviceHeartbeatEntity entity, String scopeType, String entityName) {
        com.dcim.platform.common.security.RequestContext ctx = com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null || ctx.isAdminLike()) return;

        java.util.Set<String> allowed = dataScopeService.getAllowedFsuCodes();
        if (allowed != null && allowed.isEmpty()) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无授权 FSU 范围", "fsu:view");
        }

        String code = String.valueOf(entity.getFsuId());
        if (allowed != null && !allowed.contains(code)) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无权访问此" + entityName + ": fsuCode=" + code, "fsu:view");
        }
    }
}
