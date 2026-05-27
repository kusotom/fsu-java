package com.dcim.platform.module.binterface.config;

import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * BIF2016-PROXY-001: FSU HTTP 客户端配置。
 *
 * <p>当 {@code b-interface.fsu-client.real-call-enabled=true} 时创建
 * {@link RealHttpFsuServiceClient} bean，支持可选 HTTP 代理。</p>
 */
@Configuration
public class FsuClientConfig {

    @Bean
    @ConditionalOnProperty(name = "b-interface.fsu-client.real-call-enabled", havingValue = "true")
    public FsuServiceClient realHttpFsuServiceClient(
            SoapMessageHandler soapMessageHandler,
            XmlDataParser xmlDataParser,
            FsuServiceRpcAdapter rpcAdapter,
            FsuClientProxyProperties proxyProperties) {
        if (proxyProperties.isValid()) {
            return new RealHttpFsuServiceClient(soapMessageHandler, xmlDataParser, rpcAdapter,
                    10000, 15000,
                    proxyProperties.getHost(), proxyProperties.getPort());
        }
        return new RealHttpFsuServiceClient(soapMessageHandler, xmlDataParser, rpcAdapter,
                10000, 15000);
    }
}
