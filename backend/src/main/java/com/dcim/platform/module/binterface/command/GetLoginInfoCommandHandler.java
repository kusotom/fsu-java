package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetLoginInfoResult;
import com.dcim.platform.module.binterface.service.GetLoginInfoService;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 GET_LOGININFO 命令处理器。
 *
 * <p>SC 查询 FSU 的登录状态和在线信息。
 * 职责：校验登录态、提取 FSUCode、调用 GetLoginInfoService、构造响应。</p>
 *
 * <p>不直接访问 HTTP，不直接调用 FsuServiceClient，不做持久化。</p>
 */
@Component
public class GetLoginInfoCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(GetLoginInfoCommandHandler.class);

    private static final Pattern FSU_CODE_PATTERN = Pattern.compile(
            "<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final GetLoginInfoService getLoginInfoService;

    public GetLoginInfoCommandHandler(LoginService loginService, GetLoginInfoService getLoginInfoService) {
        this.loginService = loginService;
        this.getLoginInfoService = getLoginInfoService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.GET_LOGININFO;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            log.warn("GET_LOGININFO 失败: context 为 null");
            return CommandResult.error(BInterfacePkType.GET_LOGININFO, "5001", "上下文为空");
        }

        // 提取 Info XML
        String infoXml = context.getSoapMessage() != null
                ? context.getSoapMessage().getInfo() : null;
        if (infoXml == null || infoXml.trim().isEmpty()) {
            log.warn("GET_LOGININFO 失败: Info 为空");
            return buildErrorResponse("2001", "缺少 Info");
        }

        // 提取 FSUCode
        String fsuCode = extractFsuCode(infoXml);
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            log.warn("GET_LOGININFO 失败: 缺少 FSUCode");
            return buildErrorResponse("2001", "缺少 FSUCode");
        }
        fsuCode = fsuCode.trim();

        // 校验 FSU 是否已登录
        if (!loginService.isLoggedIn(fsuCode)) {
            log.warn("GET_LOGININFO 失败: FSU 未登录 fsuCode={}", fsuCode);
            return buildErrorResponse("1002", "FSU 未登录或已离线: " + fsuCode);
        }

        log.debug("GET_LOGININFO 请求: fsuCode={}", fsuCode);

        // 调用 GetLoginInfoService
        GetLoginInfoResult result = getLoginInfoService.execute(fsuCode, null);

        // 构造 CommandResult
        return buildCommandResult(result);
    }

    private CommandResult buildCommandResult(GetLoginInfoResult result) {
        CommandResult cmdResult = new CommandResult();
        cmdResult.setPkType(BInterfacePkType.GET_LOGININFO);
        cmdResult.setImplemented(true);

        if (result.isSuccess()) {
            cmdResult.setSuccess(true);
            cmdResult.setResultCode("0");
            cmdResult.setResultDesc("查询成功");

            // Info: <ResultCode>0</ResultCode>
            String responseInfoXml = "<ResultCode>0</ResultCode>";

            // xmlData: LoginInfo
            XmlDataModel xmlData = new XmlDataModel();
            xmlData.setRootName("LoginInfo");
            java.util.LinkedHashMap<String, String> loginInfo = new java.util.LinkedHashMap<>();
            loginInfo.put("FSUCode", result.getFsuCode());
            if (result.getLoginStatus() != null) loginInfo.put("LoginStatus", result.getLoginStatus());
            if (result.getOnlineStatus() != null) loginInfo.put("OnlineStatus", result.getOnlineStatus());
            if (result.getSessionId() != null) loginInfo.put("SessionID", result.getSessionId());
            if (result.getLoginTime() != null) loginInfo.put("LoginTime", result.getLoginTime());
            if (result.getLastHeartbeat() != null) loginInfo.put("LastHeartbeat", result.getLastHeartbeat());
            xmlData.addItem(loginInfo);

            cmdResult.setResponseInfoXml(responseInfoXml);
            cmdResult.setResponseXmlData(xmlData);
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
        result.setPkType(BInterfacePkType.GET_LOGININFO);
        result.setSuccess(false);
        result.setResultCode(resultCode);
        result.setResultDesc(message);
        result.setImplemented(true);
        result.setResponseInfoXml("<ResultCode>" + resultCode + "</ResultCode>");
        result.addError(message);
        return result;
    }

    /**
     * 从 Info XML 中提取 FSUCode。
     */
    public static String extractFsuCode(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher matcher = FSU_CODE_PATTERN.matcher(infoXml);
        return matcher.find() ? matcher.group(1).trim() : null;
    }
}
