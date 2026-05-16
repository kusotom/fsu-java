package com.dcim.platform.module.telemetry.service;

import com.dcim.platform.module.telemetry.entity.HistoryDataEntity;
import com.dcim.platform.module.telemetry.repository.HistoryDataRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoryDataService {

    private final HistoryDataRepository repository;

    public HistoryDataService(HistoryDataRepository repository) {
        this.repository = repository;
    }

    public List<HistoryDataEntity> list() {
        return repository.findAll();
    }

    public HistoryDataEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("HistoryData not found: " + id));
    }

    public List<HistoryDataEntity> queryByPoint(Long pointId, LocalDateTime start, LocalDateTime end) {
        return repository.findByPointIdAndCollectTimeBetween(pointId, start, end);
    }
}
