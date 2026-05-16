package com.dcim.platform.module.resource.service;

import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FsuDeviceService {

    private final FsuDeviceRepository repository;

    public FsuDeviceService(FsuDeviceRepository repository) {
        this.repository = repository;
    }

    public List<FsuDeviceEntity> list() {
        return repository.findAll();
    }

    public FsuDeviceEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("FSU not found: " + id));
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
