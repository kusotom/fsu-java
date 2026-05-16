package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.SureadyResult;
import com.dcim.platform.module.binterface.service.SureadyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 SUREADY 命令处理器 (2024 标准, Code=103)。
 *
 * <p>BIF-P4-017: FSU → SC 注册准备确认。</p>
 */
@Component
public class SureadyCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(SureadyCommandHandler.class);
    private static final Pattern SUID_P = Pattern.compile("<SUID[^>]*>(.*?)</SUID>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FSU_P = Pattern.compile("<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private final SureadyService service;

    public SureadyCommandHandler(SureadyService service) { this.service = service; }

    @Override public BInterfacePkType getSupportedPkType() { return BInterfacePkType.SUREADY; }

    @Override
    public CommandResult handle(CommandContext ctx) {
        if (ctx == null) return err("5001", "上下文为空");
        String info = ctx.getSoapMessage() != null ? ctx.getSoapMessage().getInfo() : null;
        if (info == null || info.trim().isEmpty()) return err("2001", "缺少 Info");

        String suid = extract(SUID_P, info);
        if (suid == null) suid = extract(FSU_P, info);
        if (suid == null || suid.trim().isEmpty()) return err("2001", "缺少 SUID");
        suid = suid.trim();

        SureadyResult r = service.execute(suid);
        CommandResult c = new CommandResult();
        c.setPkType(BInterfacePkType.SUREADY);
        c.setImplemented(true);
        c.setSuccess(r.isSuccess());
        c.setResultCode(r.getResultCode());
        c.setResultDesc(r.getResultDesc());

        StringBuilder infoXml = new StringBuilder();
        infoXml.append("<SUID>").append(suid).append("</SUID>");
        infoXml.append("<Result>").append(r.isSuccess() ? "SUCCESS" : "FAILURE").append("</Result>");
        if (!r.isSuccess()) {
            infoXml.append("<FailureCode>").append(r.getResultCode()).append("</FailureCode>");
            infoXml.append("<FailureCause>").append(r.getResultDesc()).append("</FailureCause>");
        }
        c.setResponseInfoXml(infoXml.toString());

        c.setResponseXmlData(null); // xmlData not needed for SUREADY_ACK per protocol
        if (r.hasErrors()) r.getErrors().forEach(c::addError);
        return c;
    }

    private CommandResult err(String code, String msg) {
        CommandResult c = new CommandResult(); c.setPkType(BInterfacePkType.SUREADY);
        c.setSuccess(false); c.setResultCode(code); c.setResultDesc(msg); c.setImplemented(true);
        c.setResponseInfoXml("<Result>FAILURE</Result><FailureCode>" + code + "</FailureCode><FailureCause>" + msg + "</FailureCause>");
        c.addError(msg); return c;
    }

    private String extract(Pattern p, String xml) {
        if (xml == null) return null;
        Matcher m = p.matcher(xml); return m.find() ? m.group(1).trim() : null;
    }
}
