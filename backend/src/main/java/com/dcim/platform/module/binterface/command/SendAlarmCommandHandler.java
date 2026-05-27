package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.SendAlarmResult;
import com.dcim.platform.module.binterface.service.SendAlarmService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 SEND_ALARM 命令处理器。
 *
 * 处理 FSU 主动上报的告警信息。
 *
 * 处理流程：
 * <ol>
 *   <li>从 CommandContext 中提取 FSUCode（来自 Info 层）</li>
 *   <li>校验 FSU 登录态</li>
 *   <li>从 Info 中提取 AlarmTime（可选）</li>
 *   <li>委托 {@link SendAlarmService} 处理 Alarm 项入库</li>
 *   <li>构造 SEND_ALARM 响应 Info（ResultCode / AlarmID）</li>
 * </ol>
 */
@Component
public class SendAlarmCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SendAlarmCommandHandler.class);

    private final LoginService loginService;
    private final SendAlarmService sendAlarmService;

    public SendAlarmCommandHandler(LoginService loginService, SendAlarmService sendAlarmService) {
        this.loginService = loginService;
        this.sendAlarmService = sendAlarmService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.SEND_ALARM;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            return CommandResult.error(BInterfacePkType.SEND_ALARM, "5001", "命令上下文为空");
        }

        try {
            // 1. 提取 FSUCode（来自 Info 层）
            String infoXml = context.getSoapMessage() != null
                    ? context.getSoapMessage().getInfo() : null;
            String fsuCode = extractFsuCode(infoXml);

            if (fsuCode == null) {
                log.warn("SEND_ALARM 失败: Info 中未找到 FSUCode");
                return CommandResult.error(BInterfacePkType.SEND_ALARM, "2001", "缺少 FSUCode");
            }

            // 2. 校验 FSU 登录态
            if (!loginService.isLoggedIn(fsuCode)) {
                log.warn("SEND_ALARM 失败: FSU 未登录 fsuCode={}", fsuCode);
                return CommandResult.error(BInterfacePkType.SEND_ALARM, "1002",
                        "FSU 未登录: " + fsuCode);
            }

            // 3. 提取 AlarmTime（可选）
            String alarmTimeStr = extractAlarmTime(infoXml);

            // 4. 获取 xmlData
            XmlDataModel xmlData = context.getXmlData();

            // 5. 委托 SendAlarmService 处理
            SendAlarmResult sendResult = sendAlarmService.processAlarms(fsuCode, alarmTimeStr, xmlData);

            // 6. 构造响应
            return buildResponse(sendResult);

        } catch (Exception e) {
            log.error("SEND_ALARM Handler 异常", e);
            return CommandResult.error(BInterfacePkType.SEND_ALARM, "5001",
                    "SEND_ALARM 处理异常: " + e.getMessage());
        }
    }

    // ==================== 响应构造 ====================

    private CommandResult buildResponse(SendAlarmResult sendResult) {
        if (!sendResult.isSuccess()) {
            return CommandResult.error(BInterfacePkType.SEND_ALARM,
                    sendResult.getResultCode(), sendResult.getResultDesc());
        }

        String alarmIdStr = sendResult.getAlarmIds().isEmpty()
                ? "0" : String.valueOf(sendResult.getAlarmIds().get(sendResult.getAlarmIds().size() - 1));

        String infoXml = "<Result>1</Result>"
                + "<AlarmID>" + alarmIdStr + "</AlarmID>";

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setResultCode("1");
        result.setResultDesc("告警上报成功");
        result.setPkType(BInterfacePkType.SEND_ALARM);
        result.setImplemented(true);
        result.setResponseInfoXml(infoXml);
        result.setResponseXmlData(null); // SEND_ALARM 响应 xmlData 为空
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
     * 从 Info XML 中提取 AlarmTime。
     * 大小写不敏感匹配 AlarmTime 变体。
     * AlarmTime 为可选字段，不存在时返回 null。
     */
    public static String extractAlarmTime(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Pattern pattern = Pattern.compile("<AlarmTime[^>]*>(.*?)</AlarmTime>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(infoXml);
        if (matcher.find()) {
            String time = matcher.group(1).trim();
            return time.isEmpty() ? null : time;
        }
        return null;
    }
}
