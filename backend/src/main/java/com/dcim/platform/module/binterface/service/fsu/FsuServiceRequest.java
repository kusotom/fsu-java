package com.dcim.platform.module.binterface.service.fsu;

import com.dcim.platform.module.binterface.model.BInterfacePkType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * FSU 服务调用请求。
 *
 * <p>封装发往 FSU（FSUService）的 SOAP 调用所需参数。</p>
 */
public class FsuServiceRequest {

    private final String fsuCode;
    private final String serviceUrl;
    private final BInterfacePkType pkType;
    private final String infoXml;
    private final String xmlDataXml;
    private final Duration timeout;
    private final String pkTypeFormat;
    private final Map<String, Object> attributes = new HashMap<>();

    private FsuServiceRequest(String fsuCode, String serviceUrl, BInterfacePkType pkType,
                              String infoXml, String xmlDataXml, Duration timeout,
                              String pkTypeFormat) {
        this.fsuCode = fsuCode;
        this.serviceUrl = serviceUrl;
        this.pkType = pkType;
        this.infoXml = infoXml;
        this.xmlDataXml = xmlDataXml;
        this.timeout = timeout;
        this.pkTypeFormat = pkTypeFormat;
    }

    public static FsuServiceRequestBuilder builder() {
        return new FsuServiceRequestBuilder();
    }

    // ==================== Getters ====================

    public String getFsuCode() { return fsuCode; }
    public String getServiceUrl() { return serviceUrl; }
    public BInterfacePkType getPkType() { return pkType; }
    public String getInfoXml() { return infoXml; }
    public String getXmlDataXml() { return xmlDataXml; }
    public Duration getTimeout() { return timeout; }
    public String getPkTypeFormat() { return pkTypeFormat; }
    public Map<String, Object> getAttributes() { return attributes; }

    public void setAttribute(String key, Object value) { attributes.put(key, value); }
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) { return (T) attributes.get(key); }

    // ==================== Builder ====================

    public static class FsuServiceRequestBuilder {
        private String fsuCode;
        private String serviceUrl;
        private BInterfacePkType pkType;
        private String infoXml;
        private String xmlDataXml;
        private Duration timeout = Duration.ofSeconds(10);
        private String pkTypeFormat;

        FsuServiceRequestBuilder() {}

        public FsuServiceRequestBuilder fsuCode(String fsuCode) { this.fsuCode = fsuCode; return this; }
        public FsuServiceRequestBuilder serviceUrl(String serviceUrl) { this.serviceUrl = serviceUrl; return this; }
        public FsuServiceRequestBuilder pkType(BInterfacePkType pkType) { this.pkType = pkType; return this; }
        public FsuServiceRequestBuilder infoXml(String infoXml) { this.infoXml = infoXml; return this; }
        public FsuServiceRequestBuilder xmlDataXml(String xmlDataXml) { this.xmlDataXml = xmlDataXml; return this; }
        public FsuServiceRequestBuilder timeout(Duration timeout) { this.timeout = timeout; return this; }

        /**
         * 设置 PK_Type 输出格式。null 或 "structured" 使用 2024 Name+Code 格式；
         * "legacy-text" 使用旧版纯文本格式。
         */
        public FsuServiceRequestBuilder pkTypeFormat(String pkTypeFormat) {
            this.pkTypeFormat = pkTypeFormat; return this;
        }

        public FsuServiceRequest build() {
            if (fsuCode == null) throw new IllegalStateException("fsuCode must not be null");
            if (pkType == null) throw new IllegalStateException("pkType must not be null");
            return new FsuServiceRequest(fsuCode, serviceUrl, pkType, infoXml, xmlDataXml,
                    timeout, pkTypeFormat);
        }
    }
}
