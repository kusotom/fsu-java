package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 SET_THRESHOLD 命令处理器（安全闭环）。
 *
 * <p>处理 SC 向 FSU 设置告警门限参数（慢数据通道，高风险操作）。
 * 职责：校验登录态 → 提取 FSUCode/SignalID/门限字段 → 安全门禁检查 → 审计记录 → 调用 Service → 构造响应。</p>
 *
 * <p>安全控制：</p>
 * <ul>
 *   <li>默认 {@code b-interface.set-threshold.enabled=false}，未启用时返回 3001</li>
 *   <li>默认 {@code b-interface.set-threshold.require-confirmation=true}，未确认时返回 3002</li>
 *   <li>所有操作记录审计日志</li>
 *   <li>不直接访问 HTTP，不直接调用 FsuServiceClient</li>
 * </ul>
 */
@Component
public class SetThresholdCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SetThresholdCommandHandler.class);

    private static final Pattern FSU_CODE_PATTERN = Pattern.compile(
            "<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final SetThresholdSafetyGate safetyGate;
    private final SetThresholdAuditService auditService;
    private final SetThresholdService setThresholdService;

    public SetThresholdCommandHandler(LoginService loginService,
                                       SetThresholdSafetyGate safetyGate,
                                       SetThresholdAuditService auditService,
                                       SetThresholdService setThresholdService) {
        this.loginService = loginService;
        this.safetyGate = safetyGate;
        this.auditService = auditService;
        this.setThresholdService = setThresholdService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.SET_THRESHOLD;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            log.warn("SET_THRESHOLD 失败: context 为 null");
            return buildErrorResponse("5001", "上下文为空");
        }

        // 提取 Info XML
        String infoXml = context.getSoapMessage() != null
                ? context.getSoapMessage().getInfo() : null;
        if (infoXml == null || infoXml.trim().isEmpty()) {
            log.warn("SET_THRESHOLD 失败: Info 为空");
            return buildErrorResponse("2001", "缺少 Info");
        }

        // 提取 FSUCode
        String fsuCode = extractFsuCode(infoXml);
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            log.warn("SET_THRESHOLD 失败: 缺少 FSUCode");
            return buildErrorResponse("2001", "缺少 FSUCode");
        }
        fsuCode = fsuCode.trim();

        // 校验登录态
        if (!loginService.isLoggedIn(fsuCode)) {
            log.warn("SET_THRESHOLD 失败: FSU 未登录 fsuCode={}", fsuCode);
            return buildErrorResponse("1002", "FSU 未登录或已离线: " + fsuCode);
        }

        // 提取门限设置参数
        ThresholdParams params = extractThresholdParams(context.getXmlData());
        if (params == null || params.signalId == null || params.signalId.trim().isEmpty()) {
            log.warn("SET_THRESHOLD 失败: 缺少 SignalID fsuCode={}", fsuCode);
            return buildErrorResponse("2003", "缺少 SignalID");
        }

        log.debug("SET_THRESHOLD 请求: fsuCode={}, signalId={}", fsuCode, params.signalId);

        // 安全门禁检查
        SetThresholdSafetyDecision decision = safetyGate.check(fsuCode, isConfirmed(context));
        if (!decision.isAllowed()) {
            log.warn("SET_THRESHOLD 安全门禁拒绝: fsuCode={}, resultCode={}",
                    fsuCode, decision.getResultCode());
            return buildErrorResponse(decision.getResultCode(), decision.getResultDesc());
        }

        // 开始审计
        SetThresholdAuditRecord.Builder auditBuilder = auditService.begin()
                .fsuCode(fsuCode)
                .signalId(params.signalId)
                .alarmUpper(params.alarmUpper)
                .alarmLower(params.alarmLower)
                .alarmUpperUrgent(params.alarmUpperUrgent)
                .alarmLowerUrgent(params.alarmLowerUrgent)
                .confirmed(true)
                .operator("SYSTEM");

        // 调用 SetThresholdService
        SetThresholdResult result = setThresholdService.execute(
                fsuCode, null, params.signalId,
                params.alarmUpper, params.alarmLower,
                params.alarmUpperUrgent, params.alarmLowerUrgent);

        // 记录完整审计
        SetThresholdAuditRecord auditRecord = auditBuilder
                .resultCode(result.getResultCode())
                .resultDesc(result.getResultDesc())
                .success(result.isSuccess())
                .realCall(false)
                .errors(result.getErrors())
                .completeTime(java.time.LocalDateTime.now())
                .build();
        auditService.record(auditRecord);

        // 构造 CommandResult
        return buildCommandResult(result);
    }

    /**
     * 检查上下文是否包含二次确认标记。
     * 当前通过 attributes 中的 confirmed 标记位判断。
     */
    private boolean isConfirmed(CommandContext context) {
        if (context == null) return false;
        Boolean confirmed = context.getAttribute("confirmed");
        return confirmed != null && confirmed;
    }

    /**
     * 从 xmlData 中提取门限设置参数。
     * 格式为 Signal 节点包含 SignalID + 门限字段。
     */
    private ThresholdParams extractThresholdParams(XmlDataModel xmlData) {
        if (xmlData == null) return null;

        List<Map<String, String>> items = xmlData.getItems();
        if (items == null || items.isEmpty()) return null;

        // 取第一个 Signal 的参数
        Map<String, String> first = items.get(0);
        if (first == null) return null;

        ThresholdParams params = new ThresholdParams();
        params.signalId = getFieldIgnoreCase(first, "SignalID");
        params.alarmUpper = getFieldIgnoreCase(first, "AlarmUpper");
        params.alarmLower = getFieldIgnoreCase(first, "AlarmLower");
        params.alarmUpperUrgent = getFieldIgnoreCase(first, "AlarmUpperUrgent");
        params.alarmLowerUrgent = getFieldIgnoreCase(first, "AlarmLowerUrgent");
        return params;
    }

    private String getFieldIgnoreCase(Map<String, String> map, String key) {
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

    /**
     * 门限参数字段容器。
     */
    private static class ThresholdParams {
        String signalId;
        String alarmUpper;
        String alarmLower;
        String alarmUpperUrgent;
        String alarmLowerUrgent;
    }

    /**
     * 构造 SET_THRESHOLD 成功/失败 CommandResult。
     */
    private CommandResult buildCommandResult(SetThresholdResult result) {
        CommandResult cmdResult = new CommandResult();
        cmdResult.setPkType(BInterfacePkType.SET_THRESHOLD);
        cmdResult.setImplemented(true);

        if (result.isSuccess()) {
            cmdResult.setSuccess(true);
            cmdResult.setResultCode("0");
            cmdResult.setResultDesc("OK");
            cmdResult.setResponseInfoXml(
                    "<ResultCode>0</ResultCode><Count>" + result.getCount() + "</Count>");
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
        result.setPkType(BInterfacePkType.SET_THRESHOLD);
        result.setSuccess(false);
        result.setResultCode(resultCode);
        result.setResultDesc(message);
        result.setImplemented(true);
        result.setResponseInfoXml("<ResultCode>" + resultCode + "</ResultCode>");
        result.addError(message);
        return result;
    }
}
