package com.dcim.platform.module.mapping.service;

import com.dcim.platform.module.mapping.entity.DeviceSignalCandidateEntity;
import com.dcim.platform.module.mapping.entity.EStoneIIEventDictionaryEntity;
import com.dcim.platform.module.mapping.entity.EStoneIISignalDictionaryEntity;
import com.dcim.platform.module.mapping.repository.DeviceSignalCandidateRepository;
import com.dcim.platform.module.mapping.repository.EStoneIIEventDictionaryRepository;
import com.dcim.platform.module.mapping.repository.EStoneIISignalDictionaryRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EStoneIIMappingService {

    private final EStoneIIDictionaryImportService importService;
    private final EStoneIISignalDictionaryRepository signalRepository;
    private final EStoneIIEventDictionaryRepository eventRepository;
    private final DeviceSignalCandidateRepository candidateRepository;
    private final EStoneIITemplateDictionaryParser parser;
    private final BInterface2016StandardSignalIndexService standard2016SignalIndexService;

    public EStoneIIMappingService(EStoneIIDictionaryImportService importService,
                                  EStoneIISignalDictionaryRepository signalRepository,
                                  EStoneIIEventDictionaryRepository eventRepository,
                                  DeviceSignalCandidateRepository candidateRepository,
                                  EStoneIITemplateDictionaryParser parser,
                                  BInterface2016StandardSignalIndexService standard2016SignalIndexService) {
        this.importService = importService;
        this.signalRepository = signalRepository;
        this.eventRepository = eventRepository;
        this.candidateRepository = candidateRepository;
        this.parser = parser;
        this.standard2016SignalIndexService = standard2016SignalIndexService;
    }

    public EStoneIIMappingResult resolveRealtime(String fsuId, String deviceId, String deviceCode,
                                                 String spid, String signalId, String value) {
        importService.importIfNeeded();
        String normalizedSignalId = firstNonBlank(signalId, spid);
        if (isBlank(normalizedSignalId)) {
            return unmapped(fsuId, deviceId, deviceCode, spid, signalId, value, "UNKNOWN_SIGNAL_ID");
        }

        Optional<DeviceSignalCandidateEntity> candidate = findCandidate(fsuId, deviceId, deviceCode, normalizedSignalId);
        Optional<EStoneIISignalDictionaryEntity> signal = signalRepository.findBySignalId(normalizedSignalId);
        if (candidate.isPresent()) {
            DeviceSignalCandidateEntity c = candidate.get();
            EStoneIISignalDictionaryEntity s = signal.orElse(null);
            return buildSignalResult(fsuId, c.getDeviceId(), c.getDeviceCode(), c.getDeviceName(),
                    firstNonBlank(spid, normalizedSignalId), normalizedSignalId, value, s, c.getSignalName(),
                    c.getMappingStatus(), c.getConfidence(), c.getTemplateVariant(),
                    Boolean.TRUE.equals(c.getNeedRealDataConfirm()), Boolean.TRUE.equals(c.getVerifiedByRealData()),
                    "device_signal_candidate", null);
        }
        if (signal.isPresent()) {
            EStoneIISignalDictionaryEntity s = signal.get();
            return buildSignalResult(fsuId, deviceId, deviceCode, null,
                    firstNonBlank(spid, normalizedSignalId), normalizedSignalId, value, s, null,
                    "TEMPLATE_ONLY", s.getMappingConfidence(), s.getTemplateVariant(),
                    true, false, "signal_dictionary_fallback", null);
        }
        Optional<BInterface2016StandardSignalIndexService.StandardSignal> standardSignal =
                standard2016SignalIndexService.findBySignalId(normalizedSignalId);
        if (standardSignal.isPresent()) {
            return buildStandard2016SignalResult(fsuId, deviceId, deviceCode,
                    firstNonBlank(spid, normalizedSignalId), normalizedSignalId, value,
                    standardSignal.get(), null, null);
        }
        return unmapped(fsuId, deviceId, deviceCode, spid, signalId, value, "UNKNOWN_SIGNAL_ID");
    }

    public EStoneIIMappingResult resolveAlarm(String fsuId, String deviceId, String deviceCode,
                                              String spid, String signalId, String eventId,
                                              String alarmValue) {
        importService.importIfNeeded();
        String normalizedSignalId = firstNonBlank(signalId, spid);
        Optional<EStoneIISignalDictionaryEntity> signal = !isBlank(normalizedSignalId)
                ? signalRepository.findBySignalId(normalizedSignalId) : Optional.empty();
        Optional<EStoneIIEventDictionaryEntity> explicitEvent = !isBlank(eventId)
                ? eventRepository.findByEventId(eventId) : Optional.empty();
        Optional<EStoneIIEventDictionaryEntity> event = explicitEvent.isPresent()
                ? explicitEvent : findEvent(null, spid, normalizedSignalId);
        Optional<DeviceSignalCandidateEntity> candidate = !isBlank(normalizedSignalId)
                ? findCandidate(fsuId, deviceId, deviceCode, normalizedSignalId) : Optional.empty();
        String reason = !isBlank(eventId) && explicitEvent.isEmpty() ? "UNKNOWN_EVENT_ID" : null;

        if (signal.isEmpty() && event.isEmpty()) {
            Optional<BInterface2016StandardSignalIndexService.StandardSignal> standardSignal =
                    standard2016SignalIndexService.findBySignalId(normalizedSignalId);
            if (standardSignal.isPresent()) {
                return buildStandard2016SignalResult(fsuId, deviceId, deviceCode,
                        firstNonBlank(spid, normalizedSignalId), normalizedSignalId, alarmValue,
                        standardSignal.get(), firstNonBlank(eventId, normalizedSignalId),
                        !isBlank(eventId) ? "UNKNOWN_EVENT_ID" : null);
            }
            return unmapped(fsuId, deviceId, deviceCode, spid, signalId, alarmValue,
                    !isBlank(eventId) ? "UNKNOWN_EVENT_ID" : "UNKNOWN_SIGNAL_ID");
        }

        DeviceSignalCandidateEntity c = candidate.orElse(null);
        EStoneIISignalDictionaryEntity s = signal.orElse(null);
        EStoneIIEventDictionaryEntity e = event.orElse(null);
        String status = c != null ? c.getMappingStatus() : "TEMPLATE_ONLY";
        String confidence = c != null ? c.getConfidence() : (s != null ? s.getMappingConfidence() : "TEMPLATE_ONLY");
        String variant = c != null ? c.getTemplateVariant()
                : (s != null ? s.getTemplateVariant() : e != null ? e.getTemplateVariant() : "UNKNOWN");
        boolean needConfirm = c != null ? Boolean.TRUE.equals(c.getNeedRealDataConfirm()) : true;
        boolean verified = c != null && Boolean.TRUE.equals(c.getVerifiedByRealData());
        return new EStoneIIMappingResult(fsuId, firstNonBlank(c != null ? c.getDeviceId() : null, deviceId),
                firstNonBlank(c != null ? c.getDeviceCode() : null, deviceCode),
                firstNonBlank(c != null ? c.getDeviceName() : null, resolveDeviceName(fsuId, deviceId, deviceCode)),
                firstNonBlank(spid, normalizedSignalId), normalizedSignalId,
                firstNonBlank(s != null ? s.getSignalName() : null, c != null ? c.getSignalName() : null),
                s != null ? s.getUnit() : null,
                alarmValue,
                s != null ? parser.meaningOf(s.getSignalMeaningsRaw(), alarmValue) : null,
                s != null ? normalizeSignalCategory(s.getSignalCategory()) : null,
                s != null ? normalizeSignalType(s.getSignalCategory(), s.getSignalType()) : null,
                firstNonBlank(eventId, e != null ? e.getEventId() : null, normalizedSignalId),
                e != null ? e.getEventName() : null,
                e != null ? e.getMeanings() : null,
                e != null ? e.getEventSeverity() : null,
                status, confidence, variant, needConfirm, verified,
                s != null && Boolean.TRUE.equals(s.getDerived()),
                c != null ? "device_signal_candidate" : "dictionary_fallback", reason);
    }

    private EStoneIIMappingResult buildStandard2016SignalResult(String fsuId, String deviceId,
                                                                String deviceCode, String spid,
                                                                String signalId, String value,
                                                                BInterface2016StandardSignalIndexService.StandardSignal s,
                                                                String eventId, String reason) {
        return new EStoneIIMappingResult(fsuId, deviceId, deviceCode,
                resolveDeviceName(fsuId, deviceId, deviceCode), spid, signalId,
                s.signalName(),
                s.unit(),
                value,
                parser.meaningOf(s.valueMeaningsRaw(), value),
                s.signalCategory(),
                s.signalType(),
                eventId, null, null, null,
                "MAPPED_CANDIDATE", "LOW", BInterface2016StandardSignalIndexService.SOURCE,
                true, false, false,
                BInterface2016StandardSignalIndexService.SOURCE, reason);
    }

    private Optional<DeviceSignalCandidateEntity> findCandidate(String fsuId, String deviceId,
                                                               String deviceCode, String signalId) {
        if (!isBlank(fsuId) && !isBlank(deviceId)) {
            Optional<DeviceSignalCandidateEntity> r =
                    candidateRepository.findFirstByFsuIdAndDeviceIdAndSignalId(fsuId, deviceId, signalId);
            if (r.isPresent()) return r;
        }
        if (!isBlank(fsuId) && !isBlank(deviceCode)) {
            Optional<DeviceSignalCandidateEntity> r =
                    candidateRepository.findFirstByFsuIdAndDeviceCodeAndSignalId(fsuId, deviceCode, signalId);
            if (r.isPresent()) return r;
        }
        if (!isBlank(deviceId)) {
            Optional<DeviceSignalCandidateEntity> r =
                    candidateRepository.findFirstByDeviceIdAndSignalId(deviceId, signalId);
            if (r.isPresent()) return r;
        }
        if (!isBlank(deviceCode)) {
            return candidateRepository.findFirstByDeviceCodeAndSignalId(deviceCode, signalId);
        }
        return Optional.empty();
    }

    private Optional<EStoneIIEventDictionaryEntity> findEvent(String eventId, String spid, String signalId) {
        for (String candidate : new String[]{eventId, spid, signalId}) {
            if (!isBlank(candidate)) {
                Optional<EStoneIIEventDictionaryEntity> r = eventRepository.findByEventId(candidate);
                if (r.isPresent()) return r;
            }
        }
        if (!isBlank(signalId)) return eventRepository.findFirstBySignalId(signalId);
        return Optional.empty();
    }

    private EStoneIIMappingResult buildSignalResult(String fsuId, String deviceId, String deviceCode,
                                                    String deviceName, String spid, String signalId, String value,
                                                    EStoneIISignalDictionaryEntity s, String fallbackSignalName, String status,
                                                    String confidence, String variant, boolean needConfirm,
                                                    boolean verified, String source, String reason) {
        return new EStoneIIMappingResult(fsuId, deviceId, deviceCode, deviceName, spid, signalId,
                firstNonBlank(s != null ? s.getSignalName() : null, fallbackSignalName),
                s != null ? s.getUnit() : null,
                value,
                s != null ? parser.meaningOf(s.getSignalMeaningsRaw(), value) : null,
                s != null ? normalizeSignalCategory(s.getSignalCategory()) : null,
                s != null ? normalizeSignalType(s.getSignalCategory(), s.getSignalType()) : null,
                null, null, null, null,
                status, confidence, variant, needConfirm, verified,
                s != null && Boolean.TRUE.equals(s.getDerived()), source, reason);
    }

    private EStoneIIMappingResult unmapped(String fsuId, String deviceId, String deviceCode,
                                           String spid, String signalId, String value, String reason) {
        return new EStoneIIMappingResult(fsuId, deviceId, deviceCode,
                resolveDeviceName(fsuId, deviceId, deviceCode), spid, signalId,
                null, null, value, null, null, null, null, null, null, null,
                "UNMAPPED", "UNKNOWN", "UNKNOWN", true, false, false,
                "unmapped_observation", reason);
    }

    private String resolveDeviceName(String fsuId, String deviceId, String deviceCode) {
        Optional<DeviceSignalCandidateEntity> candidate = Optional.empty();
        if (!isBlank(fsuId) && !isBlank(deviceId)) {
            candidate = candidateRepository.findFirstByFsuIdAndDeviceId(fsuId, deviceId);
        }
        if (candidate.isEmpty() && !isBlank(fsuId) && !isBlank(deviceCode)) {
            candidate = candidateRepository.findFirstByFsuIdAndDeviceCode(fsuId, deviceCode);
        }
        if (candidate.isEmpty() && !isBlank(deviceId)) {
            candidate = candidateRepository.findFirstByDeviceId(deviceId);
        }
        if (candidate.isEmpty() && !isBlank(deviceCode)) {
            candidate = candidateRepository.findFirstByDeviceCode(deviceCode);
        }
        return candidate.map(DeviceSignalCandidateEntity::getDeviceName)
                .filter(v -> !isBlank(v))
                .orElse(null);
    }

    public static String normalizeSignalType(String signalCategory, String signalType) {
        if ("1".equals(signalCategory)) return "AI";
        if ("2".equals(signalCategory)) return "DI";
        if ("3".equals(signalCategory)) return "DO";
        if ("4".equals(signalCategory)) return "AO";
        return firstNonBlank(signalType, signalCategory);
    }

    public static String normalizeSignalCategory(String signalCategory) {
        if ("1".equals(signalCategory)) return "AI/模拟量";
        if ("2".equals(signalCategory)) return "DI/状态量";
        if ("3".equals(signalCategory)) return "DO/控制量";
        if ("4".equals(signalCategory)) return "AO/调节量";
        return signalCategory;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (!isBlank(v)) return v.trim();
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
