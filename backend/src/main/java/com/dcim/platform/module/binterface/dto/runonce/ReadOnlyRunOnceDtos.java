package com.dcim.platform.module.binterface.dto.runonce;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FAST-REALDATA-001: 只读 run-once API 的请求/响应 DTO。
 * 仅用于只读 GET_FSUINFO / GET_DATA probe，不写正式点位。
 */
public final class ReadOnlyRunOnceDtos {

    private ReadOnlyRunOnceDtos() {}

    // ==================== FSU Info ====================

    public static class FsuInfoRunOnceRequest {
        private String fsuCode = "51051243812345";
        private String operator;
        private String reason;
        private boolean confirmReadOnly;

        public String getFsuCode() { return fsuCode; }
        public void setFsuCode(String v) { this.fsuCode = v; }
        public String getOperator() { return operator; }
        public void setOperator(String v) { this.operator = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }
        public boolean isConfirmReadOnly() { return confirmReadOnly; }
        public void setConfirmReadOnly(boolean v) { this.confirmReadOnly = v; }
    }

    public static class FsuInfoRunOnceResponse {
        private boolean success;
        private String fsuCode;
        private BigDecimal cpuUsage;
        private BigDecimal memUsage;
        private boolean realDeviceAccessed;
        private boolean statusUpdated;
        private String readTime;
        private String source = "real-fsu-get-fsuinfo-2016";
        private boolean readOnly = true;
        private String resultCode;
        private String resultDesc;
        private String rawSamplePath;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getFsuCode() { return fsuCode; }
        public void setFsuCode(String v) { this.fsuCode = v; }
        public BigDecimal getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(BigDecimal v) { this.cpuUsage = v; }
        public BigDecimal getMemUsage() { return memUsage; }
        public void setMemUsage(BigDecimal v) { this.memUsage = v; }
        public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
        public void setRealDeviceAccessed(boolean v) { this.realDeviceAccessed = v; }
        public boolean isStatusUpdated() { return statusUpdated; }
        public void setStatusUpdated(boolean v) { this.statusUpdated = v; }
        public String getReadTime() { return readTime; }
        public void setReadTime(String v) { this.readTime = v; }
        public String getSource() { return source; }
        public boolean isReadOnly() { return readOnly; }
        public String getResultCode() { return resultCode; }
        public void setResultCode(String v) { this.resultCode = v; }
        public String getResultDesc() { return resultDesc; }
        public void setResultDesc(String v) { this.resultDesc = v; }
        public String getRawSamplePath() { return rawSamplePath; }
        public void setRawSamplePath(String v) { this.rawSamplePath = v; }
    }

    // ==================== GET_DATA Probe ====================

    public static class GetDataProbeRequest {
        private String fsuCode = "51051243812345";
        private List<String> deviceIds;
        private String operator;
        private String reason;
        private boolean confirmReadOnly;
        private int maxSignalsPerDevice = 30;
        /** 策略: "device-list"/"simple-signalid"/"single-device" 等 */
        private String strategy;
        /** 是否使用 LOGIN 注册上下文 (默认 true — 优先) */
        private boolean useRegistrationContext = true;

        public String getFsuCode() { return fsuCode; }
        public void setFsuCode(String v) { this.fsuCode = v; }
        public List<String> getDeviceIds() { return deviceIds; }
        public void setDeviceIds(List<String> v) { this.deviceIds = v; }
        public String getOperator() { return operator; }
        public void setOperator(String v) { this.operator = v; }
        public String getReason() { return reason; }
        public void setReason(String v) { this.reason = v; }
        public boolean isConfirmReadOnly() { return confirmReadOnly; }
        public void setConfirmReadOnly(boolean v) { this.confirmReadOnly = v; }
        public int getMaxSignalsPerDevice() { return maxSignalsPerDevice; }
        public void setMaxSignalsPerDevice(int v) { this.maxSignalsPerDevice = v; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String v) { this.strategy = v; }
        public boolean isUseRegistrationContext() { return useRegistrationContext; }
        public void setUseRegistrationContext(boolean v) { this.useRegistrationContext = v; }
    }

    public static class GetDataProbeResponse {
        private boolean success;
        private String fsuCode;
        private boolean readOnly = true;
        private boolean realDeviceAccessed;
        private boolean emptyData;
        private boolean ackReceived;
        private String probeTime;
        private String resultCode;
        private String resultDesc;
        private String emptyDataMessage;
        private String rawSamplePath;
        private List<DeviceProbeResult> devices = new ArrayList<>();

        // 2016 protocol metadata
        private String protocolVersion = "B接口2016";
        private String strategyName;
        private boolean httpSuccess;
        private int ackCode;
        private String fsuResultRaw;
        private String fsuResultMeaning;
        private Boolean businessSuccess;
        private boolean deviceListPresent;
        private int valuesReturned;

        // BIF2016-CONNECTION-003-FIX-001: Result semantics additions
        private String protocolResultMeaning;
        private String statusText;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean v) { this.success = v; }
        public String getFsuCode() { return fsuCode; }
        public void setFsuCode(String v) { this.fsuCode = v; }
        public boolean isReadOnly() { return readOnly; }
        public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
        public void setRealDeviceAccessed(boolean v) { this.realDeviceAccessed = v; }
        public boolean isEmptyData() { return emptyData; }
        public void setEmptyData(boolean v) { this.emptyData = v; }
        public boolean isAckReceived() { return ackReceived; }
        public void setAckReceived(boolean v) { this.ackReceived = v; }
        public String getProbeTime() { return probeTime; }
        public void setProbeTime(String v) { this.probeTime = v; }
        public String getResultCode() { return resultCode; }
        public void setResultCode(String v) { this.resultCode = v; }
        public String getResultDesc() { return resultDesc; }
        public void setResultDesc(String v) { this.resultDesc = v; }
        public String getEmptyDataMessage() { return emptyDataMessage; }
        public void setEmptyDataMessage(String v) { this.emptyDataMessage = v; }
        public String getRawSamplePath() { return rawSamplePath; }
        public void setRawSamplePath(String v) { this.rawSamplePath = v; }
        public List<DeviceProbeResult> getDevices() { return devices; }
        public void setDevices(List<DeviceProbeResult> v) { this.devices = v; }

        public String getProtocolVersion() { return protocolVersion; }
        public String getStrategyName() { return strategyName; }
        public void setStrategyName(String v) { this.strategyName = v; }
        public boolean isHttpSuccess() { return httpSuccess; }
        public void setHttpSuccess(boolean v) { this.httpSuccess = v; }
        public int getAckCode() { return ackCode; }
        public void setAckCode(int v) { this.ackCode = v; }
        public String getFsuResultRaw() { return fsuResultRaw; }
        public void setFsuResultRaw(String v) { this.fsuResultRaw = v; }
        public String getFsuResultMeaning() { return fsuResultMeaning; }
        public void setFsuResultMeaning(String v) { this.fsuResultMeaning = v; }
        public Boolean getBusinessSuccess() { return businessSuccess; }
        public void setBusinessSuccess(Boolean v) { this.businessSuccess = v; }
        public boolean isDeviceListPresent() { return deviceListPresent; }
        public void setDeviceListPresent(boolean v) { this.deviceListPresent = v; }
        public int getValuesReturned() { return valuesReturned; }
        public void setValuesReturned(int v) { this.valuesReturned = v; }

        public String getProtocolResultMeaning() { return protocolResultMeaning; }
        public void setProtocolResultMeaning(String v) { this.protocolResultMeaning = v; }
        public String getStatusText() { return statusText; }
        public void setStatusText(String v) { this.statusText = v; }

        // ── BIF2016-CONNECTION-003: registration context metadata ──
        private String targetSource;                          // login_registration_context | manual_probe | static_config
        private boolean registrationContextUsed;
        private String registrationContextCompleteness;
        private List<String> registrationContextMissingFields = new ArrayList<>();
        private String fsuIpSource;                           // login_registration_context | session_fallback | static_config
        private String deviceListSource;                      // login_registration_context | request_manual | static_config
        private int deviceCapabilityCount;
        private boolean manualFallbackUsed;
        private String manualFallbackReason;
        private String errorCode;

        public String getTargetSource() { return targetSource; }
        public void setTargetSource(String v) { this.targetSource = v; }
        public boolean isRegistrationContextUsed() { return registrationContextUsed; }
        public void setRegistrationContextUsed(boolean v) { this.registrationContextUsed = v; }
        public String getRegistrationContextCompleteness() { return registrationContextCompleteness; }
        public void setRegistrationContextCompleteness(String v) { this.registrationContextCompleteness = v; }
        public List<String> getRegistrationContextMissingFields() { return registrationContextMissingFields; }
        public void setRegistrationContextMissingFields(List<String> v) { this.registrationContextMissingFields = v; }
        public String getFsuIpSource() { return fsuIpSource; }
        public void setFsuIpSource(String v) { this.fsuIpSource = v; }
        public String getDeviceListSource() { return deviceListSource; }
        public void setDeviceListSource(String v) { this.deviceListSource = v; }
        public int getDeviceCapabilityCount() { return deviceCapabilityCount; }
        public void setDeviceCapabilityCount(int v) { this.deviceCapabilityCount = v; }
        public boolean isManualFallbackUsed() { return manualFallbackUsed; }
        public void setManualFallbackUsed(boolean v) { this.manualFallbackUsed = v; }
        public String getManualFallbackReason() { return manualFallbackReason; }
        public void setManualFallbackReason(String v) { this.manualFallbackReason = v; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String v) { this.errorCode = v; }
    }

    public static class DeviceProbeResult {
        private String deviceId;
        private String deviceTypeCode;
        private String deviceTypeSource = "protocol-deviceid-inference";
        private String confidence = "medium";
        private int signalsRequested;
        private int valuesReturned;
        private List<SignalValueResult> values = new ArrayList<>();
        // Device.Code fallback
        private boolean deviceCodeFallback;
        private String deviceCodeFallbackReason;

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String v) { this.deviceId = v; }
        public String getDeviceTypeCode() { return deviceTypeCode; }
        public void setDeviceTypeCode(String v) { this.deviceTypeCode = v; }
        public String getDeviceTypeSource() { return deviceTypeSource; }
        public void setDeviceTypeSource(String v) { this.deviceTypeSource = v; }
        public String getConfidence() { return confidence; }
        public void setConfidence(String v) { this.confidence = v; }
        public int getSignalsRequested() { return signalsRequested; }
        public void setSignalsRequested(int v) { this.signalsRequested = v; }
        public int getValuesReturned() { return valuesReturned; }
        public void setValuesReturned(int v) { this.valuesReturned = v; }
        public List<SignalValueResult> getValues() { return values; }
        public void setValues(List<SignalValueResult> v) { this.values = v; }
        public boolean isDeviceCodeFallback() { return deviceCodeFallback; }
        public void setDeviceCodeFallback(boolean v) { this.deviceCodeFallback = v; }
        public String getDeviceCodeFallbackReason() { return deviceCodeFallbackReason; }
        public void setDeviceCodeFallbackReason(String v) { this.deviceCodeFallbackReason = v; }
    }

    public static class SignalValueResult {
        private String signalId;
        private String spid;
        private String signalName;
        private String signalType;
        private String value;
        private String unit;
        private String quality = "real-fsu";

        public String getSignalId() { return signalId; }
        public void setSignalId(String v) { this.signalId = v; }
        public String getSpid() { return spid; }
        public void setSpid(String v) { this.spid = v; }
        public String getSignalName() { return signalName; }
        public void setSignalName(String v) { this.signalName = v; }
        public String getSignalType() { return signalType; }
        public void setSignalType(String v) { this.signalType = v; }
        public String getValue() { return value; }
        public void setValue(String v) { this.value = v; }
        public String getUnit() { return unit; }
        public void setUnit(String v) { this.unit = v; }
        public String getQuality() { return quality; }
        public void setQuality(String v) { this.quality = v; }
    }
}
