package com.dcim.platform.module.binterface.service;

import java.util.ArrayList;
import java.util.List;

/**
 * B接口2016 GET_DATA DeviceList/TSemaphore 查询结果。
 */
public class BInterface2016GetDataResult {

    private final boolean success;
    private final boolean emptyData;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final String fsuId;
    private final boolean realDeviceAccessed;
     private final List<DeviceData> devices;
    private final List<UnmappedEntry> unmapped;
    private final String rawRequest;
    private final String rawResponse;
    private final String rawUnwrapped;

    private BInterface2016GetDataResult(Builder b) {
        this.success = b.success;
        this.emptyData = b.emptyData;
        this.resultCode = b.resultCode;
        this.resultDesc = b.resultDesc;
        this.fsuCode = b.fsuCode;
        this.fsuId = b.fsuId;
        this.realDeviceAccessed = b.realDeviceAccessed;
        this.devices = List.copyOf(b.devices);
        this.unmapped = List.copyOf(b.unmapped);
        this.rawRequest = b.rawRequest;
        this.rawResponse = b.rawResponse;
        this.rawUnwrapped = b.rawUnwrapped;
    }

    public boolean isSuccess() { return success; }
    public boolean isEmptyData() { return emptyData; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public String getFsuId() { return fsuId; }
    public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
    public List<DeviceData> getDevices() { return devices; }
    public List<UnmappedEntry> getUnmapped() { return unmapped; }
    public String getRawRequest() { return rawRequest; }
    public String getRawResponse() { return rawResponse; }
    public String getRawUnwrapped() { return rawUnwrapped; }

    // ==================== 内嵌模型 ====================

    public static class DeviceData {
        private final String deviceId;
        private final String deviceCode;
        private final List<SemaphoreValue> semaphores;

        public DeviceData(String deviceId, String deviceCode, List<SemaphoreValue> semaphores) {
            this.deviceId = deviceId; this.deviceCode = deviceCode;
            this.semaphores = semaphores != null ? semaphores : List.of();
        }
        public String getDeviceId() { return deviceId; }
        public String getDeviceCode() { return deviceCode; }
        public List<SemaphoreValue> getSemaphores() { return semaphores; }
    }

    public static class SemaphoreValue {
        private final String id;
        private final String code;
        private final String measuredVal;
        private final String status;
        private final String time;
        private final boolean matched;

        public SemaphoreValue(String id, String code, String measuredVal,
                               String status, String time, boolean matched) {
            this.id = id; this.code = code;
            this.measuredVal = measuredVal; this.status = status;
            this.time = time; this.matched = matched;
        }
        public String getId() { return id; }
        public String getCode() { return code; }
        public String getMeasuredVal() { return measuredVal; }
        public String getStatus() { return status; }
        public String getTime() { return time; }
        public boolean isMatched() { return matched; }
    }

    public static class UnmappedEntry {
        private final String deviceId;
        private final String spid;
        private final String signalId;
        private final String value;

        public UnmappedEntry(String deviceId, String spid, String signalId, String value) {
            this.deviceId = deviceId; this.spid = spid;
            this.signalId = signalId; this.value = value;
        }
        public String getDeviceId() { return deviceId; }
        public String getSpid() { return spid; }
        public String getSignalId() { return signalId; }
        public String getValue() { return value; }
    }

    // ==================== Builder ====================

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean success;
        private boolean emptyData;
        private String resultCode = "0";
        private String resultDesc = "OK";
        private String fsuCode;
        private String fsuId;
        private boolean realDeviceAccessed;
        private final List<DeviceData> devices = new ArrayList<>();
        private final List<UnmappedEntry> unmapped = new ArrayList<>();
        private String rawRequest;
        private String rawResponse;
        private String rawUnwrapped;

        public Builder success(boolean v) { this.success = v; return this; }
        public Builder emptyData(boolean v) { this.emptyData = v; return this; }
        public Builder resultCode(String v) { this.resultCode = v; return this; }
        public Builder resultDesc(String v) { this.resultDesc = v; return this; }
        public Builder fsuCode(String v) { this.fsuCode = v; return this; }
        public Builder fsuId(String v) { this.fsuId = v; return this; }
        public Builder realDeviceAccessed(boolean v) { this.realDeviceAccessed = v; return this; }
        public Builder addDevice(DeviceData d) { this.devices.add(d); return this; }
        public Builder addUnmapped(UnmappedEntry e) { this.unmapped.add(e); return this; }
        public Builder rawRequest(String v) { this.rawRequest = v; return this; }
        public Builder rawResponse(String v) { this.rawResponse = v; return this; }
        public Builder rawUnwrapped(String v) { this.rawUnwrapped = v; return this; }
        public BInterface2016GetDataResult build() { return new BInterface2016GetDataResult(this); }
    }

    // ==================== 工厂方法 ====================

    public static BInterface2016GetDataResult successEmpty(String fsuCode, String fsuId,
                                                            boolean realDeviceAccessed,
                                                            String rawRequest, String rawResponse) {
        return builder().success(true).emptyData(true)
                .fsuCode(fsuCode).fsuId(fsuId)
                .realDeviceAccessed(realDeviceAccessed)
                .rawRequest(rawRequest).rawResponse(rawResponse)
                .build();
    }

    public static BInterface2016GetDataResult fail(String code, String desc, String fsuCode) {
        return builder().success(false).resultCode(code).resultDesc(desc).fsuCode(fsuCode).build();
    }
}
