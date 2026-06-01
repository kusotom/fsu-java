package com.dcim.platform.module.binterface.service.sc;

import com.dcim.platform.module.binterface.command.CommandDispatcher;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SCService SOAP 处理核心。
 *
 * 从 ScServiceController 提取，供多个入口复用。
 * 两个入口（/api/b-interface/sc-service 和 /services/SCService）共享同一套处理逻辑。
 *
 * 处理链路：
 * <pre>
 *   raw SOAP →
 *   SoapMessageHandler.parse() →
 *   BInterfaceMessageLogService.saveInbound() (best-effort) →
 *   CommandDispatcher.dispatch() →
 *   SoapMessageHandler.buildResponse() / buildFault()
 * </pre>
 */
@Service
public class ScServiceProcessor {

    private static final Logger log = LoggerFactory.getLogger(ScServiceProcessor.class);

    private final SoapMessageHandler soapHandler;
    private final CommandDispatcher dispatcher;
    private final BInterfaceMessageLogService messageLogService;

    public ScServiceProcessor(SoapMessageHandler soapHandler,
                               CommandDispatcher dispatcher,
                               BInterfaceMessageLogService messageLogService) {
        this.soapHandler = soapHandler;
        this.dispatcher = dispatcher;
        this.messageLogService = messageLogService;
    }

    /**
     * 处理一条 SCService 入站请求。
     *
     * @param requestBody 完整原始 SOAP/XML 请求体
     * @return SOAP XML 响应字符串（不返回 JSON）
     */
    public String process(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return soapHandler.buildFault("soap:Client", "请求体为空", null);
        }

        try {
            BInterfaceMessage message = soapHandler.parse(requestBody);
            log.info("SCService 收到命令: pkType={}", message.getPkType());

            // 保存原始报文（best-effort，失败不影响 ACK）
            logInboundMessage(message, requestBody);

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
            // 解析失败时也尝试保存原始报文（best-effort）
            messageLogService.saveInbound("UNKNOWN", null, requestBody);
            return soapHandler.buildFault("soap:Server", "服务器内部错误: " + e.getMessage(), null);
        }
    }

    // ==================== 内部方法 ====================

    private void logInboundMessage(BInterfaceMessage message, String requestBody) {
        try {
            String command = message.getPkType() != null ? message.getPkType().name() : "UNKNOWN";
            String fsuCode = extractFsuCode(message);
            messageLogService.saveInbound(command, fsuCode, requestBody);
        } catch (Exception e) {
            log.warn("入站报文日志保存失败: {}", e.getMessage());
        }
    }

    private String extractFsuCode(BInterfaceMessage message) {
        try {
            if (message.getInfo() != null) {
                String info = message.getInfo();
                int start = info.indexOf("<FsuCode>");
                if (start >= 0) {
                    start += "<FsuCode>".length();
                    int end = info.indexOf("</FsuCode>", start);
                    if (end >= 0) {
                        return info.substring(start, end).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("提取 FsuCode 失败: {}", e.getMessage());
        }
        return null;
    }
}
