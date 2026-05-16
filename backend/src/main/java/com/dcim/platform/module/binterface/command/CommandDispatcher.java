package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * B接口命令分发器。
 *
 * 核心职责：
 * <ol>
 *   <li>接收 SoapMessageHandler 解析后的 BInterfaceMessage</li>
 *   <li>调用 XmlDataParser 将 xmlData 原始 XML 解析为 XmlDataModel</li>
 *   <li>构造 CommandContext 供 Handler 使用</li>
 *   <li>根据 PK_Type 将请求分发到对应的 CommandHandler</li>
 *   <li>统一捕获 Handler 异常并转换为结构化的 CommandResult</li>
 * </ol>
 *
 * 路由规则：
 * <ol>
 *   <li>精确匹配：按 PK_Type 查找对应 Handler</li>
 *   <li>兜底匹配：找不到时使用 UNKNOWN Handler</li>
 *   <li>异常兜底：Handler 抛异常时返回 error CommandResult</li>
 * </ol>
 *
 * 所有实现了 CommandHandler 接口的 Spring Bean 会被自动注入。
 *
 * @see CommandHandler
 * @see CommandContext
 * @see CommandResult
 */
@Component
public class CommandDispatcher {

    private static final Logger log = LoggerFactory.getLogger(CommandDispatcher.class);

    private final Map<BInterfacePkType, CommandHandler> handlerMap;

    private final XmlDataParser xmlDataParser;

    private final UnknownCommandHandler fallbackHandler;

    public CommandDispatcher(List<CommandHandler> handlers,
                             UnknownCommandHandler fallbackHandler,
                             XmlDataParser xmlDataParser) {
        this.fallbackHandler = fallbackHandler;
        this.xmlDataParser = xmlDataParser;
        this.handlerMap = new HashMap<>();
        for (CommandHandler handler : handlers) {
            BInterfacePkType pkType = handler.getSupportedPkType();
            if (pkType != BInterfacePkType.UNKNOWN) {
                handlerMap.put(pkType, handler);
            }
        }
        log.info("CommandDispatcher 已注册 {} 个处理器: {}", handlerMap.size(), handlerMap.keySet());
    }

    /**
     * 分发 B接口命令到对应的处理器。
     *
     * @param request  SoapMessageHandler 解析后的请求消息
     * @param rawSoap  原始 SOAP XML 字符串
     * @return 处理结果（不会返回 null）
     */
    public CommandResult dispatch(BInterfaceMessage request, String rawSoap) {
        if (request == null) {
            log.warn("收到 null request，使用兜底处理器");
            return fallback(request.getPkType(), request, rawSoap, null);
        }

        BInterfacePkType pkType = request.getPkType();
        if (pkType == null) {
            log.warn("收到 null PK_Type，使用兜底处理器");
            return fallback(null, request, rawSoap, null);
        }

        try {
            // 解析 xmlData
            String rawXmlData = request.getXmlData();
            XmlDataModel xmlData = xmlDataParser.parse(rawXmlData);

            if (!xmlData.isValid()) {
                log.warn("PK_Type={} 的 xmlData 解析失败: {}", pkType, xmlData.getFirstError());
                return CommandResult.error(pkType, "2001",
                        "xmlData 解析失败: " + xmlData.getFirstError());
            }

            // 构造上下文
            CommandContext context = new CommandContext(pkType, request, xmlData, rawSoap, rawXmlData);

            // 查找 Handler
            CommandHandler handler = handlerMap.get(pkType);
            if (handler != null) {
                log.debug("分发命令: {} → {}", pkType, handler.getClass().getSimpleName());
                CommandResult result = handler.handle(context);
                if (result == null) {
                    log.warn("Handler {} 返回了 null", handler.getClass().getSimpleName());
                    return CommandResult.error(pkType, "5001",
                            "Handler 返回空结果: " + handler.getClass().getSimpleName());
                }
                return result;
            }

            log.warn("未找到 PK_Type={} 的处理器，使用 UNKNOWN 兜底", pkType);
            return fallbackHandler.handle(context);

        } catch (Exception e) {
            log.error("命令分发异常: pkType={}, error={}", pkType, e.getMessage(), e);
            return CommandResult.error(pkType, "5001",
                    "服务器内部错误: " + e.getMessage());
        }
    }

    /**
     * 简化分发（不依赖原始 SOAP 字符串）。
     */
    public CommandResult dispatch(BInterfaceMessage request) {
        return dispatch(request, null);
    }

    /**
     * 检查指定 PK_Type 是否有注册的处理器。
     */
    public boolean hasHandler(BInterfacePkType pkType) {
        return pkType != null && handlerMap.containsKey(pkType);
    }

    /**
     * 获取已注册的处理器数量。
     */
    public int getHandlerCount() {
        return handlerMap.size();
    }

    /**
     * 获取已注册的 PK_Type 集合。
     */
    public Map<BInterfacePkType, CommandHandler> getHandlerMap() {
        return handlerMap;
    }

    // ==================== 内部方法 ====================

    private CommandResult fallback(BInterfacePkType pkType, BInterfaceMessage request,
                                   String rawSoap, String rawXmlData) {
        CommandContext ctx = new CommandContext(pkType, request, new XmlDataModel(), rawSoap, rawXmlData);
        return fallbackHandler.handle(ctx);
    }
}
