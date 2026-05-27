package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.SendDataResult;
import com.dcim.platform.module.binterface.service.SendDataService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 SEND_DATA 命令处理器。
 *
 * 处理 FSU 主动上报的实时采集数据。
 *
 * 处理流程：
 * <ol>
 *   <li>从 CommandContext 中提取 FSUCode（来自 Info 层）</li>
 *   <li>校验 FSU 登录态</li>
 *   <li>从 Info 中提取 CollectTime（可选）</li>
 *   <li>委托 {@link SendDataService} 处理 Signal 项入库</li>
 *   <li>构造 SEND_DATA 响应 Info（ResultCode / Count）</li>
 * </ol>
 */
@Component
public class SendDataCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SendDataCommandHandler.class);

    private final LoginService loginService;
    private final SendDataService sendDataService;

    public SendDataCommandHandler(LoginService loginService, SendDataService sendDataService) {
        this.loginService = loginService;
        this.sendDataService = sendDataService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.SEND_DATA;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            return CommandResult.error(BInterfacePkType.SEND_DATA, "5001", "命令上下文为空");
        }

        try {
            // 1. 提取 FSUCode（来自 Info 层）
            String infoXml = context.getSoapMessage() != null
                    ? context.getSoapMessage().getInfo() : null;
            String fsuCode = extractFsuCode(infoXml);

            if (fsuCode == null) {
                log.warn("SEND_DATA 失败: Info 中未找到 FSUCode");
                return CommandResult.error(BInterfacePkType.SEND_DATA, "2001", "缺少 FSUCode");
            }

            // 2. 校验 FSU 登录态
            if (!loginService.isLoggedIn(fsuCode)) {
                log.warn("SEND_DATA 失败: FSU 未登录 fsuCode={}", fsuCode);
                return CommandResult.error(BInterfacePkType.SEND_DATA, "1002",
                        "FSU 未登录: " + fsuCode);
            }

            // 3. 提取 CollectTime（可选）
            String collectTimeStr = extractCollectTime(infoXml);

            // 4. 获取 xmlData
            XmlDataModel xmlData = context.getXmlData();

            // 5. 委托 SendDataService 处理
            SendDataResult sendResult = sendDataService.processData(fsuCode, collectTimeStr, xmlData);

            // 6. 构造响应
            return buildResponse(sendResult);

        } catch (Exception e) {
            log.error("SEND_DATA Handler 异常", e);
            return CommandResult.error(BInterfacePkType.SEND_DATA, "5001",
                    "SEND_DATA 处理异常: " + e.getMessage());
        }
    }

    // ==================== 响应构造 ====================

    private CommandResult buildResponse(SendDataResult sendResult) {
        if (!sendResult.isSuccess()) {
            return CommandResult.error(BInterfacePkType.SEND_DATA,
                    sendResult.getResultCode(), sendResult.getResultDesc());
        }

        String infoXml = "<Result>1</Result>"
                + "<Count>" + sendResult.getAcceptedCount() + "</Count>";

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setResultCode("1");
        result.setResultDesc("数据上报成功");
        result.setPkType(BInterfacePkType.SEND_DATA);
        result.setImplemented(true);
        result.setResponseInfoXml(infoXml);
        result.setResponseXmlData(null); // SEND_DATA 响应 xmlData 为空
        return result;
    }

    // ==================== 字段提取 ====================

    /**
     * 从 Info XML 中提取 FSUCode。
     * 大小写不敏感匹配 FSUCode / FsuCode 变体。
     */
    public static String extractFsuCode(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Pattern pattern = Pattern.compile("<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(infoXml);
        if (matcher.find()) {
            String code = matcher.group(1).trim();
            return code.isEmpty() ? null : code;
        }
        return null;
    }

    /**
     * 从 Info XML 中提取 CollectTime。
     * 大小写不敏感匹配 CollectTime 变体。
     * CollectTime 为可选字段，不存在时返回 null。
     */
    public static String extractCollectTime(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Pattern pattern = Pattern.compile("<CollectTime[^>]*>(.*?)</CollectTime>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(infoXml);
        if (matcher.find()) {
            String time = matcher.group(1).trim();
            return time.isEmpty() ? null : time;
        }
        return null;
    }
}
