package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.TimeCheckResult;
import com.dcim.platform.module.binterface.service.TimeCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 TIME_CHECK 命令处理器。
 *
 * <p>处理 SC 与 FSU 之间的时间同步校验。
 * SC 向 FSU 发送标准时间，FSU 返回自身系统时间。</p>
 *
 * <p>不修改 FSU 时间，不修改系统时间，不做持久化。</p>
 */
@Component
public class TimeCheckCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(TimeCheckCommandHandler.class);

    private static final Pattern STANDARD_TIME_PATTERN = Pattern.compile(
            "<StandardTime[^>]*>(.*?)</StandardTime>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern FSU_CODE_PATTERN = Pattern.compile(
            "<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final TimeCheckService timeCheckService;

    public TimeCheckCommandHandler(LoginService loginService, TimeCheckService timeCheckService) {
        this.loginService = loginService;
        this.timeCheckService = timeCheckService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.TIME_CHECK;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            log.warn("TIME_CHECK 失败: context 为 null");
            return CommandResult.error(BInterfacePkType.TIME_CHECK, "5001", "上下文为空");
        }

        try {
            // 1. 提取 Info XML
            String infoXml = context.getSoapMessage() != null
                    ? context.getSoapMessage().getInfo() : null;
            if (infoXml == null || infoXml.trim().isEmpty()) {
                log.warn("TIME_CHECK 失败: Info 为空");
                return buildErrorResponse("2001", "缺少 Info");
            }

            // 2. 提取 FSUCode
            String fsuCode = extractFsuCode(infoXml);
            if (fsuCode == null || fsuCode.trim().isEmpty()) {
                log.warn("TIME_CHECK 失败: 缺少 FSUCode");
                return buildErrorResponse("2001", "缺少 FSUCode");
            }
            fsuCode = fsuCode.trim();

            // 3. 提取 StandardTime
            String standardTime = extractStandardTime(infoXml);
            if (standardTime == null || standardTime.trim().isEmpty()) {
                log.warn("TIME_CHECK 失败: 缺少 StandardTime fsuCode={}", fsuCode);
                return buildErrorResponse("2003", "缺少 StandardTime");
            }
            standardTime = standardTime.trim();

            // 4. 校验 FSU 是否已登录
            if (!loginService.isLoggedIn(fsuCode)) {
                log.warn("TIME_CHECK 失败: FSU 未登录 fsuCode={}", fsuCode);
                return buildErrorResponse("1002", "FSU 未登录或已离线: " + fsuCode);
            }

            log.debug("TIME_CHECK 请求: fsuCode={}, standardTime={}", fsuCode, standardTime);

            // 5. 调用 TimeCheckService
            TimeCheckResult result = timeCheckService.execute(fsuCode, null, standardTime);

            // 6. 构造 CommandResult
            return buildCommandResult(result);

        } catch (Exception e) {
            log.error("TIME_CHECK Handler 异常", e);
            return CommandResult.error(BInterfacePkType.TIME_CHECK, "5001",
                    "时间同步处理异常: " + e.getMessage());
        }
    }

    // ==================== 响应构造 ====================

    private CommandResult buildCommandResult(TimeCheckResult result) {
        CommandResult cmdResult = new CommandResult();
        cmdResult.setPkType(BInterfacePkType.TIME_CHECK);
        cmdResult.setImplemented(true);

        if (result.isSuccess()) {
            cmdResult.setSuccess(true);
            cmdResult.setResultCode("0");
            cmdResult.setResultDesc("时间同步成功");

            // Info: <ResultCode>0</ResultCode><FSUTime>...</FSUTime>
            String infoXml = "<ResultCode>0</ResultCode>";
            if (result.getFsuTime() != null) {
                infoXml += "<FSUTime>" + result.getFsuTime() + "</FSUTime>";
            }
            cmdResult.setResponseInfoXml(infoXml);
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

    private CommandResult buildErrorResponse(String resultCode, String message) {
        CommandResult result = new CommandResult();
        result.setPkType(BInterfacePkType.TIME_CHECK);
        result.setSuccess(false);
        result.setResultCode(resultCode);
        result.setResultDesc(message);
        result.setImplemented(true);
        result.setResponseInfoXml("<ResultCode>" + resultCode + "</ResultCode>");
        result.addError(message);
        return result;
    }

    // ==================== 字段提取 ====================

    /**
     * 从 Info XML 中提取 FSUCode。
     */
    public static String extractFsuCode(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher matcher = FSU_CODE_PATTERN.matcher(infoXml);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    /**
     * 从 Info XML 中提取 StandardTime。
     */
    public static String extractStandardTime(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher matcher = STANDARD_TIME_PATTERN.matcher(infoXml);
        if (matcher.find()) {
            String time = matcher.group(1).trim();
            return time.isEmpty() ? null : time;
        }
        return null;
    }
}
