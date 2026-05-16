package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetThresholdResult;
import com.dcim.platform.module.binterface.service.GetThresholdService;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 GET_THRESHOLD 命令处理器。
 *
 * <p>处理 SC 向 FSU 查询门限参数（慢数据通道，只读）。
 * 职责：校验登录态、提取 FSUCode/SignalID、调用 GetThresholdService、构造响应。</p>
 *
 * <p>不直接访问 HTTP，不直接调用 FsuServiceClient，不做持久化，不修改 FSU 门限。</p>
 */
@Component
public class GetThresholdCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(GetThresholdCommandHandler.class);

    private static final Pattern FSU_CODE_PATTERN = Pattern.compile(
            "<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final GetThresholdService getThresholdService;

    public GetThresholdCommandHandler(LoginService loginService, GetThresholdService getThresholdService) {
        this.loginService = loginService;
        this.getThresholdService = getThresholdService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.GET_THRESHOLD;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            log.warn("GET_THRESHOLD 失败: context 为 null");
            return CommandResult.error(BInterfacePkType.GET_THRESHOLD, "5001", "上下文为空");
        }

        // 提取 Info XML
        String infoXml = context.getSoapMessage() != null
                ? context.getSoapMessage().getInfo() : null;
        if (infoXml == null || infoXml.trim().isEmpty()) {
            log.warn("GET_THRESHOLD 失败: Info 为空");
            return buildErrorResponse("2001", "缺少 Info");
        }

        // 提取 FSUCode
        String fsuCode = extractFsuCode(infoXml);
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            log.warn("GET_THRESHOLD 失败: 缺少 FSUCode");
            return buildErrorResponse("2001", "缺少 FSUCode");
        }
        fsuCode = fsuCode.trim();

        // 校验登录态
        if (!loginService.isLoggedIn(fsuCode)) {
            log.warn("GET_THRESHOLD 失败: FSU 未登录 fsuCode={}", fsuCode);
            return buildErrorResponse("1002", "FSU 未登录或已离线: " + fsuCode);
        }

        // 提取 SignalID 列表
        List<String> signalIds = extractSignalIds(context.getXmlData());
        if (signalIds.isEmpty()) {
            log.warn("GET_THRESHOLD 失败: 缺少 SignalID fsuCode={}", fsuCode);
            return buildErrorResponse("2003", "缺少 SignalID");
        }

        log.debug("GET_THRESHOLD 请求: fsuCode={}, signalIds={}", fsuCode, signalIds);

        // 调用 GetThresholdService
        GetThresholdResult result = getThresholdService.execute(fsuCode, null, signalIds);

        // 构造 CommandResult
        return buildCommandResult(result);
    }

    /**
     * 构造 GET_THRESHOLD 成功/失败 CommandResult。
     */
    private CommandResult buildCommandResult(GetThresholdResult result) {
        CommandResult cmdResult = new CommandResult();
        cmdResult.setPkType(BInterfacePkType.GET_THRESHOLD);
        cmdResult.setImplemented(true);

        if (result.isSuccess()) {
            cmdResult.setSuccess(true);
            cmdResult.setResultCode("0");
            cmdResult.setResultDesc("OK");

            // Info: <ResultCode>0</ResultCode><Count>N</Count>
            cmdResult.setResponseInfoXml(
                    "<ResultCode>0</ResultCode><Count>" + result.getCount() + "</Count>");

            // xmlData: Signal 列表（门限结构）
            if (!result.getThresholds().isEmpty()) {
                cmdResult.setResponseXmlData(buildResponseXmlData(result));
            }
        } else {
            cmdResult.setSuccess(false);
            cmdResult.setResultCode(result.getResultCode());
            cmdResult.setResultDesc(result.getResultDesc());
            cmdResult.setResponseInfoXml(
                    "<ResultCode>" + result.getResultCode() + "</ResultCode>");
            if (result.hasErrors()) {
                result.getErrors().forEach(cmdResult::addError);
            }
        }
        return cmdResult;
    }

    /**
     * 构建 GET_THRESHOLD 响应 xmlData（Signal 门限结构）。
     */
    private XmlDataModel buildResponseXmlData(GetThresholdResult result) {
        XmlDataModel xmlData = new XmlDataModel();
        xmlData.setRootName("Signal");
        for (GetThresholdResult.ThresholdValue tv : result.getThresholds()) {
            java.util.LinkedHashMap<String, String> item = new java.util.LinkedHashMap<>();
            item.put("SignalID", tv.getSignalId());
            if (tv.getAlarmUpper() != null) item.put("AlarmUpper", tv.getAlarmUpper());
            if (tv.getAlarmLower() != null) item.put("AlarmLower", tv.getAlarmLower());
            if (tv.getAlarmUpperUrgent() != null) item.put("AlarmUpperUrgent", tv.getAlarmUpperUrgent());
            if (tv.getAlarmLowerUrgent() != null) item.put("AlarmLowerUrgent", tv.getAlarmLowerUrgent());
            xmlData.addItem(item);
        }
        return xmlData;
    }

    /**
     * 从 xmlData 中提取 SignalID 列表。
     */
    private List<String> extractSignalIds(XmlDataModel xmlData) {
        if (xmlData == null) return List.of();

        List<String> fromItems = xmlData.getItemFieldValues("SignalID");
        if (!fromItems.isEmpty()) {
            return fromItems;
        }

        String singleSignalId = xmlData.getSignalId();
        if (singleSignalId != null) {
            return List.of(singleSignalId);
        }

        return List.of();
    }

    /**
     * 从 Info XML 提取 FSUCode。
     */
    public static String extractFsuCode(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher matcher = FSU_CODE_PATTERN.matcher(infoXml);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * 构造错误响应。
     */
    private CommandResult buildErrorResponse(String resultCode, String message) {
        CommandResult result = new CommandResult();
        result.setPkType(BInterfacePkType.GET_THRESHOLD);
        result.setSuccess(false);
        result.setResultCode(resultCode);
        result.setResultDesc(message);
        result.setImplemented(true);
        result.setResponseInfoXml("<ResultCode>" + resultCode + "</ResultCode>");
        result.addError(message);
        return result;
    }
}
