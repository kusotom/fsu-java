package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResolver;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResult;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TIME_CHECK 时间同步校验服务。
 *
 * <p>SC 主动向 FSU 发起时间同步校验。
 * 通过 FsuServiceClient 发送 SOAP 请求到 FSUService，解析响应返回 FSU 时间。</p>
 *
 * <p>边界：</p>
 * <ul>
 *   <li>TIME_CHECK：SC→FSU，只读查询，不修改 FSU 时间</li>
 *   <li>本服务不调用 SetThresholdService，不修改任何 FSU 参数</li>
 *   <li>本服务不修改系统时间</li>
 * </ul>
 */
@Service
public class TimeCheckService {

    private static final Logger log = LoggerFactory.getLogger(TimeCheckService.class);

    private static final Pattern FSU_TIME_PATTERN = Pattern.compile(
            "<FSUTime[^>]*>(.*?)</FSUTime>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public TimeCheckService(FsuServiceClient fsuServiceClient, FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    /**
     * 执行 TIME_CHECK 时间同步校验。
     *
     * @param fsuCode       FSU 编码
     * @param fsuServiceUrl FSU 服务地址（可为 null，由 FsuServiceClient 决定默认值）
     * @param standardTime  SC 端标准时间（ISO8601 格式字符串）
     * @return 时间同步结果
     */
    public TimeCheckResult execute(String fsuCode, String fsuServiceUrl, String standardTime) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return TimeCheckResult.fail("2001", "缺少 FSUCode");
        }
        if (standardTime == null || standardTime.trim().isEmpty()) {
            return TimeCheckResult.fail("2003", "缺少 StandardTime");
        }

        // 解析 FSU endpoint（未显式指定时从数据库获取）
        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null && fsuEndpointResolver != null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(fsuCode);
            if (!endpoint.isSuccess()) {
                log.warn("FSU endpoint 解析失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, endpoint.getResultCode(), endpoint.getResultDesc());
                return TimeCheckResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), fsuCode);
            }
            effectiveServiceUrl = endpoint.getServiceUrl();
            log.debug("FSU endpoint 解析成功: fsuCode={}, url={}", fsuCode, effectiveServiceUrl);
        }

        try {
            // 1. 构造 Info 字段
            String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>"
                    + "<StandardTime>" + escapeXml(standardTime.trim()) + "</StandardTime>";

            // 2. 构造 FsuServiceRequest（TIME_CHECK 无 xmlData）
            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(fsuCode)
                    .serviceUrl(effectiveServiceUrl)
                    .pkType(BInterfacePkType.TIME_CHECK)
                    .pkTypeFormat("legacy-2016")
                    .infoXml(infoXml)
                    .build();

            // 3. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);

            if (!response.isSuccess()) {
                log.warn("TIME_CHECK FSUService 调用失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, response.getResultCode(), response.getResultDesc());
                return TimeCheckResult.fail(response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 调用失败", fsuCode);
            }

            // 4. 从响应 Info 中解析 FSUTime
            String fsuTime = extractFsuTime(response.getInfoXml());
            if (fsuTime == null) {
                log.warn("TIME_CHECK 响应中未找到 FSUTime: fsuCode={}", fsuCode);
                return TimeCheckResult.fail("2003", "响应缺少 FSUTime", fsuCode);
            }

            log.debug("TIME_CHECK 成功: fsuCode={}, standardTime={}, fsuTime={}",
                    fsuCode, standardTime, fsuTime);

            return TimeCheckResult.success(fsuCode, fsuTime, standardTime);

        } catch (Exception e) {
            log.error("TIME_CHECK 处理异常: fsuCode={}", fsuCode, e);
            return TimeCheckResult.fail("5001", "时间同步异常: " + e.getMessage(), fsuCode);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 从 Info XML 中提取 FSUTime 字段。
     */
    public String extractFsuTime(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher matcher = FSU_TIME_PATTERN.matcher(infoXml);
        if (matcher.find()) {
            String time = matcher.group(1).trim();
            return time.isEmpty() ? null : time;
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
