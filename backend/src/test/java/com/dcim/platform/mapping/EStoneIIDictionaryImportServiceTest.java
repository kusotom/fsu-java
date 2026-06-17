package com.dcim.platform.mapping;

import com.dcim.platform.module.mapping.entity.EStoneIIControlReferenceEntity;
import com.dcim.platform.module.mapping.repository.DeviceSignalCandidateRepository;
import com.dcim.platform.module.mapping.repository.EStoneIIControlReferenceRepository;
import com.dcim.platform.module.mapping.repository.EStoneIIEventDictionaryRepository;
import com.dcim.platform.module.mapping.repository.EStoneIISignalDictionaryRepository;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIITemplateDictionaryParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EStoneIIDictionaryImportServiceTest {

    @Test
    void importsSignalsEventsControlsAndCandidatesWithControlsDisabled() {
        EStoneIITemplateDictionaryParser parser =
                new EStoneIITemplateDictionaryParser(new DefaultResourceLoader());
        List<Object> savedSignals = new ArrayList<>();
        List<Object> savedEvents = new ArrayList<>();
        List<Object> savedControls = new ArrayList<>();
        List<Object> savedCandidates = new ArrayList<>();

        EStoneIISignalDictionaryRepository signalRepository =
                repositoryProxy(EStoneIISignalDictionaryRepository.class, savedSignals);
        EStoneIIEventDictionaryRepository eventRepository =
                repositoryProxy(EStoneIIEventDictionaryRepository.class, savedEvents);
        EStoneIIControlReferenceRepository controlRepository =
                repositoryProxy(EStoneIIControlReferenceRepository.class, savedControls);
        DeviceSignalCandidateRepository candidateRepository =
                repositoryProxy(DeviceSignalCandidateRepository.class, savedCandidates);

        EStoneIIDictionaryImportService service = new EStoneIIDictionaryImportService(
                parser, signalRepository, eventRepository, controlRepository, candidateRepository);

        EStoneIIDictionaryImportService.ImportSummary summary = service.importAll();

        assertEquals(39, summary.signals());
        assertEquals(13, summary.events());
        assertEquals(4, summary.controls());
        assertEquals(23, summary.candidates());
        assertTrue(summary.imported());
        assertEquals(39, savedSignals.size());
        assertEquals(13, savedEvents.size());
        assertEquals(4, savedControls.size());
        assertEquals(23, savedCandidates.size());

        for (Object saved : savedControls) {
            EStoneIIControlReferenceEntity control = (EStoneIIControlReferenceEntity) saved;
            assertFalse(control.getEnabledForControl());
            assertEquals("disabled", control.getControlAccess());
            assertEquals("template_reference_only", control.getSource());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T repositoryProxy(Class<T> type, List<Object> saved) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            String name = method.getName();
            if ("findBySignalId".equals(name) || "findByEventId".equals(name)
                    || "findByCommandId".equals(name)
                    || "findFirstByFsuIdAndDeviceIdAndSignalIdAndMappingSource".equals(name)) {
                return Optional.empty();
            }
            if ("count".equals(name)) return 0L;
            if ("save".equals(name)) {
                saved.add(args[0]);
                return args[0];
            }
            if ("toString".equals(name)) return type.getSimpleName() + "Proxy";
            if ("hashCode".equals(name)) return System.identityHashCode(proxy);
            if ("equals".equals(name)) return proxy == args[0];
            throw new UnsupportedOperationException(name);
        });
    }
}
