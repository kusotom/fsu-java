package com.dcim.platform.module.alarm.service;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AlarmRecordService {

    private final AlarmRecordRepository repository;

    public AlarmRecordService(AlarmRecordRepository repository) {
        this.repository = repository;
    }

    public List<AlarmRecordEntity> list() {
        return repository.findAll();
    }

    public AlarmRecordEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Alarm not found: " + id));
    }

    public List<AlarmRecordEntity> listByStatus(String alarmStatus) {
        return repository.findByAlarmStatus(alarmStatus);
    }
}
