package com.dcim.platform.telemetry;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.common.security.RequestContext;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
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
        EStoneIIDictionaryImportService importService = mock(EStoneIIDictionaryImportService.class);
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);

        when(realtimeRepo.findAll()).thenReturn(List.of(realtime(1L, "TEMP-001", "25.5000")));
        when(fsuRepo.findAll()).thenReturn(List.of(fsu(1L, "FSU-001")));
        when(mappingService.resolveRealtime(eq("FSU-001"), isNull(), isNull(), eq("TEMP-001"), eq("TEMP-001"), eq("25.5000")))
                .thenReturn(unmapped("TEMP-001", "25.5000"));

        RealtimeDataService service = new RealtimeDataService(
                realtimeRepo, new DataScopeService(), fsuRepo, importService, mappingService);

        var dto = service.list().get(0);

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
        EStoneIIDictionaryImportService importService = mock(EStoneIIDictionaryImportService.class);
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);

        when(realtimeRepo.findAll()).thenReturn(List.of(realtime(2L, "0407107001", "0.0000")));
        when(fsuRepo.findAll()).thenReturn(List.of(fsu(2L, "51051243812345")));
        when(mappingService.resolveRealtime(eq("51051243812345"), isNull(), isNull(), eq("0407107001"), eq("0407107001"), eq("0.0000")))
                .thenReturn(new EStoneIIMappingResult("51051243812345", null, null, null,
                        "0407107001", "0407107001", "后半组电压", "", "0.0000", null,
                        "AI/模拟量", "AI", null, null, null, null,
                        "MAPPED_CANDIDATE", "LOW", "BINTERFACE_2016_STANDARD",
                        true, false, false, "BINTERFACE_2016_STANDARD", null));

        RealtimeDataService service = new RealtimeDataService(
                realtimeRepo, new DataScopeService(), fsuRepo, importService, mappingService);

        var dto = service.list().get(0);

        assertEquals("MAPPED_CANDIDATE", dto.mappingStatus);
        assertEquals("0.0000", dto.value);
        assertEquals("", dto.unit);
        assertEquals(Boolean.FALSE, dto.legacyData);
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

    private static EStoneIIMappingResult unmapped(String signalId, String value) {
        return new EStoneIIMappingResult(null, null, null, null,
                signalId, signalId, null, null, value, null,
                null, null, null, null, null, null,
                "UNMAPPED", "UNKNOWN", "UNKNOWN",
                true, false, false, "unmapped_observation", "UNKNOWN_SIGNAL_ID");
    }
}
