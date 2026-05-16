package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.SetTimeResult;
import com.dcim.platform.module.binterface.service.SetTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 SET_TIME 命令处理器 (2024 标准, Code=901)。
 *
 * <p>BIF-P4-014: SC → FSU 时间同步。会修改 FSU 设备时间。</p>
 */
@Component
public class SetTimeCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SetTimeCommandHandler.class);
    private static final Pattern SUID_P = Pattern.compile("<SUID[^>]*>(.*?)</SUID>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FSUCODE_P = Pattern.compile("<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TIME_P = Pattern.compile("<TTime[^>]*>(.*?)</TTime>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final LoginService loginService;
    private final SetTimeService setTimeService;

    public SetTimeCommandHandler(LoginService loginService, SetTimeService setTimeService) {
        this.loginService = loginService;
        this.setTimeService = setTimeService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() { return BInterfacePkType.SET_TIME; }

    @Override
    public CommandResult handle(CommandContext ctx) {
        if (ctx == null) return err("5001", "上下文为空");
        String info = ctx.getSoapMessage() != null ? ctx.getSoapMessage().getInfo() : null;
        if (info == null || info.trim().isEmpty()) return err("2001", "缺少 Info");

        String suid = extract(SUID_P, info);
        if (suid == null) suid = extract(FSUCODE_P, info);
        if (suid == null || suid.trim().isEmpty()) return err("2001", "缺少 SUID");
        suid = suid.trim();

        String ttime = extract(TIME_P, info);
        if (ttime == null || ttime.trim().isEmpty()) return err("2003", "缺少 TTime");

        if (!loginService.isLoggedIn(suid)) return err("1002", "FSU 未登录: " + suid);

        SetTimeResult r = setTimeService.execute(suid, null, ttime.trim());
        CommandResult c = new CommandResult();
        c.setPkType(BInterfacePkType.SET_TIME); c.setImplemented(true);
        c.setSuccess(r.isSuccess()); c.setResultCode(r.getResultCode()); c.setResultDesc(r.getResultDesc());
        c.setResponseInfoXml("<ResultCode>" + r.getResultCode() + "</ResultCode>");
        if (r.hasErrors()) r.getErrors().forEach(c::addError);
        return c;
    }

    private CommandResult err(String code, String msg) {
        CommandResult c = new CommandResult(); c.setPkType(BInterfacePkType.SET_TIME);
        c.setSuccess(false); c.setResultCode(code); c.setResultDesc(msg); c.setImplemented(true);
        c.setResponseInfoXml("<ResultCode>" + code + "</ResultCode>"); c.addError(msg);
        return c;
    }

    private String extract(Pattern p, String xml) {
        if (xml == null) return null;
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }
}
