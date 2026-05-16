package com.dcim.platform.module.binterface.service.fsu;

import com.dcim.platform.module.binterface.xml.XmlDataModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FSU 服务调用响应。
 *
 * <p>封装 FSUService 调用的返回结果，包括结构化 xmlData 和原始 SOAP。</p>
 */
public class FsuServiceResponse {

    private final boolean success;
    private final String resultCode;
    private final String resultDesc;
    private final String rawSoap;
    private final String infoXml;
    private final String xmlDataXml;
    private final XmlDataModel xmlData;
    private final boolean realCall;
    private final List<String> errors;

    private FsuServiceResponse(boolean success, String resultCode, String resultDesc,
                                String rawSoap, String infoXml,
                                String xmlDataXml, XmlDataModel xmlData,
                                boolean realCall, List<String> errors) {
        this.success = success;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.rawSoap = rawSoap;
        this.infoXml = infoXml;
        this.xmlDataXml = xmlDataXml;
        this.xmlData = xmlData;
        this.realCall = realCall;
        this.errors = errors;
    }

    // ==================== 工厂方法 ====================

    public static FsuServiceResponse success(String rawSoap, String infoXml,
                                              String xmlDataXml, XmlDataModel xmlData) {
        return new FsuServiceResponse(true, "0", "OK", rawSoap, infoXml,
                xmlDataXml, xmlData, false, List.of());
    }

    public static FsuServiceResponse successReal(String rawSoap, String infoXml,
                                                  String xmlDataXml, XmlDataModel xmlData) {
        return new FsuServiceResponse(true, "0", "OK", rawSoap, infoXml,
                xmlDataXml, xmlData, true, List.of());
    }

    public static FsuServiceResponse fail(String resultCode, String desc) {
        return new FsuServiceResponse(false, resultCode, desc, null, null,
                null, null, false, List.of(desc));
    }

    public static FsuServiceResponse fail(String resultCode, String desc, List<String> errors) {
        return new FsuServiceResponse(false, resultCode, desc, null, null,
                null, null, false, errors);
    }

    public static FsuServiceResponse error(String desc) {
        return new FsuServiceResponse(false, "5001", desc, null, null,
                null, null, false, List.of(desc));
    }

    public static FsuServiceResponse disabled() {
        return new FsuServiceResponse(false, "5001", "真实 FSU 调用已被禁用: real-call-enabled=false",
                null, null, null, null, false,
                List.of("真实 FSU 调用已被禁用，请设置 b-interface.fsu-client.real-call-enabled=true"));
    }

    // ==================== Getters ====================

    public boolean isSuccess() { return success; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public String getRawSoap() { return rawSoap; }
    public String getInfoXml() { return infoXml; }
    public String getXmlDataXml() { return xmlDataXml; }
    public XmlDataModel getXmlData() { return xmlData; }
    public boolean isRealCall() { return realCall; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }

    @Override
    public String toString() {
        return "FsuServiceResponse{success=" + success + ", resultCode=" + resultCode
                + ", realCall=" + realCall + ", errors=" + errors + "}";
    }
}
