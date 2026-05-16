package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginResult;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 LOGIN 命令处理器。
 *
 * 处理 FSU 登录认证请求。
 *
 * 处理流程：
 * <ol>
 *   <li>从 CommandContext 中提取 FSUCode（来自 Info 层）</li>
 *   <li>从 xmlData 中提取 DeviceInfo 作为注册元数据</li>
 *   <li>调用 {@link LoginService} 完成注册和 Session 建立</li>
 *   <li>构造 LOGIN 响应 Info（ResultCode / SessionID / ExpireSeconds / ServerTime）</li>
 * </ol>
 *
 * 边界：
 * - 不直接解析 SOAP Envelope
 * - 不访问数据库（委托 LoginService）
 * - 不访问网络
 * - 不访问 Controller
 */
@Component
public class LoginCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(LoginCommandHandler.class);

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final LoginService loginService;

    public LoginCommandHandler(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.LOGIN;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            return CommandResult.error(BInterfacePkType.LOGIN, "5001", "命令上下文为空");
        }

        try {
            // 1. 提取 FSUCode（来自 Info 层）
            String infoXml = context.getSoapMessage() != null
                    ? context.getSoapMessage().getInfo() : null;
            String fsuCode = extractFsuCode(infoXml);

            if (fsuCode == null) {
                log.warn("LOGIN 失败: Info 中未找到 FSUCode");
                return CommandResult.error(BInterfacePkType.LOGIN, "2001", "缺少 FSUCode");
            }

            // 2. 提取远程地址（可选）
            String remoteAddr = context.getAttribute("remoteAddr");

            // 3. 调用 LoginService
            LoginResult loginResult = loginService.login(fsuCode, remoteAddr);

            // 4. 构造响应
            return buildResponse(loginResult, fsuCode);

        } catch (Exception e) {
            log.error("LOGIN Handler 异常", e);
            return CommandResult.error(BInterfacePkType.LOGIN, "5001",
                    "登录处理异常: " + e.getMessage());
        }
    }

    // ==================== 响应构造 ====================

    private CommandResult buildResponse(LoginResult loginResult, String fsuCode) {
        if (!loginResult.isSuccess()) {
            return CommandResult.error(BInterfacePkType.LOGIN,
                    loginResult.getResultCode(), loginResult.getResultDesc());
        }

        // 构造成功响应 Info
        Instant now = Instant.now();
        String serverTime = ISO_FORMATTER.format(now.atZone(ZoneId.systemDefault()));

        String infoXml = "<ResultCode>0</ResultCode>"
                + "<SessionID>" + loginResult.getSessionId() + "</SessionID>"
                + "<ExpireSeconds>" + LoginService.DEFAULT_EXPIRE_SECONDS + "</ExpireSeconds>"
                + "<ServerTime>" + serverTime + "</ServerTime>";

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setResultCode("0");
        result.setResultDesc("登录成功");
        result.setPkType(BInterfacePkType.LOGIN);
        result.setImplemented(true);
        result.setResponseInfoXml(infoXml);
        result.setResponseXmlData(null); // LOGIN 响应 xmlData 为空
        return result;
    }

    // ==================== 字段提取 ====================

    /**
     * 从 Info XML 中提取 FSUCode。
     * 使用正则表达式，支持 FSUCode / FsuCode 大小写变体。
     */
    public static String extractFsuCode(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        // 大小写不敏感匹配
        Pattern pattern = Pattern.compile("<FSUCode[^>]*>(.*?)</FSUCode>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(infoXml);
        if (matcher.find()) {
            String code = matcher.group(1).trim();
            return code.isEmpty() ? null : code;
        }
        return null;
    }

    /**
     * 从 xmlData DeviceInfo 中提取设备信息（供扩展用）。
     */
    @SuppressWarnings("unused")
    private String extractDeviceField(XmlDataModel xmlData, String fieldName) {
        if (xmlData == null || xmlData.itemCount() == 0) return null;
        return xmlData.getFirstItemField(fieldName);
    }
}
