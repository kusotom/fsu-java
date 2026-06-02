package com.dcim.platform.module.alarm.service;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.binterface.dto.BInterfaceFrontendDtos.AlarmDto;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AlarmRecordService {

    private final AlarmRecordRepository repository;
    private final DataScopeService dataScopeService;
    private final FsuDeviceRepository fsuDeviceRepository;
    private final EStoneIIDictionaryImportService dictionaryImportService;
    private final EStoneIIMappingService mappingService;

    public AlarmRecordService(AlarmRecordRepository repository, DataScopeService dataScopeService,
                              FsuDeviceRepository fsuDeviceRepository,
                              EStoneIIDictionaryImportService dictionaryImportService,
                              EStoneIIMappingService mappingService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.dictionaryImportService = dictionaryImportService;
        this.mappingService = mappingService;
    }

    public List<AlarmDto> list() {
        dictionaryImportService.importIfNeeded();
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        List<AlarmRecordEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all,
                        e -> fsuCodeById.getOrDefault(e.getFsuId(), String.valueOf(e.getFsuId())))
                .stream()
                .map(e -> toDto(e, fsuCodeById.get(e.getFsuId())))
                .toList();
    }

    public AlarmDto getById(Long id) {
        AlarmRecordEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Alarm not found: " + id));
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        List<AlarmRecordEntity> visible = dataScopeService.filterByFsuScope(List.of(entity),
                e -> fsuCodeById.getOrDefault(e.getFsuId(), String.valueOf(e.getFsuId())));
        if (visible.isEmpty()) throw new RuntimeException("Alarm not found: " + id);
        return toDto(entity, fsuCodeById.get(entity.getFsuId()));
    }

    public List<AlarmDto> listByStatus(String alarmStatus) {
        dictionaryImportService.importIfNeeded();
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        List<AlarmRecordEntity> all = repository.findByAlarmStatus(alarmStatus);
        return dataScopeService.filterByFsuScope(all,
                        e -> fsuCodeById.getOrDefault(e.getFsuId(), String.valueOf(e.getFsuId())))
                .stream()
                .map(e -> toDto(e, fsuCodeById.get(e.getFsuId())))
                .toList();
    }

    private AlarmDto toDto(AlarmRecordEntity e, String fsuCode) {
        AlarmDto dto = new AlarmDto();
        dto.id = e.getId();
        dto.fsuCode = fsuCode;
        dto.deviceId = e.getDeviceId();
        dto.spid = e.getSpid();
        dto.signalId = e.getPointCode();
        dto.serialNo = e.getSerialNo();
        dto.alarmLevel = e.getAlarmLevel();
        dto.alarmStatus = e.getAlarmStatus();
        dto.alarmDesc = e.getAlarmDesc();
        dto.alarmTime = e.getOccurTime() != null ? e.getOccurTime().toString() : null;
        dto.recoveryTime = e.getClearTime() != null ? e.getClearTime().toString() : null;
        dto.createdAt = e.getCreatedAt() != null ? e.getCreatedAt().toString() : null;
        dto.updatedAt = e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null;
        EStoneIIMappingResult mapped = mappingService.resolveAlarm(fsuCode, e.getDeviceId(), null,
                e.getSpid(), e.getPointCode(), e.getAlarmCode(), e.getAlarmValue());
        dto.deviceId = mapped.deviceId() != null ? mapped.deviceId() : dto.deviceId;
        dto.deviceCode = mapped.deviceCode();
        dto.deviceName = mapped.deviceName();
        dto.signalId = mapped.signalId();
        dto.spid = mapped.spid();
        dto.signalName = mapped.signalName();
        dto.eventId = mapped.eventId();
        dto.eventName = mapped.eventName();
        dto.alarmMeaning = mapped.alarmMeaning();
        dto.eventSeverity = mapped.eventSeverity();
        dto.mappingStatus = mapped.mappingStatus();
        dto.mappingConfidence = mapped.mappingConfidence();
        dto.templateVariant = mapped.templateVariant();
        dto.needRealDataConfirm = mapped.needRealDataConfirm();
        dto.verifiedByRealData = mapped.verifiedByRealData();
        dto.source = mapped.source();
        return dto;
    }

    private Map<Long, String> fsuCodeByDbId() {
        Map<Long, String> map = new HashMap<>();
        for (FsuDeviceEntity fsu : fsuDeviceRepository.findAll()) {
            if (fsu.getId() != null && fsu.getFsuCode() != null) map.put(fsu.getId(), fsu.getFsuCode());
        }
        return map;
    }
}
