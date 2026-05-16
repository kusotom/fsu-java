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

/**
 * GET_SPCONFIGOPTION 配置模板查询服务 (2024 标准, Code=401)。
 *
 * <p>BIF-P4-015: SC 查询 FSU 监控点配置模板选型。只读，不修改配置。</p>
 */
@Service
public class GetSpConfigOptionService {

    private static final Logger log = LoggerFactory.getLogger(GetSpConfigOptionService.class);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public GetSpConfigOptionService(FsuServiceClient fsuServiceClient,
                                     FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    public GetSpConfigOptionResult execute(String fsuCode, String fsuServiceUrl) {
        if (fsuCode == null || fsuCode.trim().isEmpty())
            return GetSpConfigOptionResult.fail("2001", "缺少 SUID");
        String suid = fsuCode.trim();

        String url = fsuServiceUrl;
        if (url == null && fsuEndpointResolver != null) {
            FsuEndpointResult ep = fsuEndpointResolver.resolve(suid);
            if (!ep.isSuccess()) return GetSpConfigOptionResult.fail(ep.getResultCode(), ep.getResultDesc(), suid);
            url = ep.getServiceUrl();
        }

        try {
            String infoXml = "<SUID>" + suid + "</SUID>";
            FsuServiceRequest req = FsuServiceRequest.builder()
                    .fsuCode(suid).serviceUrl(url)
                    .pkType(BInterfacePkType.GET_SPCONFIGOPTION).infoXml(infoXml).build();

            FsuServiceResponse resp = fsuServiceClient.call(req);
            if (!resp.isSuccess())
                return GetSpConfigOptionResult.fail(resp.getResultCode(),
                        resp.getResultDesc() != null ? resp.getResultDesc() : "FSU 查询失败", suid);

            log.debug("GET_SPCONFIGOPTION 成功: suid={}", suid);
            return GetSpConfigOptionResult.success(suid);
        } catch (Exception e) {
            log.error("GET_SPCONFIGOPTION 异常: suid={}", suid, e);
            return GetSpConfigOptionResult.fail("5001", "查询异常", suid);
        }
    }
}
