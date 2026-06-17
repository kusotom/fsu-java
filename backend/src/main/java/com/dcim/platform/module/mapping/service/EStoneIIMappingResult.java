package com.dcim.platform.module.mapping.service;

public record EStoneIIMappingResult(
        String fsuId,
        String deviceId,
        String deviceCode,
        String deviceName,
        String spid,
        String signalId,
        String signalName,
        String unit,
        String value,
        String valueMeaning,
        String signalCategory,
        String signalType,
        String eventId,
        String eventName,
        String alarmMeaning,
        String eventSeverity,
        String mappingStatus,
        String mappingConfidence,
        String templateVariant,
        boolean needRealDataConfirm,
        boolean verifiedByRealData,
        boolean derived,
        String source,
        String reason
) {
    public boolean mapped() {
        return mappingStatus != null && !"UNMAPPED".equals(mappingStatus);
    }
}
