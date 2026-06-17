package com.dcim.platform.module.resource.service;

import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MonitoringPointService {

    private final MonitoringPointRepository repository;
    private final DataScopeService dataScopeService;

    public MonitoringPointService(MonitoringPointRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<MonitoringPointEntity> list() {
        List<MonitoringPointEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, e -> String.valueOf(e.getFsuId()));
    }

    public MonitoringPointEntity getById(Long id) {
        MonitoringPointEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("MonitoringPointEntity not found: " + id));
        checkScopeForEntity(entity, "fsuCode", "MonitoringPointEntity");
        return entity;
    }

    private void checkScopeForEntity(MonitoringPointEntity entity, String scopeType, String entityName) {
        com.dcim.platform.common.security.RequestContext ctx = com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null || ctx.isAdminLike()) return;

        String code = String.valueOf(entity.getFsuId());
        if (code != null && !ctx.getFsuScope().isEmpty() && !ctx.getFsuScope().contains(code)) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无权访问此" + entityName + ": fsuCode=" + code, "fsu:view");
        }
    }

    public MonitoringPointEntity create(MonitoringPointEntity entity) {
        return repository.save(entity);
    }

    public MonitoringPointEntity update(Long id, MonitoringPointEntity entity) {
        MonitoringPointEntity existing = getById(id);
        existing.setFsuId(entity.getFsuId());
        existing.setCabinetId(entity.getCabinetId());
        existing.setPointCode(entity.getPointCode());
        existing.setPointName(entity.getPointName());
        existing.setPointType(entity.getPointType());
        existing.setDataType(entity.getDataType());
        existing.setUnit(entity.getUnit());
        existing.setValueRange(entity.getValueRange());
        existing.setPrecisionVal(entity.getPrecisionVal());
        existing.setAlarmUpper(entity.getAlarmUpper());
        existing.setAlarmLower(entity.getAlarmLower());
        existing.setAlarmUpperUrgent(entity.getAlarmUpperUrgent());
        existing.setAlarmLowerUrgent(entity.getAlarmLowerUrgent());
        existing.setPollingInterval(entity.getPollingInterval());
        existing.setStatus(entity.getStatus());
        existing.setSortOrder(entity.getSortOrder());
        return repository.save(existing);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
