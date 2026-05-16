package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetSuInfoResult;
import com.dcim.platform.module.binterface.service.GetSuInfoService;
import com.dcim.platform.module.binterface.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 GET_SUINFO 命令处理器 (2024 标准)。
 *
 * <p>BIF-P4-012: 处理 SC → FSU 在线状态查询 (Code=1001)。
 * 替代 HEARTBEAT 作为 2024 标准心跳/状态查询机制。</p>
 */
@Component
public class GetSuInfoCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(GetSuInfoCommandHandler.class);

    private static final Pattern SUID_PATTERN = Pattern.compile(
            "<SUID[^>]*>(.*?)</SUID>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FSU_CODE_PATTERN = Pattern.compile(
            "<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final GetSuInfoService getSuInfoService;

    public GetSuInfoCommandHandler(LoginService loginService, GetSuInfoService getSuInfoService) {
        this.loginService = loginService;
        this.getSuInfoService = getSuInfoService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.GET_SUINFO;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            log.warn("GET_SUINFO 失败: context 为 null");
            return buildError("5001", "上下文为空");
        }

        String infoXml = context.getSoapMessage() != null
                ? context.getSoapMessage().getInfo() : null;
        if (infoXml == null || infoXml.trim().isEmpty()) {
            return buildError("2001", "缺少 Info");
        }

        // 提取 SUID（优先 2024 标准字段，兼容 FSUCode）
        String suid = extractSuid(infoXml);
        if (suid == null || suid.trim().isEmpty()) {
            return buildError("2001", "缺少 SUID");
        }
        suid = suid.trim();

        // 校验登录态
        if (!loginService.isLoggedIn(suid)) {
            log.warn("GET_SUINFO FSU 未登录: suid={}", suid);
            return buildError("1002", "FSU 未登录或已离线: " + suid);
        }

        log.debug("GET_SUINFO 请求: suid={}", suid);

        GetSuInfoResult result = getSuInfoService.execute(suid, null);
        return buildCommandResult(result);
    }

    // ==================== 内部 ====================

    private CommandResult buildCommandResult(GetSuInfoResult result) {
        CommandResult cmd = new CommandResult();
        cmd.setPkType(BInterfacePkType.GET_SUINFO);
        cmd.setImplemented(true);

        if (result.isSuccess()) {
            cmd.setSuccess(true);
            cmd.setResultCode("0");
            cmd.setResultDesc("OK");
            StringBuilder info = new StringBuilder();
            info.append("<ResultCode>0</ResultCode>");
            if (result.getSuid() != null) info.append("<SUID>").append(result.getSuid()).append("</SUID>");
            cmd.setResponseInfoXml(info.toString());
        } else {
            cmd.setSuccess(false);
            cmd.setResultCode(result.getResultCode());
            cmd.setResultDesc(result.getResultDesc());
            cmd.setResponseInfoXml("<ResultCode>" + result.getResultCode() + "</ResultCode>");
            if (result.hasErrors()) result.getErrors().forEach(cmd::addError);
        }
        return cmd;
    }

    private CommandResult buildError(String code, String msg) {
        CommandResult cmd = new CommandResult();
        cmd.setPkType(BInterfacePkType.GET_SUINFO);
        cmd.setSuccess(false);
        cmd.setResultCode(code);
        cmd.setResultDesc(msg);
        cmd.setImplemented(true);
        cmd.setResponseInfoXml("<ResultCode>" + code + "</ResultCode>");
        cmd.addError(msg);
        return cmd;
    }

    /** 提取 SUID（优先 2024 SUID，兼容 FSUCode）。 */
    static String extractSuid(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Matcher m = SUID_PATTERN.matcher(infoXml);
        if (m.find()) {
            String v = m.group(1).trim();
            if (!v.isEmpty()) return v;
        }
        Matcher m2 = FSU_CODE_PATTERN.matcher(infoXml);
        return m2.find() ? m2.group(1).trim() : null;
    }
}
