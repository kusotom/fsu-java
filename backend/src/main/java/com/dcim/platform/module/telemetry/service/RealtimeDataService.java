package com.dcim.platform.module.telemetry.service;

import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RealtimeDataService {

    private final RealtimeDataRepository repository;

    public RealtimeDataService(RealtimeDataRepository repository) {
        this.repository = repository;
    }

    public List<RealtimeDataEntity> list() {
        return repository.findAll();
    }

    public RealtimeDataEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("RealtimeData not found: " + id));
    }

    // TODO: upsert by point_id - will be implemented in INIT-005
}
