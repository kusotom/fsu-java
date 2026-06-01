package com.dcim.platform.mapping;

import com.dcim.platform.module.mapping.entity.UnmappedSignalObservationEntity;
import com.dcim.platform.module.mapping.repository.UnmappedSignalObservationRepository;
import com.dcim.platform.module.mapping.service.UnmappedSignalObservationService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UnmappedSignalObservationServiceTest {

    @Test
    void recordsUnknownSignalObservationAndIncrementsSeenCount() {
        List<UnmappedSignalObservationEntity> saved = new ArrayList<>();
        UnmappedSignalObservationRepository repository = repositoryProxy(saved);

        UnmappedSignalObservationService service = new UnmappedSignalObservationService(repository);
        service.record("51051243812345", "unknown-device", null, "999999999", "999999999",
                "999999999", "厂家未知点", "1", null, "GET_DATA", 12L, "sample-1",
                "UNKNOWN_DEVICE_SIGNAL_PAIR");

        assertEquals(1, saved.size());
        UnmappedSignalObservationEntity entity = saved.get(0);
        assertEquals("51051243812345", entity.getFsuId());
        assertEquals("unknown-device", entity.getDeviceId());
        assertEquals("999999999", entity.getSignalId());
        assertEquals("GET_DATA", entity.getSourceCommand());
        assertEquals("UNKNOWN_DEVICE_SIGNAL_PAIR", entity.getReason());
        assertEquals(1, entity.getSeenCount());
        assertNotNull(entity.getFirstSeenAt());
        assertNotNull(entity.getLastSeenAt());
    }

    @Test
    void keepsDifferentUnknownEventIdsAsSeparateObservations() {
        List<UnmappedSignalObservationEntity> saved = new ArrayList<>();
        UnmappedSignalObservationRepository repository = repositoryProxy(saved);

        UnmappedSignalObservationService service = new UnmappedSignalObservationService(repository);
        service.record("51051243812345", "51051241820004", null, "510000250", "510000250",
                "UNKNOWN-EVENT-1", "未知告警1", "1", null, "SEND_ALARM", null, null,
                "UNKNOWN_EVENT_ID");
        service.record("51051243812345", "51051241820004", null, "510000250", "510000250",
                "UNKNOWN-EVENT-2", "未知告警2", "1", null, "SEND_ALARM", null, null,
                "UNKNOWN_EVENT_ID");

        assertEquals(2, saved.size());
        assertEquals("UNKNOWN-EVENT-1", saved.get(0).getRawId());
        assertEquals("UNKNOWN-EVENT-2", saved.get(1).getRawId());
    }

    @Test
    void skipsGetLoginInfoDeviceOnlyObservationWithoutSignalIdentity() {
        List<UnmappedSignalObservationEntity> saved = new ArrayList<>();
        UnmappedSignalObservationRepository repository = repositoryProxy(saved);

        UnmappedSignalObservationService service = new UnmappedSignalObservationService(repository);
        service.record("51051243812345", "51051241820004", null, null, null,
                null, null, null, null, "GET_LOGININFO", null, null,
                "missing_signal_mapping");

        assertTrue(saved.isEmpty());
    }

    private static UnmappedSignalObservationRepository repositoryProxy(List<UnmappedSignalObservationEntity> saved) {
        return (UnmappedSignalObservationRepository) Proxy.newProxyInstance(
                UnmappedSignalObservationRepository.class.getClassLoader(),
                new Class<?>[]{UnmappedSignalObservationRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findFirstByFsuIdAndDeviceIdAndSignalIdAndRawIdAndSourceCommandAndReason" -> Optional.empty();
                    case "save" -> {
                        saved.add((UnmappedSignalObservationEntity) args[0]);
                        yield args[0];
                    }
                    case "toString" -> "UnmappedRepoProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
