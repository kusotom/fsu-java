package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetActiveAlarmResult;
import com.dcim.platform.module.binterface.service.GetActiveAlarmService;
import com.dcim.platform.module.binterface.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 GET_ACTIVEALARM 命令处理器 (2024 标准, Code=603)。
 *
 * <p>BIF-P4-016: SC → FSU 活动告警查询。只读。</p>
 */
@Component
public class GetActiveAlarmCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(GetActiveAlarmCommandHandler.class);
    private static final Pattern SUID_P = Pattern.compile("<SUID[^>]*>(.*?)</SUID>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FSU_P = Pattern.compile("<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final GetActiveAlarmService service;

    public GetActiveAlarmCommandHandler(LoginService loginService, GetActiveAlarmService service) {
        this.loginService = loginService;
        this.service = service;
    }

    @Override
    public BInterfacePkType getSupportedPkType() { return BInterfacePkType.GET_ACTIVEALARM; }

    @Override
    public CommandResult handle(CommandContext ctx) {
        if (ctx == null) return err("5001", "上下文为空");
        String info = ctx.getSoapMessage() != null ? ctx.getSoapMessage().getInfo() : null;
        if (info == null || info.trim().isEmpty()) return err("2001", "缺少 Info");

        String suid = extract(SUID_P, info);
        if (suid == null) suid = extract(FSU_P, info);
        if (suid == null || suid.trim().isEmpty()) return err("2001", "缺少 SUID");
        suid = suid.trim();

        if (!loginService.isLoggedIn(suid)) return err("1002", "FSU 未登录: " + suid);

        GetActiveAlarmResult r = service.execute(suid, null);
        CommandResult c = new CommandResult(); c.setPkType(BInterfacePkType.GET_ACTIVEALARM);
        c.setImplemented(true); c.setSuccess(r.isSuccess());
        c.setResultCode(r.getResultCode()); c.setResultDesc(r.getResultDesc());
        c.setResponseInfoXml("<ResultCode>" + r.getResultCode() + "</ResultCode><Count>" + r.getParsedCount() + "</Count>");
        if (r.hasErrors()) r.getErrors().forEach(c::addError);
        return c;
    }

    private CommandResult err(String code, String msg) {
        CommandResult c = new CommandResult(); c.setPkType(BInterfacePkType.GET_ACTIVEALARM);
        c.setSuccess(false); c.setResultCode(code); c.setResultDesc(msg); c.setImplemented(true);
        c.setResponseInfoXml("<ResultCode>" + code + "</ResultCode>"); c.addError(msg);
        return c;
    }

    private String extract(Pattern p, String xml) {
        if (xml == null) return null;
        Matcher m = p.matcher(xml); return m.find() ? m.group(1).trim() : null;
    }
}
