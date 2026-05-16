package com.dcim.platform.module.binterface.service.sc;

import com.dcim.platform.module.binterface.command.CommandDispatcher;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCService 服务端接口
 *
 * 接收 FSU 主动上报的 SOAP/XML 报文（快数据通道）。
 * 遵循 B接口 2016 WSDL 规范：
 * - RPC style, soapenc:encoded
 * - invoke(xmlData: string) → invokeReturn: string
 *
 * 处理链路：
 * <pre>
 *   HTTP SOAP →
 *   SoapMessageHandler.parse() →
 *   CommandDispatcher.dispatch() →
 *   CommandHandler.handle() →
 *   CommandResult →
 *   SoapMessageHandler.buildResponse()
 * </pre>
 */
@RestController
@RequestMapping("/api/b-interface")
public class ScServiceController {

    private static final Logger log = LoggerFactory.getLogger(ScServiceController.class);

    private final SoapMessageHandler soapHandler;

    private final CommandDispatcher dispatcher;

    public ScServiceController(SoapMessageHandler soapHandler, CommandDispatcher dispatcher) {
        this.soapHandler = soapHandler;
        this.dispatcher = dispatcher;
    }

    @PostMapping(value = "/sc-service",
            consumes = {MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = MediaType.TEXT_XML_VALUE)
    public String handleScService(@RequestBody(required = false) String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return soapHandler.buildFault("soap:Client", "请求体为空", null);
        }

        try {
            BInterfaceMessage message = soapHandler.parse(requestBody);
            log.info("SCService 收到命令: pkType={}, request={}",
                    message.getPkType(), message.getRequest());

            if (message.getPkType() == null || message.getPkType() == BInterfacePkType.UNKNOWN) {
                if (message.getInfo() != null && message.getInfo().contains("Fault")) {
                    return soapHandler.buildFault("soap:Server", "接收到 SOAP Fault", message.getInfo());
                }
                return soapHandler.buildFault("soap:Client", "未知命令码", null);
            }

            // 分发到 CommandHandler
            CommandResult result = dispatcher.dispatch(message, requestBody);

            if (!result.isSuccess()) {
                log.warn("命令处理未成功: pkType={}, code={}, desc={}",
                        result.getPkType(), result.getResultCode(), result.getResultDesc());
            }

            // 构造 SOAP 响应
            String pkTypeStr = result.getPkType().name();
            String infoXml = result.toInfoXml();
            String xmlDataXml = result.toXmlDataXml();
            return soapHandler.buildResponse(pkTypeStr, infoXml, xmlDataXml);

        } catch (Exception e) {
            log.error("SCService 处理失败", e);
            return soapHandler.buildFault("soap:Server", "服务器内部错误: " + e.getMessage(), null);
        }
    }
}
