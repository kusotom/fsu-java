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
 * GET_SUFTP FTP 参数查询服务 (2024 标准)。
 *
 * <p>BIF-P4-013: SC 主动向 FSU 查询 FTP 配置参数 (Code=801)。
 * 只读查询，不修改 FSU 配置。Password 脱敏处理。</p>
 */
@Service
public class GetSuFtpService {

    private static final Logger log = LoggerFactory.getLogger(GetSuFtpService.class);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public GetSuFtpService(FsuServiceClient fsuServiceClient,
                           FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    public GetSuFtpResult execute(String fsuCode, String fsuServiceUrl) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return GetSuFtpResult.fail("2001", "缺少 SUID");
        }
        String suid = fsuCode.trim();

        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null && fsuEndpointResolver != null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(suid);
            if (!endpoint.isSuccess()) {
                return GetSuFtpResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), suid);
            }
            effectiveServiceUrl = endpoint.getServiceUrl();
        }

        try {
            String infoXml = "<SUID>" + suid + "</SUID>";

            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(suid)
                    .serviceUrl(effectiveServiceUrl)
                    .pkType(BInterfacePkType.GET_SUFTP)
                    .infoXml(infoXml)
                    .build();

            FsuServiceResponse response = fsuServiceClient.call(request);

            if (!response.isSuccess()) {
                log.warn("GET_SUFTP 调用失败: suid={}, code={}", suid, response.getResultCode());
                return GetSuFtpResult.fail(response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 查询失败", suid);
            }

            XmlDataModel xmlData = response.getXmlData();
            if (xmlData == null || xmlData.isEmpty()) {
                return GetSuFtpResult.fail("2003", "无有效响应数据", suid);
            }

            List<Map<String, String>> items = xmlData.getItems();
            Map<String, String> data = (items != null && !items.isEmpty())
                    ? items.get(0) : xmlData.getFields();

            String userName = getField(data, "UserName");
            String password = getField(data, "Password");
            Integer ftpPort = parseInt(getField(data, "FTPPort"));

            log.debug("GET_SUFTP 成功: suid={}, userName={}, ftpPort={}",
                    suid, mask(userName), ftpPort);

            return GetSuFtpResult.success(suid, userName, password, ftpPort);

        } catch (Exception e) {
            log.error("GET_SUFTP 处理异常: suid={}", suid, e);
            return GetSuFtpResult.fail("5001", "查询异常: " + e.getMessage(), suid);
        }
    }

    private String getField(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue() != null ? e.getValue().trim() : null;
        }
        return null;
    }

    private Integer parseInt(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return Integer.parseInt(value.trim()); } catch (NumberFormatException e) { return null; }
    }

    private String mask(String s) {
        if (s == null || s.length() <= 2) return "****";
        return s.substring(0, 1) + "****";
    }
}
