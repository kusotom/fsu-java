package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetSuFtpResult;
import com.dcim.platform.module.binterface.service.GetSuFtpService;
import com.dcim.platform.module.binterface.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 GET_SUFTP 命令处理器 (2024 标准, Code=801)。
 *
 * <p>BIF-P4-013: SC → FSU FTP 参数查询。只读，不修改 FSU 配置。</p>
 */
@Component
public class GetSuFtpCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(GetSuFtpCommandHandler.class);

    private static final Pattern SUID_PATTERN = Pattern.compile(
            "<SUID[^>]*>(.*?)</SUID>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FSU_CODE_PATTERN = Pattern.compile(
            "<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final GetSuFtpService getSuFtpService;

    public GetSuFtpCommandHandler(LoginService loginService, GetSuFtpService getSuFtpService) {
        this.loginService = loginService;
        this.getSuFtpService = getSuFtpService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.GET_SUFTP;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) return buildError("5001", "上下文为空");

        String infoXml = context.getSoapMessage() != null
                ? context.getSoapMessage().getInfo() : null;
        if (infoXml == null || infoXml.trim().isEmpty()) return buildError("2001", "缺少 Info");

        String suid = extractSuid(infoXml);
        if (suid == null || suid.trim().isEmpty()) return buildError("2001", "缺少 SUID");
        suid = suid.trim();

        if (!loginService.isLoggedIn(suid)) {
            return buildError("1002", "FSU 未登录或已离线: " + suid);
        }

        GetSuFtpResult result = getSuFtpService.execute(suid, null);
        return buildResult(result);
    }

    private CommandResult buildResult(GetSuFtpResult r) {
        CommandResult cmd = new CommandResult();
        cmd.setPkType(BInterfacePkType.GET_SUFTP);
        cmd.setImplemented(true);
        cmd.setSuccess(r.isSuccess());
        cmd.setResultCode(r.getResultCode());
        cmd.setResultDesc(r.getResultDesc());
        cmd.setResponseInfoXml("<ResultCode>" + r.getResultCode() + "</ResultCode>");
        if (r.hasErrors()) r.getErrors().forEach(cmd::addError);
        return cmd;
    }

    private CommandResult buildError(String code, String msg) {
        CommandResult cmd = new CommandResult();
        cmd.setPkType(BInterfacePkType.GET_SUFTP);
        cmd.setSuccess(false);
        cmd.setResultCode(code);
        cmd.setResultDesc(msg);
        cmd.setImplemented(true);
        cmd.setResponseInfoXml("<ResultCode>" + code + "</ResultCode>");
        cmd.addError(msg);
        return cmd;
    }

    static String extractSuid(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher m = SUID_PATTERN.matcher(infoXml);
        if (m.find()) { String v = m.group(1).trim(); if (!v.isEmpty()) return v; }
        Matcher m2 = FSU_CODE_PATTERN.matcher(infoXml);
        return m2.find() ? m2.group(1).trim() : null;
    }
}
