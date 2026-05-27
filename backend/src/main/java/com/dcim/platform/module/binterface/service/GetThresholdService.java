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
 * GET_THRESHOLD 门限查询服务。
 *
 * <p>SC 主动向 FSU 查询告警门限参数。
 * 通过 FsuServiceClient 发送 SOAP 请求到 FSUService，解析响应返回门限值列表。</p>
 *
 * <p>与 SET_THRESHOLD 边界：</p>
 * <ul>
 *   <li>GET_THRESHOLD：SC→FSU，查询方向，只读</li>
 *   <li>SET_THRESHOLD：SC→FSU，设置方向，高风险操作</li>
 *   <li>本服务不调用 SetThresholdService，不修改 FSU 门限</li>
 * </ul>
 */
@Service
public class GetThresholdService {

    private static final Logger log = LoggerFactory.getLogger(GetThresholdService.class);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public GetThresholdService(FsuServiceClient fsuServiceClient, FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    /**
     * 执行 GET_THRESHOLD 查询。
     *
     * @param fsuCode       FSU 编码
     * @param fsuServiceUrl FSU 服务地址（可为 null）
     * @param signalIds     要查询的信号 ID 列表
     * @return 门限查询结果
     */
    public GetThresholdResult execute(String fsuCode, String fsuServiceUrl, List<String> signalIds) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return GetThresholdResult.fail("2001", "缺少 FSUCode");
        }
        if (signalIds == null || signalIds.isEmpty()) {
            return GetThresholdResult.fail("2003", "缺少 SignalID");
        }

        // 解析 FSU endpoint（未显式指定时从数据库获取）
        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null && fsuEndpointResolver != null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(fsuCode);
            if (!endpoint.isSuccess()) {
                log.warn("FSU endpoint 解析失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, endpoint.getResultCode(), endpoint.getResultDesc());
                return GetThresholdResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), fsuCode);
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
                    .pkType(BInterfacePkType.GET_THRESHOLD)
                    .pkTypeFormat("legacy-2016")
                    .infoXml(infoXml)
                    .xmlDataXml(xmlDataXml)
                    .build();

            // 4. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);

            if (!response.isSuccess()) {
                log.warn("GET_THRESHOLD FSUService 调用失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, response.getResultCode(), response.getResultDesc());
                return GetThresholdResult.fail(response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 查询失败", fsuCode);
            }

            // 5. 解析响应中的门限数据
            XmlDataModel responseXmlData = response.getXmlData();
            if (responseXmlData == null || responseXmlData.isEmpty()) {
                log.warn("GET_THRESHOLD 响应无数据: fsuCode={}", fsuCode);
                return GetThresholdResult.success(fsuCode, List.of());
            }

            List<GetThresholdResult.ThresholdValue> thresholds = parseThresholds(responseXmlData);
            log.debug("GET_THRESHOLD 成功: fsuCode={}, count={}", fsuCode, thresholds.size());

            return GetThresholdResult.success(fsuCode, thresholds);

        } catch (Exception e) {
            log.error("GET_THRESHOLD 处理异常: fsuCode={}", fsuCode, e);
            return GetThresholdResult.fail("5001", "查询异常: " + e.getMessage(), fsuCode);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构造 GET_THRESHOLD 请求的 XMLData 内容（SignalID 列表）。
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
     * 从响应 XmlDataModel 中解析门限值列表。
     *
     * <p>每个 Signal 节点包含：SignalID, AlarmUpper, AlarmLower,
     * AlarmUpperUrgent, AlarmLowerUrgent。</p>
     */
    List<GetThresholdResult.ThresholdValue> parseThresholds(XmlDataModel xmlData) {
        List<GetThresholdResult.ThresholdValue> result = new ArrayList<>();
        List<Map<String, String>> items = xmlData.getItems();
        if (items == null) return result;

        for (Map<String, String> item : items) {
            if (item == null) continue;
            String signalId = getFieldIgnoreCase(item, "SignalID");
            if (signalId == null) continue;

            String alarmUpper = getFieldIgnoreCase(item, "AlarmUpper");
            String alarmLower = getFieldIgnoreCase(item, "AlarmLower");
            String alarmUpperUrgent = getFieldIgnoreCase(item, "AlarmUpperUrgent");
            String alarmLowerUrgent = getFieldIgnoreCase(item, "AlarmLowerUrgent");

            result.add(new GetThresholdResult.ThresholdValue(
                    signalId, alarmUpper, alarmLower, alarmUpperUrgent, alarmLowerUrgent));
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
