package com.dcim.platform.module.binterface.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** B接口前端只读 DTO (BACKEND-FE-API-002). */
public class BInterfaceFrontendDtos {

    public static class FsuSummary {
        public Long id; public String fsuCode; public String fsuId; public String stationName;
        public String onlineStatus; public String loginStatus; public String sessionId;
        public String fsuIp; public String scIp; public String macId; public String version;
        public String lastLoginTime; public String lastHeartbeatTime; public String updatedAt;
        public int deviceCount; public int mappedPointCount; public int unmappedDeviceCount;
        public String statusDetail;
    }

    public static class FsuDetail extends FsuSummary {
        public List<DeviceInfo> devices;
        public List<MessageLogSummary> recentMessages;
    }

    public static class DeviceInfo {
        public String fsuCode; public String deviceId; public String deviceCode;
        public String deviceName; public String deviceType; public String source;
        public String mappingStatus = "unknown"; public int signalCount; public String lastSeenAt;
    }

    public static class MessageLogSummary {
        public Long id; public String direction; public String command; public String fsuCode;
        public String messageType; public int rawMessageLength; public String rawMessagePreview;
        public String rawMessage; public String createdAt;
    }

    public static class AlarmDto {
        public Long id; public String fsuCode; public String deviceId; public String deviceCode;
        public String spid; public String signalId; public String serialNo;
        public String eventId; public String signalName; public String eventName; public String alarmMeaning; public String eventSeverity;
        public String alarmLevel; public String alarmStatus; public String alarmDesc; public String alarmType;
        public String mappingStatus; public String mappingConfidence; public String templateVariant;
        public Boolean needRealDataConfirm; public Boolean verifiedByRealData; public String source;
        public String alarmTime; public String recoveryTime; public String createdAt; public String updatedAt;
    }

    public static class RealtimePointDto {
        public String fsuCode; public String deviceId; public String deviceCode;
        public String deviceName;
        public String spid; public String signalId; public String signalName; public String signalType; public String unit;
        public String value; public String valueMeaning; public String rawValue; public String quality;
        public String signalCategory; public String mappingStatus; public String mappingConfidence; public String templateVariant;
        public Boolean needRealDataConfirm; public Boolean verifiedByRealData; public Boolean derived; public String source;
        public Boolean hasSignalIdentity; public Boolean isDeviceOnly; public Boolean legacyData; public String observationType;
        public String collectTime; public String updatedAt;
        public BigDecimal valueNumber; public String valueText;
    }

    public static class UnmappedSignalDto {
        public String fsuCode; public String deviceId; public String deviceCode;
        public String spid; public String signalId; public String source; public String reason;
        public String rawId; public String rawName;
        public String signalName; public String signalType; public String unit; public String valueMeaning;
        public String mappingStatus; public String mappingConfidence; public String templateVariant;
        public Boolean needRealDataConfirm; public Boolean verifiedByRealData;
        public Boolean hasSignalIdentity; public Boolean isDeviceOnly; public Boolean legacyData; public String observationType;
        public String rawCommand; public String rawValue; public String firstSeenAt; public String lastSeenAt; public int seenCount;
    }

    public static class ThresholdDto {
        public String fsuCode; public String deviceId; public String deviceCode;
        public String spid; public String signalId; public String signalName; public String signalType; public String unit;
        public String threshold; public String absoluteVal; public String relativeVal; public String status;
        public String mappingStatus; public String source; public String updatedAt;
    }
}
