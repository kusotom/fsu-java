package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResolver;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResult;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GET_DATA 查询服务。
 *
 * <p>SC 主动向 FSU 查询实时监控数据。
 * 通过 FsuServiceClient 发送 SOAP 请求到 FSUService，解析响应返回信号值列表。</p>
 *
 * <p>与 SEND_DATA 边界：</p>
 * <ul>
 *   <li>GET_DATA：SC→FSU，查询方向，只读</li>
 *   <li>SEND_DATA：FSU→SC，上报方向，含入库持久化</li>
 *   <li>本服务不调用 SendDataService 或 SendAlarmService</li>
 * </ul>
 */
@Service
public class GetDataService {

    private static final Logger log = LoggerFactory.getLogger(GetDataService.class);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public GetDataService(FsuServiceClient fsuServiceClient, FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    /**
     * 执行 GET_DATA 查询。
     *
     * @param fsuCode     FSU 编码
     * @param fsuServiceUrl FSU 服务地址（可为 null，由 FsuServiceClient 决定默认值）
     * @param signalIds   要查询的信号 ID 列表
     * @return 查询结果
     */
    public GetDataResult execute(String fsuCode, String fsuServiceUrl, List<String> signalIds) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return GetDataResult.fail("2001", "缺少 FSUCode");
        }
        if (signalIds == null || signalIds.isEmpty()) {
            return GetDataResult.fail("2003", "缺少 SignalID");
        }

        // 解析 FSU endpoint（未显式指定时从数据库获取）
        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null && fsuEndpointResolver != null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(fsuCode);
            if (!endpoint.isSuccess()) {
                log.warn("FSU endpoint 解析失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, endpoint.getResultCode(), endpoint.getResultDesc());
                return GetDataResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), fsuCode);
            }
            effectiveServiceUrl = endpoint.getServiceUrl();
            log.debug("FSU endpoint 解析成功: fsuCode={}, url={}", fsuCode, effectiveServiceUrl);
        }

        try {
            // 1. 构造请求 XMLData（SignalID 列表）
            String xmlDataXml = buildRequestXmlData(signalIds);

            // 2. 构造 Info 字段
            String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";

            // 3. 构造 FsuServiceRequest
            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(fsuCode)
                    .serviceUrl(effectiveServiceUrl)
                    .pkType(BInterfacePkType.GET_DATA)
                    .infoXml(infoXml)
                    .xmlDataXml(xmlDataXml)
                    .build();

            // 4. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);

            if (!response.isSuccess()) {
                log.warn("GET_DATA FSUService 调用失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, response.getResultCode(), response.getResultDesc());
                return GetDataResult.fail(response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 查询失败", fsuCode);
            }

            // 5. 解析响应中的信号数据
            XmlDataModel responseXmlData = response.getXmlData();
            if (responseXmlData == null || responseXmlData.isEmpty()) {
                log.warn("GET_DATA 响应无数据: fsuCode={}", fsuCode);
                return GetDataResult.success(fsuCode, List.of());
            }

            List<GetDataResult.SignalValue> signals = parseSignals(responseXmlData);
            log.debug("GET_DATA 成功: fsuCode={}, count={}", fsuCode, signals.size());

            return GetDataResult.success(fsuCode, signals);

        } catch (Exception e) {
            log.error("GET_DATA 处理异常: fsuCode={}", fsuCode, e);
            return GetDataResult.fail("5001", "查询异常: " + e.getMessage(), fsuCode);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构造 GET_DATA 请求的 XMLData 内容（SignalID 列表）。
     */
    String buildRequestXmlData(List<String> signalIds) {
        StringBuilder sb = new StringBuilder();
        for (String id : signalIds) {
            if (id != null && !id.trim().isEmpty()) {
                sb.append("<SignalID>").append(escapeXml(id.trim())).append("</SignalID>\n");
            }
        }
        return sb.toString();
    }

    /**
     * 从响应 XmlDataModel 中解析 Signal 列表。
     */
    List<GetDataResult.SignalValue> parseSignals(XmlDataModel xmlData) {
        List<GetDataResult.SignalValue> result = new ArrayList<>();
        List<Map<String, String>> items = xmlData.getItems();
        if (items == null) return result;

        for (Map<String, String> item : items) {
            if (item == null) continue;
            String signalId = getFieldIgnoreCase(item, "SignalID");
            if (signalId == null) continue;

            String value = getFieldIgnoreCase(item, "Value");
            String quality = getFieldIgnoreCase(item, "Quality");
            String status = getFieldIgnoreCase(item, "Status");
            String collectTime = getFieldIgnoreCase(item, "CollectTime");

            result.add(new GetDataResult.SignalValue(
                    signalId, value, quality, status, collectTime));
        }
        return result;
    }

    private String getFieldIgnoreCase(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) {
                return e.getValue() != null ? e.getValue().trim() : null;
            }
        }
        return null;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
