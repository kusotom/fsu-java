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

import java.util.List;
import java.util.Map;

/**
 * SET_THRESHOLD 门限设置服务。
 *
 * <p>SC 主动向 FSU 设置告警门限参数。
 * 通过 FsuServiceClient 发送 SOAP 请求到 FSUService，解析响应返回设置结果。</p>
 *
 * <p>与 GET_THRESHOLD 边界：</p>
 * <ul>
 *   <li>GET_THRESHOLD：SC→FSU，查询方向，只读</li>
 *   <li>SET_THRESHOLD：SC→FSU，设置方向，高风险操作</li>
 *   <li>本服务只做设置，不处理查询逻辑</li>
 * </ul>
 */
@Service
public class SetThresholdService {

    private static final Logger log = LoggerFactory.getLogger(SetThresholdService.class);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public SetThresholdService(FsuServiceClient fsuServiceClient, FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    /**
     * 执行 SET_THRESHOLD 操作。
     *
     * @param fsuCode        FSU 编码
     * @param fsuServiceUrl  FSU 服务地址（可为 null）
     * @param signalId       信号 ID
     * @param alarmUpper     告警上限
     * @param alarmLower     告警下限
     * @param alarmUpperUrgent 严重告警上限
     * @param alarmLowerUrgent 严重告警下限
     * @return 设置结果
     */
    public SetThresholdResult execute(String fsuCode, String fsuServiceUrl,
                                      String signalId, String alarmUpper, String alarmLower,
                                      String alarmUpperUrgent, String alarmLowerUrgent) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return SetThresholdResult.fail("2001", "缺少 FSUCode");
        }
        if (signalId == null || signalId.trim().isEmpty()) {
            return SetThresholdResult.fail("2003", "缺少 SignalID");
        }

        // 解析 FSU endpoint（未显式指定时从数据库获取）
        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null && fsuEndpointResolver != null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(fsuCode);
            if (!endpoint.isSuccess()) {
                log.warn("FSU endpoint 解析失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, endpoint.getResultCode(), endpoint.getResultDesc());
                return SetThresholdResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), fsuCode);
            }
            effectiveServiceUrl = endpoint.getServiceUrl();
            log.debug("FSU endpoint 解析成功: fsuCode={}, url={}", fsuCode, effectiveServiceUrl);
        }

        try {
            // 1. 构造请求 XMLData
            String xmlDataXml = buildRequestXmlData(signalId, alarmUpper, alarmLower,
                    alarmUpperUrgent, alarmLowerUrgent);

            // 2. 构造 Info 字段
            String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";

            // 3. 构造 FsuServiceRequest
            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(fsuCode)
                    .serviceUrl(effectiveServiceUrl)
                    .pkType(BInterfacePkType.SET_THRESHOLD)
                    .infoXml(infoXml)
                    .xmlDataXml(xmlDataXml)
                    .build();

            // 4. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);

            if (!response.isSuccess()) {
                log.warn("SET_THRESHOLD FSUService 调用失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, response.getResultCode(), response.getResultDesc());
                return SetThresholdResult.fail(response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 设置失败", fsuCode);
            }

            // 5. 解析响应中的 Count
            int count = parseCount(response.getInfoXml());
            log.debug("SET_THRESHOLD 成功: fsuCode={}, signalId={}, count={}", fsuCode, signalId, count);

            return SetThresholdResult.success(fsuCode, count);

        } catch (Exception e) {
            log.error("SET_THRESHOLD 处理异常: fsuCode={}, signalId={}", fsuCode, signalId, e);
            return SetThresholdResult.fail("5001", "设置异常: " + e.getMessage(), fsuCode);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构造 SET_THRESHOLD 请求的 XMLData 内容（Signal 门限结构）。
     */
    String buildRequestXmlData(String signalId, String alarmUpper, String alarmLower,
                                String alarmUpperUrgent, String alarmLowerUrgent) {
        StringBuilder sb = new StringBuilder();
        sb.append("<Signal>\n");
        sb.append("  <SignalID>").append(escapeXml(signalId)).append("</SignalID>\n");
        if (alarmUpper != null && !alarmUpper.trim().isEmpty()) {
            sb.append("  <AlarmUpper>").append(escapeXml(alarmUpper.trim())).append("</AlarmUpper>\n");
        }
        if (alarmLower != null && !alarmLower.trim().isEmpty()) {
            sb.append("  <AlarmLower>").append(escapeXml(alarmLower.trim())).append("</AlarmLower>\n");
        }
        if (alarmUpperUrgent != null && !alarmUpperUrgent.trim().isEmpty()) {
            sb.append("  <AlarmUpperUrgent>").append(escapeXml(alarmUpperUrgent.trim())).append("</AlarmUpperUrgent>\n");
        }
        if (alarmLowerUrgent != null && !alarmLowerUrgent.trim().isEmpty()) {
            sb.append("  <AlarmLowerUrgent>").append(escapeXml(alarmLowerUrgent.trim())).append("</AlarmLowerUrgent>\n");
        }
        sb.append("</Signal>");
        return sb.toString();
    }

    /**
     * 从 Info XML 中解析 Count 字段。
     */
    public int parseCount(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return 0;
        String tag = "<Count>";
        String endTag = "</Count>";
        int start = infoXml.indexOf(tag);
        if (start < 0) {
            // 大小写不敏感尝试
            String lower = infoXml.toLowerCase();
            start = lower.indexOf("<count>");
            if (start >= 0) {
                endTag = "</count>";
            }
        }
        if (start < 0) return 0;
        start += tag.length();
        int end = infoXml.indexOf(endTag, start);
        if (end < 0) return 0;
        try {
            return Integer.parseInt(infoXml.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
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
