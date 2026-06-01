package com.dcim.platform.module.binterface.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * B接口2016 GET_FSUINFO 心跳轮询结果。
 */
public class BInterface2016GetFsuInfoResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String fsuCode;
    private final String fsuId;
    private final BigDecimal cpuUsage;
    private final BigDecimal memUsage;
    private final boolean realDeviceAccessed;
    private final boolean statusUpdated;
    private final String rawRequest;
    private final String rawResponse;

    private BInterface2016GetFsuInfoResult(Builder builder) {
        this.success = builder.success;
        this.resultCode = builder.resultCode;
        this.resultDesc = builder.resultDesc;
        this.fsuCode = builder.fsuCode;
        this.fsuId = builder.fsuId;
        this.cpuUsage = builder.cpuUsage;
        this.memUsage = builder.memUsage;
        this.realDeviceAccessed = builder.realDeviceAccessed;
        this.statusUpdated = builder.statusUpdated;
        this.rawRequest = builder.rawRequest;
        this.rawResponse = builder.rawResponse;
    }

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getFsuCode() { return fsuCode; }
    public String getFsuId() { return fsuId; }
    public BigDecimal getCpuUsage() { return cpuUsage; }
    public BigDecimal getMemUsage() { return memUsage; }
    public boolean isRealDeviceAccessed() { return realDeviceAccessed; }
    public boolean isStatusUpdated() { return statusUpdated; }
    public String getRawRequest() { return rawRequest; }
    public String getRawResponse() { return rawResponse; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private boolean success;
        private String resultCode = "0";
        private String resultDesc = "OK";
        private String fsuCode;
        private String fsuId;
        private BigDecimal cpuUsage;
        private BigDecimal memUsage;
        private boolean realDeviceAccessed;
        private boolean statusUpdated;
        private String rawRequest;
        private String rawResponse;

        public Builder success(boolean v) { this.success = v; return this; }
        public Builder resultCode(String v) { this.resultCode = v; return this; }
        public Builder resultDesc(String v) { this.resultDesc = v; return this; }
        public Builder fsuCode(String v) { this.fsuCode = v; return this; }
        public Builder fsuId(String v) { this.fsuId = v; return this; }
        public Builder cpuUsage(BigDecimal v) { this.cpuUsage = v; return this; }
        public Builder memUsage(BigDecimal v) { this.memUsage = v; return this; }
        public Builder realDeviceAccessed(boolean v) { this.realDeviceAccessed = v; return this; }
        public Builder statusUpdated(boolean v) { this.statusUpdated = v; return this; }
        public Builder rawRequest(String v) { this.rawRequest = v; return this; }
        public Builder rawResponse(String v) { this.rawResponse = v; return this; }

        public BInterface2016GetFsuInfoResult build() { return new BInterface2016GetFsuInfoResult(this); }
    }

    // ==================== 工厂方法 ====================

    public static BInterface2016GetFsuInfoResult success(String fsuCode, String fsuId,
                                                          BigDecimal cpu, BigDecimal mem,
                                                          boolean realDeviceAccessed, boolean statusUpdated,
                                                          String rawRequest, String rawResponse) {
        return builder()
                .success(true).resultCode("0").resultDesc("OK")
                .fsuCode(fsuCode).fsuId(fsuId)
                .cpuUsage(cpu).memUsage(mem)
                .realDeviceAccessed(realDeviceAccessed)
                .statusUpdated(statusUpdated)
                .rawRequest(rawRequest).rawResponse(rawResponse)
                .build();
    }

    public static BInterface2016GetFsuInfoResult fail(String code, String desc, String fsuCode) {
        return fail(code, desc, fsuCode, false);
    }

    public static BInterface2016GetFsuInfoResult fail(String code, String desc, String fsuCode,
                                                       boolean realDeviceAccessed) {
        return builder()
                .success(false).resultCode(code).resultDesc(desc)
                .fsuCode(fsuCode)
                .realDeviceAccessed(realDeviceAccessed)
                .build();
    }
}
