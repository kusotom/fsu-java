package com.dcim.platform.telemetry;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.common.security.RequestContext;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.resource.repository.SiteRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import com.dcim.platform.module.telemetry.service.RealtimeDataService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RealtimeDataServiceMappingClassificationTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void sourceUnknownLegacyPointCodeIsHistoricalPendingBackfillNotTrueUnmapped() {
        RequestContext.setCurrent(new RequestContext(1L, "admin", null, List.of("admin"), List.of(), null, null));
        RealtimeDataRepository realtimeRepo = mock(RealtimeDataRepository.class);
        FsuDeviceRepository fsuRepo = mock(FsuDeviceRepository.class);
        MonitoringPointRepository pointRepo = mock(MonitoringPointRepository.class);
        EStoneIIDictionaryImportService importService = mock(EStoneIIDictionaryImportService.class);
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);

        when(realtimeRepo.findAll()).thenReturn(List.of(realtime(1L, "TEMP-001", "25.5000")));
        when(fsuRepo.findAll()).thenReturn(List.of(fsu(1L, "FSU-001")));
        when(pointRepo.findAll()).thenReturn(List.of(point(1L, "TEMP-001", "机柜温度", "AI", "℃")));
        when(mappingService.resolveRealtime(eq("FSU-001"), isNull(), isNull(), eq("TEMP-001"), eq("TEMP-001"), eq("25.5000")))
                .thenReturn(unmapped("TEMP-001", "25.5000"));

        RealtimeDataService service = new RealtimeDataService(
                realtimeRepo, new DataScopeService(), fsuRepo, pointRepo, mock(SiteRepository.class), importService, mappingService);

        var dto = service.list().get(0);

        assertEquals("机柜温度", dto.pointName);
        assertEquals("℃", dto.unit);
        assertEquals("HISTORICAL_PENDING_BACKFILL", dto.mappingStatus);
        assertEquals("legacy_realtime_data", dto.source);
        assertEquals(Boolean.TRUE, dto.legacyData);
        assertEquals("HISTORICAL_REALTIME", dto.observationType);
        assertEquals(Boolean.TRUE, dto.hasSignalIdentity);
    }

    @Test
    void standard2016CandidateZeroValueIsPreservedAsMappedCandidate() {
        RequestContext.setCurrent(new RequestContext(1L, "admin", null, List.of("admin"), List.of(), null, null));
        RealtimeDataRepository realtimeRepo = mock(RealtimeDataRepository.class);
        FsuDeviceRepository fsuRepo = mock(FsuDeviceRepository.class);
        MonitoringPointRepository pointRepo = mock(MonitoringPointRepository.class);
        EStoneIIDictionaryImportService importService = mock(EStoneIIDictionaryImportService.class);
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);

        when(realtimeRepo.findAll()).thenReturn(List.of(realtime(2L, "0407107001", "0.0000")));
        when(fsuRepo.findAll()).thenReturn(List.of(fsu(2L, "51051243812345")));
        when(pointRepo.findAll()).thenReturn(List.of());
        when(mappingService.resolveRealtime(eq("51051243812345"), isNull(), isNull(), eq("0407107001"), eq("0407107001"), eq("0.0000")))
                .thenReturn(new EStoneIIMappingResult("51051243812345", null, null, null,
                        "0407107001", "0407107001", "后半组电压", "", "0.0000", null,
                        "AI/模拟量", "AI", null, null, null, null,
                        "MAPPED_CANDIDATE", "LOW", "BINTERFACE_2016_STANDARD",
                        true, false, false, "BINTERFACE_2016_STANDARD", null));

        RealtimeDataService service = new RealtimeDataService(
                realtimeRepo, new DataScopeService(), fsuRepo, pointRepo, mock(SiteRepository.class), importService, mappingService);

        var dto = service.list().get(0);

        assertEquals("MAPPED_CANDIDATE", dto.mappingStatus);
        assertEquals("后半组电压", dto.pointName);
        assertEquals("后半组电压", dto.signalName);
        assertEquals("0.0000", dto.value);
        assertEquals("", dto.unit);
        assertEquals(Boolean.FALSE, dto.legacyData);
    }

    @Test
    void mappedResultDeviceAndSignalNamesAreReturnedToFrontendDto() {
        RequestContext.setCurrent(new RequestContext(1L, "admin", null, List.of("admin"), List.of(), null, null));
        RealtimeDataRepository realtimeRepo = mock(RealtimeDataRepository.class);
        FsuDeviceRepository fsuRepo = mock(FsuDeviceRepository.class);
        MonitoringPointRepository pointRepo = mock(MonitoringPointRepository.class);
        EStoneIIDictionaryImportService importService = mock(EStoneIIDictionaryImportService.class);
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);

        when(realtimeRepo.findAll()).thenReturn(List.of(realtime(3L, "510000211", "25.6000")));
        when(fsuRepo.findAll()).thenReturn(List.of(fsu(3L, "51051243812345")));
        when(pointRepo.findAll()).thenReturn(List.of());
        when(mappingService.resolveRealtime(eq("51051243812345"), isNull(), isNull(), eq("510000211"), eq("510000211"), eq("25.6000")))
                .thenReturn(new EStoneIIMappingResult("51051243812345",
                        "51051241830004", "51051241830004", "TempHumidity",
                        "510000211", "510000211", "I2C温度", "℃", "25.6000", null,
                        "AI/模拟量", "AI", null, null, null, null,
                        "MAPPED_CANDIDATE", "HIGH", "BOTH",
                        true, false, false, "device_signal_candidate", null));

        RealtimeDataService service = new RealtimeDataService(
                realtimeRepo, new DataScopeService(), fsuRepo, pointRepo, mock(SiteRepository.class), importService, mappingService);

        var dto = service.list().get(0);

        assertEquals("51051241830004", dto.deviceId);
        assertEquals("TempHumidity", dto.deviceName);
        assertEquals("I2C温度", dto.pointName);
        assertEquals("I2C温度", dto.signalName);
        assertEquals("℃", dto.unit);
        assertEquals("MAPPED_CANDIDATE", dto.mappingStatus);
    }

    private static RealtimeDataEntity realtime(Long fsuId, String pointCode, String value) {
        RealtimeDataEntity entity = new RealtimeDataEntity();
        entity.setId(1L);
        entity.setFsuId(fsuId);
        entity.setPointId(1L);
        entity.setPointCode(pointCode);
        entity.setValueNumber(new BigDecimal(value));
        entity.setQuality("GOOD");
        entity.setCollectTime(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private static FsuDeviceEntity fsu(Long id, String fsuCode) {
        FsuDeviceEntity entity = new FsuDeviceEntity();
        entity.setId(id);
        entity.setFsuCode(fsuCode);
        return entity;
    }

    private static MonitoringPointEntity point(Long id, String pointCode, String pointName, String pointType, String unit) {
        MonitoringPointEntity entity = new MonitoringPointEntity();
        entity.setId(id);
        entity.setPointCode(pointCode);
        entity.setPointName(pointName);
        entity.setPointType(pointType);
        entity.setUnit(unit);
        return entity;
    }

    private static EStoneIIMappingResult unmapped(String signalId, String value) {
        return new EStoneIIMappingResult(null, null, null, null,
                signalId, signalId, null, null, value, null,
                null, null, null, null, null, null,
                "UNMAPPED", "UNKNOWN", "UNKNOWN",
                true, false, false, "unmapped_observation", "UNKNOWN_SIGNAL_ID");
    }
}
