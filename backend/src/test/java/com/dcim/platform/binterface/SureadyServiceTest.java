package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.service.SureadyResult;
import com.dcim.platform.module.binterface.service.SureadyService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SUREADY 服务测试 (BIF-P4-017)。
 */
class SureadyServiceTest {

    private FsuDeviceRepository deviceRepo;
    private BInterfaceFsuStatusRepository statusRepo;
    private SureadyService service;

    @BeforeEach
    void setUp() {
        deviceRepo = mock(FsuDeviceRepository.class);
        statusRepo = mock(BInterfaceFsuStatusRepository.class);
        service = new SureadyService(deviceRepo, statusRepo);
    }

    @Test void shouldSucceedForKnownSuid() {
        FsuDeviceEntity dev = new FsuDeviceEntity();
        dev.setId(1L); dev.setFsuCode("FSU-001"); dev.setStatus("OFFLINE");
        when(deviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));
        when(statusRepo.findByFsuCode("FSU-001")).thenReturn(Optional.empty());

        SureadyResult r = service.execute("FSU-001");
        assertTrue(r.isSuccess());
        assertTrue(r.isRegistered());
        assertEquals("FSU-001", r.getSuid());
    }

    @Test void shouldUpdateDeviceStatusToOnline() {
        FsuDeviceEntity dev = new FsuDeviceEntity();
        dev.setId(1L); dev.setFsuCode("FSU-001"); dev.setStatus("OFFLINE");
        when(deviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));
        when(statusRepo.findByFsuCode("FSU-001")).thenReturn(Optional.empty());

        service.execute("FSU-001");
        verify(deviceRepo).save(argThat(d -> "ONLINE".equals(d.getStatus())));
    }

    @Test void shouldUpdateFsuStatus() {
        FsuDeviceEntity dev = new FsuDeviceEntity();
        dev.setId(1L); dev.setFsuCode("FSU-001"); dev.setStatus("ONLINE");
        when(deviceRepo.findByFsuCode("FSU-001")).thenReturn(Optional.of(dev));
        when(statusRepo.findByFsuCode("FSU-001")).thenReturn(Optional.empty());

        service.execute("FSU-001");
        verify(statusRepo).save(any(BInterfaceFsuStatusEntity.class));
    }

    @Test void shouldFailForNullSuid() {
        assertFalse(service.execute(null).isSuccess());
    }

    @Test void shouldFailForUnknownSuid() {
        when(deviceRepo.findByFsuCode("UNKNOWN")).thenReturn(Optional.empty());
        SureadyResult r = service.execute("UNKNOWN");
        assertFalse(r.isSuccess());
        assertEquals("SUID_ERROR", r.getResultCode());
    }

    @Test void sureadyCodeShouldBe103() {
        assertEquals(103, com.dcim.platform.module.binterface.model.BInterfaceCommand2024.SUREADY.getCode());
    }

    @Test void sureadyAckCodeShouldBe104() {
        assertEquals(104, com.dcim.platform.module.binterface.model.BInterfaceCommand2024.SUREADY_ACK.getCode());
    }

    @Test void pkTypeShouldContainSuready() {
        assertNotNull(com.dcim.platform.module.binterface.model.BInterfacePkType.valueOf("SUREADY"));
        assertNotNull(com.dcim.platform.module.binterface.model.BInterfacePkType.valueOf("SUREADY_ACK"));
    }
}
