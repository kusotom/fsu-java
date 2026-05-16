package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetFtpResult;
import com.dcim.platform.module.binterface.service.GetFtpService;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 GET_FTP 命令处理器。
 *
 * <p>SC 获取 FSU 的 FTP 配置参数。
 * 职责：校验登录态、提取 FSUCode/FileType、调用 GetFtpService、构造响应。</p>
 *
 * <p>与 SET_FTP 无关。GET_FTP 是只读查询，不修改 FSU 的 FTP 配置。</p>
 */
@Component
public class GetFtpCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(GetFtpCommandHandler.class);

    private static final Pattern FSU_CODE_PATTERN = Pattern.compile(
            "<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern FILE_TYPE_PATTERN = Pattern.compile(
            "<FileType[^>]*>(.*?)</FileType>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final GetFtpService getFtpService;

    public GetFtpCommandHandler(LoginService loginService, GetFtpService getFtpService) {
        this.loginService = loginService;
        this.getFtpService = getFtpService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.GET_FTP;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            log.warn("GET_FTP 失败: context 为 null");
            return CommandResult.error(BInterfacePkType.GET_FTP, "5001", "上下文为空");
        }

        // 提取 Info XML
        String infoXml = context.getSoapMessage() != null
                ? context.getSoapMessage().getInfo() : null;
        if (infoXml == null || infoXml.trim().isEmpty()) {
            log.warn("GET_FTP 失败: Info 为空");
            return buildErrorResponse("2001", "缺少 Info");
        }

        // 提取 FSUCode
        String fsuCode = extractFsuCode(infoXml);
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            log.warn("GET_FTP 失败: 缺少 FSUCode");
            return buildErrorResponse("2001", "缺少 FSUCode");
        }
        fsuCode = fsuCode.trim();

        // 校验 FSU 是否已登录
        if (!loginService.isLoggedIn(fsuCode)) {
            log.warn("GET_FTP 失败: FSU 未登录 fsuCode={}", fsuCode);
            return buildErrorResponse("1002", "FSU 未登录或已离线: " + fsuCode);
        }

        // 提取 FileType（可选）
        String fileType = extractFileType(infoXml);

        log.debug("GET_FTP 请求: fsuCode={}, fileType={}", fsuCode, fileType);

        // 调用 GetFtpService
        GetFtpResult result = getFtpService.execute(fsuCode, null, fileType);

        // 构造 CommandResult
        return buildCommandResult(result);
    }

    private CommandResult buildCommandResult(GetFtpResult result) {
        CommandResult cmdResult = new CommandResult();
        cmdResult.setPkType(BInterfacePkType.GET_FTP);
        cmdResult.setImplemented(true);

        if (result.isSuccess()) {
            cmdResult.setSuccess(true);
            cmdResult.setResultCode("0");
            cmdResult.setResultDesc("查询成功");

            // Info: <ResultCode>0</ResultCode>
            cmdResult.setResponseInfoXml("<ResultCode>0</ResultCode>");

            // xmlData: FTPConfig
            XmlDataModel xmlData = new XmlDataModel();
            xmlData.setRootName("FTPConfig");
            java.util.LinkedHashMap<String, String> config = new java.util.LinkedHashMap<>();
            if (result.getHost() != null) config.put("Host", result.getHost());
            config.put("Port", String.valueOf(result.getPort()));
            if (result.getUsername() != null) config.put("Username", result.getUsername());
            config.put("PassiveMode", String.valueOf(result.isPassiveMode()));
            if (result.getBasePath() != null) config.put("BasePath", result.getBasePath());
            xmlData.addItem(config);

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
        result.setPkType(BInterfacePkType.GET_FTP);
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

    /**
     * 从 Info XML 中提取 FileType。
     */
    public static String extractFileType(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher matcher = FILE_TYPE_PATTERN.matcher(infoXml);
        if (matcher.find()) {
            String type = matcher.group(1).trim();
            return type.isEmpty() ? null : type;
        }
        return null;
    }
}
