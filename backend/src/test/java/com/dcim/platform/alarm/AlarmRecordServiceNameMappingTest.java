package com.dcim.platform.alarm;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.common.security.RequestContext;
import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.alarm.service.AlarmRecordService;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlarmRecordServiceNameMappingTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void mappedAlarmReturnsDeviceSignalAndEventNames() {
        RequestContext.setCurrent(new RequestContext(1L, "admin", null, List.of("admin"), List.of(), null, null));
        AlarmRecordRepository alarmRepo = mock(AlarmRecordRepository.class);
        FsuDeviceRepository fsuRepo = mock(FsuDeviceRepository.class);
        EStoneIIDictionaryImportService importService = mock(EStoneIIDictionaryImportService.class);
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);

        when(alarmRepo.findAll()).thenReturn(List.of(alarm()));
        when(fsuRepo.findAll()).thenReturn(List.of(fsu(1L, "51051243812345")));
        when(mappingService.resolveAlarm(eq("51051243812345"), eq("51051241820004"), isNull(),
                eq("510000250"), eq("510000250"), eq("510000250"), eq("1")))
                .thenReturn(new EStoneIIMappingResult("51051243812345",
                        "51051241820004", "51051241820004", "Smoke",
                        "510000250", "510000250", "烟感", "", "1", "有告警",
                        "DI/状态量", "DI", "510000250", "烟感告警", "烟感告警", "一级告警",
                        "MAPPED_CANDIDATE", "HIGH", "BOTH",
                        true, false, false, "device_signal_candidate", null));

        AlarmRecordService service = new AlarmRecordService(
                alarmRepo, new DataScopeService(), fsuRepo, importService, mappingService);

        var dto = service.list().get(0);

        assertEquals("Smoke", dto.deviceName);
        assertEquals("烟感", dto.signalName);
        assertEquals("烟感告警", dto.eventName);
        assertEquals("烟感告警", dto.alarmMeaning);
        assertEquals("一级告警", dto.eventSeverity);
        assertEquals("MAPPED_CANDIDATE", dto.mappingStatus);
    }

    private static AlarmRecordEntity alarm() {
        AlarmRecordEntity entity = new AlarmRecordEntity();
        entity.setId(1L);
        entity.setFsuId(1L);
        entity.setDeviceId("51051241820004");
        entity.setPointCode("510000250");
        entity.setSpid("510000250");
        entity.setAlarmCode("510000250");
        entity.setAlarmLevel("1");
        entity.setAlarmStatus("ACTIVE");
        entity.setAlarmValue("1");
        entity.setOccurTime(LocalDateTime.now());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private static FsuDeviceEntity fsu(Long id, String fsuCode) {
        FsuDeviceEntity entity = new FsuDeviceEntity();
        entity.setId(id);
        entity.setFsuCode(fsuCode);
        return entity;
    }
}
