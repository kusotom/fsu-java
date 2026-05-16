package com.dcim.platform.module.binterface.service.fsu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FSU endpoint 解析结果。
 *
 * <p>封装从 FSU 编码解析 FSUService URL 的结果，包含来源标识和错误详情。</p>
 */
public class FsuEndpointResult {

    private final boolean success;
    private final String fsuCode;
    private final String serviceUrl;
    private final String resultCode;
    private final String resultDesc;
    private final List<String> errors;
    private final boolean fromExplicitServiceUrl;
    private final boolean fromHostPort;

    private FsuEndpointResult(boolean success, String fsuCode, String serviceUrl,
                              String resultCode, String resultDesc,
                              List<String> errors,
                              boolean fromExplicitServiceUrl, boolean fromHostPort) {
        this.success = success;
        this.fsuCode = fsuCode;
        this.serviceUrl = serviceUrl;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.errors = errors != null
                ? Collections.unmodifiableList(new ArrayList<>(errors))
                : List.of();
        this.fromExplicitServiceUrl = fromExplicitServiceUrl;
        this.fromHostPort = fromHostPort;
    }

    // ==================== Static factories ====================

    public static FsuEndpointResult fromExplicitServiceUrl(String fsuCode, String serviceUrl) {
        return new FsuEndpointResult(true, fsuCode, serviceUrl,
                "0", "成功", null, true, false);
    }

    public static FsuEndpointResult fromHostPort(String fsuCode, String serviceUrl) {
        return new FsuEndpointResult(true, fsuCode, serviceUrl,
                "0", "成功", null, false, true);
    }

    public static FsuEndpointResult fail(String fsuCode, String resultCode, String resultDesc) {
        return new FsuEndpointResult(false, fsuCode, null,
                resultCode, resultDesc, List.of(resultDesc), false, false);
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getFsuCode() { return fsuCode; }
    public String getServiceUrl() { return serviceUrl; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public List<String> getErrors() { return errors; }
    public boolean isFromExplicitServiceUrl() { return fromExplicitServiceUrl; }
    public boolean isFromHostPort() { return fromHostPort; }

    @Override
    public String toString() {
        return "FsuEndpointResult{" +
                "success=" + success +
                ", fsuCode='" + fsuCode + '\'' +
                ", serviceUrl='" + serviceUrl + '\'' +
                ", resultCode='" + resultCode + '\'' +
                ", fromExplicitServiceUrl=" + fromExplicitServiceUrl +
                ", fromHostPort=" + fromHostPort +
                '}';
    }
}
