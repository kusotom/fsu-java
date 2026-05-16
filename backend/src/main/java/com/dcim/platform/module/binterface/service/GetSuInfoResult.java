package com.dcim.platform.module.binterface.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GET_SUINFO / GET_SUINFO_ACK 在线状态查询结果。
 *
 * <p>BIF-P4-012: 基于 B接口 2024 协议 GET_SUINFO (Code=1001) 命令。
 * SC 主动查询 FSU/SU 的运行状态（CPU/内存/本地时间），用于心跳和在线状态维护。</p>
 */
public class GetSuInfoResult {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String suid;
    private final BigDecimal cpuUsage;
    private final BigDecimal memUsage;
    private final LocalDateTime suDateTime;
    private final List<String> errors;

    private GetSuInfoResult(boolean success, String resultCode, String resultDesc,
                            String suid, BigDecimal cpuUsage, BigDecimal memUsage,
                            LocalDateTime suDateTime, List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.suid = suid;
        this.cpuUsage = cpuUsage;
        this.memUsage = memUsage;
        this.suDateTime = suDateTime;
        this.errors = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
    }

    // ==================== 工厂方法 ====================

    public static GetSuInfoResult success(String suid, BigDecimal cpuUsage,
                                           BigDecimal memUsage, LocalDateTime suDateTime) {
        return new GetSuInfoResult(true, "0", "OK", suid, cpuUsage, memUsage, suDateTime, List.of());
    }

    public static GetSuInfoResult fail(String resultCode, String desc) {
        return new GetSuInfoResult(false, resultCode, desc, null, null, null, null, List.of(desc));
    }

    public static GetSuInfoResult fail(String resultCode, String desc, String suid) {
        return new GetSuInfoResult(false, resultCode, desc, suid, null, null, null, List.of(desc));
    }

    public static GetSuInfoResult fail(String resultCode, String desc, String suid, List<String> errors) {
        return new GetSuInfoResult(false, resultCode, desc, suid, null, null, null, errors);
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getSuid() { return suid; }
    public BigDecimal getCpuUsage() { return cpuUsage; }
    public BigDecimal getMemUsage() { return memUsage; }
    public LocalDateTime getSuDateTime() { return suDateTime; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "GetSuInfoResult{success=" + success + ", suid=" + suid
                + ", cpu=" + cpuUsage + ", mem=" + memUsage
                + ", suDateTime=" + suDateTime + "}";
    }
}
