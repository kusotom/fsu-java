package com.dcim.platform.module.resource.service;

import com.dcim.platform.module.resource.entity.CabinetEntity;
import com.dcim.platform.module.resource.repository.CabinetRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CabinetService {

    private final CabinetRepository repository;

    public CabinetService(CabinetRepository repository) {
        this.repository = repository;
    }

    public List<CabinetEntity> list() {
        return repository.findAll();
    }

    public CabinetEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Cabinet not found: " + id));
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
