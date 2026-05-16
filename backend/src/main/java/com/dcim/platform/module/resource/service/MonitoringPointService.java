package com.dcim.platform.module.resource.service;

import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MonitoringPointService {

    private final MonitoringPointRepository repository;

    public MonitoringPointService(MonitoringPointRepository repository) {
        this.repository = repository;
    }

    public List<MonitoringPointEntity> list() {
        return repository.findAll();
    }

    public MonitoringPointEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Point not found: " + id));
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
