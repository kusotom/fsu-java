package com.dcim.platform.module.mapping.service;

import com.dcim.platform.module.mapping.entity.UnmappedSignalObservationEntity;
import com.dcim.platform.module.mapping.repository.UnmappedSignalObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UnmappedSignalObservationService {

    private final UnmappedSignalObservationRepository repository;

    public UnmappedSignalObservationService(UnmappedSignalObservationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void record(String fsuId, String deviceId, String deviceCode, String spid,
                       String signalId, String rawId, String rawName, String rawValue,
                       String unit, String sourceCommand, Long messageLogId,
                       String rawSampleId, String reason) {
        String effectiveSignalId = firstNonBlank(signalId, spid, rawId);
        if (effectiveSignalId.isEmpty() && isGetLoginInfo(sourceCommand)) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        UnmappedSignalObservationEntity entity = repository
                .findFirstByFsuIdAndDeviceIdAndSignalIdAndRawIdAndSourceCommandAndReason(
                        nullToEmpty(fsuId), nullToEmpty(deviceId), nullToEmpty(effectiveSignalId),
                        nullToEmpty(rawId), nullToEmpty(sourceCommand), nullToEmpty(reason))
                .orElseGet(UnmappedSignalObservationEntity::new);
        if (entity.getFirstSeenAt() == null) entity.setFirstSeenAt(now);
        entity.setFsuId(nullToEmpty(fsuId));
        entity.setDeviceId(nullToEmpty(deviceId));
        entity.setDeviceCode(deviceCode);
        entity.setSpid(spid);
        entity.setSignalId(effectiveSignalId);
        entity.setRawId(rawId);
        entity.setRawName(rawName);
        entity.setRawValue(rawValue);
        entity.setUnit(unit);
        entity.setSourceCommand(sourceCommand);
        entity.setMessageLogId(messageLogId);
        entity.setRawSampleId(rawSampleId);
        entity.setLastSeenAt(now);
        entity.setSeenCount(entity.getSeenCount() == null ? 1 : entity.getSeenCount() + 1);
        entity.setReason(nullToEmpty(reason));
        repository.save(entity);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isGetLoginInfo(String sourceCommand) {
        return sourceCommand != null && "GET_LOGININFO".equalsIgnoreCase(sourceCommand.trim());
    }
}
