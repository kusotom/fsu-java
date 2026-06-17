package com.dcim.platform.module.resource.service;

import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FsuDeviceService {

    private final FsuDeviceRepository repository;
    private final DataScopeService dataScopeService;

    public FsuDeviceService(FsuDeviceRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<FsuDeviceEntity> list() {
        List<FsuDeviceEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, FsuDeviceEntity::getFsuCode);
    }

    public FsuDeviceEntity getById(Long id) {
        FsuDeviceEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("FsuDeviceEntity not found: " + id));
        checkScopeForEntity(entity, "fsuCode", "FsuDeviceEntity");
        return entity;
    }

    /** BE-AUTH-P0-FIX-002: scope 校验 — empty scope = default-deny */
    private void checkScopeForEntity(FsuDeviceEntity entity, String scopeType, String entityName) {
        com.dcim.platform.common.security.RequestContext ctx = com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null || ctx.isAdminLike()) return;

        java.util.Set<String> allowed = dataScopeService.getAllowedFsuCodes();
        if (allowed != null && allowed.isEmpty()) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无授权 FSU 范围", "fsu:view");
        }

        String code = entity.getFsuCode();
        if (allowed != null && !allowed.contains(code)) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无权访问此" + entityName + ": fsuCode=" + code, "fsu:view");
        }
    }

    public FsuDeviceEntity create(FsuDeviceEntity entity) {
        return repository.save(entity);
    }

    public FsuDeviceEntity update(Long id, FsuDeviceEntity entity) {
        FsuDeviceEntity existing = getById(id);
        existing.setSiteId(entity.getSiteId());
        existing.setCabinetId(entity.getCabinetId());
        existing.setFsuCode(entity.getFsuCode());
        existing.setFsuName(entity.getFsuName());
        existing.setFsuType(entity.getFsuType());
        existing.setModel(entity.getModel());
        existing.setManufacturer(entity.getManufacturer());
        existing.setFirmwareVersion(entity.getFirmwareVersion());
        existing.setIpAddr(entity.getIpAddr());
        existing.setPort(entity.getPort());
        existing.setMacAddr(entity.getMacAddr());
        existing.setProtocolVersion(entity.getProtocolVersion());
        existing.setStatus(entity.getStatus());
        existing.setDescription(entity.getDescription());
        return repository.save(existing);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
