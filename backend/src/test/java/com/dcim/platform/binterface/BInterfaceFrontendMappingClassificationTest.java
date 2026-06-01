package com.dcim.platform.binterface;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.common.security.RequestContext;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.binterface.controller.BInterfaceFrontendReadController;
import com.dcim.platform.module.binterface.dto.BInterfaceFrontendDtos.UnmappedSignalDto;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import com.dcim.platform.module.mapping.entity.UnmappedSignalObservationEntity;
import com.dcim.platform.module.mapping.repository.DeviceSignalCandidateRepository;
import com.dcim.platform.module.mapping.repository.UnmappedSignalObservationRepository;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BInterfaceFrontendMappingClassificationTest {

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void getLoginInfoDeviceOnlyObservationIsNotReturnedAsTrueUnmappedSignal() {
        RequestContext.setCurrent(new RequestContext(1L, "admin", null, List.of("admin"), List.of(), null, null));
        UnmappedSignalObservationRepository unmappedRepo = mock(UnmappedSignalObservationRepository.class);
        DeviceSignalCandidateRepository candidateRepo = mock(DeviceSignalCandidateRepository.class);
        when(candidateRepo.findAllByOrderByDeviceIdAscSignalIdAsc()).thenReturn(List.of());
        when(unmappedRepo.findAllByOrderByLastSeenAtDesc()).thenReturn(List.of(deviceOnlyObservation()));

        BInterfaceFrontendReadController controller = controller(candidateRepo, unmappedRepo, mock(EStoneIIMappingService.class));
        List<UnmappedSignalDto> data = controller.getUnmappedSignals("51051243812345").getData();

        assertEquals(1, data.size());
        UnmappedSignalDto dto = data.get(0);
        assertEquals("DEVICE_ONLY", dto.mappingStatus);
        assertEquals(Boolean.FALSE, dto.hasSignalIdentity);
        assertEquals(Boolean.TRUE, dto.isDeviceOnly);
        assertEquals("DEVICE_ONLY", dto.observationType);
        assertEquals("DEVICE_DISCOVERED_WAIT_GET_DATA", dto.reason);
    }

    @Test
    void historicalObservationForStandard2016SignalIsReclassifiedByMappingService() {
        RequestContext.setCurrent(new RequestContext(1L, "admin", null, List.of("admin"), List.of(), null, null));
        UnmappedSignalObservationRepository unmappedRepo = mock(UnmappedSignalObservationRepository.class);
        DeviceSignalCandidateRepository candidateRepo = mock(DeviceSignalCandidateRepository.class);
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);
        when(candidateRepo.findAllByOrderByDeviceIdAscSignalIdAsc()).thenReturn(List.of());
        when(unmappedRepo.findAllByOrderByLastSeenAtDesc()).thenReturn(List.of(signalObservation("0407107001", "0.0000")));
        when(mappingService.resolveRealtime(eq("51051243812345"), isNull(), isNull(), eq("0407107001"), eq("0407107001"), eq("0.0000")))
                .thenReturn(new EStoneIIMappingResult("51051243812345", null, null, null,
                        "0407107001", "0407107001", "后半组电压", "", "0.0000", null,
                        "AI/模拟量", "AI", null, null, null, null,
                        "MAPPED_CANDIDATE", "LOW", "BINTERFACE_2016_STANDARD",
                        true, false, false, "BINTERFACE_2016_STANDARD", null));

        BInterfaceFrontendReadController controller = controller(candidateRepo, unmappedRepo, mappingService);
        List<UnmappedSignalDto> data = controller.getUnmappedSignals("51051243812345").getData();

        assertEquals(1, data.size());
        UnmappedSignalDto dto = data.get(0);
        assertEquals("MAPPED_CANDIDATE", dto.mappingStatus);
        assertEquals("LOW", dto.mappingConfidence);
        assertEquals("BINTERFACE_2016_STANDARD", dto.source);
        assertEquals(Boolean.TRUE, dto.hasSignalIdentity);
        assertEquals("MAPPED_FROM_OBSERVATION", dto.observationType);
    }

    private static BInterfaceFrontendReadController controller(DeviceSignalCandidateRepository candidateRepo,
                                                               UnmappedSignalObservationRepository unmappedRepo,
                                                               EStoneIIMappingService mappingService) {
        EStoneIIDictionaryImportService importService = mock(EStoneIIDictionaryImportService.class);
        return new BInterfaceFrontendReadController(
                mock(BInterfaceFsuStatusRepository.class),
                mock(FsuDeviceRepository.class),
                mock(BInterfaceMessageLogRepository.class),
                mock(BInterfaceMessageLogService.class),
                mock(AlarmRecordRepository.class),
                mock(RealtimeDataRepository.class),
                mock(MonitoringPointRepository.class),
                new DataScopeService(),
                importService,
                mappingService,
                candidateRepo,
                unmappedRepo
        );
    }

    private static UnmappedSignalObservationEntity deviceOnlyObservation() {
        UnmappedSignalObservationEntity entity = new UnmappedSignalObservationEntity();
        entity.setFsuId("51051243812345");
        entity.setDeviceId("51051241820004");
        entity.setSourceCommand("GET_LOGININFO");
        entity.setReason("missing_signal_mapping");
        entity.setFirstSeenAt(LocalDateTime.now());
        entity.setLastSeenAt(LocalDateTime.now());
        entity.setSeenCount(1);
        return entity;
    }

    private static UnmappedSignalObservationEntity signalObservation(String signalId, String value) {
        UnmappedSignalObservationEntity entity = new UnmappedSignalObservationEntity();
        entity.setFsuId("51051243812345");
        entity.setSignalId(signalId);
        entity.setRawId(signalId);
        entity.setRawValue(value);
        entity.setSourceCommand("GET_DATA");
        entity.setReason("UNKNOWN_SIGNAL_ID");
        entity.setFirstSeenAt(LocalDateTime.now());
        entity.setLastSeenAt(LocalDateTime.now());
        entity.setSeenCount(1);
        return entity;
    }
}
