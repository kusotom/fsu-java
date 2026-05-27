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
 * GET_FTP FTP 配置查询服务。
 *
 * <p>SC 主动向 FSU 查询 FTP 配置参数。
 * 通过 FsuServiceClient 发送 SOAP 请求到 FSUService，解析响应返回 FTP 配置。</p>
 *
 * <p>与 SET_FTP 边界：</p>
 * <ul>
 *   <li>GET_FTP：SC→FSU，查询方向，只读</li>
 *   <li>SET_FTP：SC→FSU，配置方向，高风险操作</li>
 *   <li>本服务不调用 SetFtpService，不修改 FSU 的 FTP 配置</li>
 * </ul>
 */
@Service
public class GetFtpService {

    private static final Logger log = LoggerFactory.getLogger(GetFtpService.class);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public GetFtpService(FsuServiceClient fsuServiceClient, FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    /**
     * 执行 GET_FTP 查询。
     *
     * @param fsuCode       FSU 编码
     * @param fsuServiceUrl FSU 服务地址（可为 null）
     * @param fileType      文件类型（可选，如 IMAGE）
     * @return FTP 配置查询结果
     */
    public GetFtpResult execute(String fsuCode, String fsuServiceUrl, String fileType) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return GetFtpResult.fail("2001", "缺少 FSUCode");
        }

        // 解析 FSU endpoint（未显式指定时从数据库获取）
        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null && fsuEndpointResolver != null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(fsuCode);
            if (!endpoint.isSuccess()) {
                log.warn("FSU endpoint 解析失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, endpoint.getResultCode(), endpoint.getResultDesc());
                return GetFtpResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), fsuCode);
            }
            effectiveServiceUrl = endpoint.getServiceUrl();
            log.debug("FSU endpoint 解析成功: fsuCode={}, url={}", fsuCode, effectiveServiceUrl);
        }

        try {
            // 1. 构造 Info 字段
            StringBuilder infoXml = new StringBuilder();
            infoXml.append("<FSUCode>").append(fsuCode).append("</FSUCode>");
            if (fileType != null && !fileType.trim().isEmpty()) {
                infoXml.append("<FileType>").append(fileType.trim()).append("</FileType>");
            }

            // 2. 构造 FsuServiceRequest（GET_FTP 无 xmlData）
            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(fsuCode)
                    .serviceUrl(effectiveServiceUrl)
                    .pkType(BInterfacePkType.GET_FTP)
                    .pkTypeFormat("legacy-2016")
                    .infoXml(infoXml.toString())
                    .build();

            // 3. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);

            if (!response.isSuccess()) {
                log.warn("GET_FTP FSUService 调用失败: fsuCode={}, resultCode={}, desc={}",
                        fsuCode, response.getResultCode(), response.getResultDesc());
                return GetFtpResult.fail(response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 查询失败", fsuCode);
            }

            // 4. 解析响应 xmlData
            XmlDataModel xmlData = response.getXmlData();
            if (xmlData == null || xmlData.isEmpty()) {
                log.warn("GET_FTP 响应无数据: fsuCode={}", fsuCode);
                return GetFtpResult.fail("2003", "无有效响应数据", fsuCode);
            }

            // 5. 提取 FTPConfig 字段（包裹模式：单个 item 含所有子字段）
            List<Map<String, String>> items = xmlData.getItems();
            if (items == null || items.isEmpty()) {
                log.warn("GET_FTP 响应无 FTPConfig: fsuCode={}", fsuCode);
                return GetFtpResult.fail("2003", "响应缺少 FTPConfig", fsuCode);
            }

            Map<String, String> config = items.get(0);
            String host = getField(config, "Host");
            int port = parseInt(getField(config, "Port"), 21);
            String username = getField(config, "Username");
            boolean passiveMode = "true".equalsIgnoreCase(getField(config, "PassiveMode"));
            String basePath = getField(config, "BasePath");

            log.debug("GET_FTP 成功: fsuCode={}, host={}, port={}, username={}",
                    fsuCode, host, port,
                    username != null ? username.substring(0, 1) + "****" : null);

            return GetFtpResult.success(fsuCode, host, port, username, passiveMode, basePath);

        } catch (Exception e) {
            log.error("GET_FTP 处理异常: fsuCode={}", fsuCode, e);
            return GetFtpResult.fail("5001", "查询异常: " + e.getMessage(), fsuCode);
        }
    }

    private String getField(Map<String, String> map, String key) {
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

    private int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
