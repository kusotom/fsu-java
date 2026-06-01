package com.dcim.platform.module.resource.service;

import com.dcim.platform.module.resource.entity.CabinetEntity;
import com.dcim.platform.module.resource.repository.CabinetRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CabinetService {

    private final CabinetRepository repository;
    private final DataScopeService dataScopeService;

    public CabinetService(CabinetRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<CabinetEntity> list() {
        List<CabinetEntity> all = repository.findAll();
        return dataScopeService.filterByStationScope(all, CabinetEntity::getSiteId);
    }

    public CabinetEntity getById(Long id) {
        CabinetEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("CabinetEntity not found: " + id));
        checkScopeForEntity(entity, "stationId", "CabinetEntity");
        return entity;
    }

    private void checkScopeForEntity(CabinetEntity entity, String scopeType, String entityName) {
        com.dcim.platform.common.security.RequestContext ctx = com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null || ctx.isAdminLike()) return;

        Long stationId = entity.getSiteId();
        if (stationId != null && !ctx.getStationScope().isEmpty() && !ctx.getStationScope().contains(stationId)) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无权访问此" + entityName + ": stationId=" + stationId, "site:view");
        }
    }

    public CabinetEntity create(CabinetEntity entity) {
        return repository.save(entity);
    }

    public CabinetEntity update(Long id, CabinetEntity entity) {
        CabinetEntity existing = getById(id);
        existing.setSiteId(entity.getSiteId());
        existing.setCabinetCode(entity.getCabinetCode());
        existing.setCabinetName(entity.getCabinetName());
        existing.setCabinetType(entity.getCabinetType());
        existing.setModel(entity.getModel());
        existing.setManufacturer(entity.getManufacturer());
        existing.setInstallDate(entity.getInstallDate());
        existing.setStatus(entity.getStatus());
        existing.setDescription(entity.getDescription());
        return repository.save(existing);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
