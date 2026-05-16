package com.dcim.platform.module.telemetry.service;

import com.dcim.platform.module.telemetry.entity.DeviceHeartbeatEntity;
import com.dcim.platform.module.telemetry.repository.DeviceHeartbeatRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DeviceHeartbeatService {

    private final DeviceHeartbeatRepository repository;

    public DeviceHeartbeatService(DeviceHeartbeatRepository repository) {
        this.repository = repository;
    }

    public List<DeviceHeartbeatEntity> list() {
        return repository.findAll();
    }

    public DeviceHeartbeatEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Heartbeat not found: " + id));
    }
}
