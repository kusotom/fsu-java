package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.LoginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B接口 HEARTBEAT 命令处理器。
 *
 * 处理 FSU 心跳上报，复用 LoginService 的 Session 管理能力。
 *
 * 处理流程：
 * <ol>
 *   <li>从 Info 中提取 FSUCode</li>
 *   <li>校验 FSU 是否已登录（复用 LoginService.isLoggedIn）</li>
 *   <li>更新最后活跃时间（复用 LoginService.updateLastSeen）</li>
 *   <li>构造 HEARTBEAT 响应 Info（ResultCode / ServerTime）</li>
 * </ol>
 *
 * 边界：
 * - 不直接解析 SOAP Envelope
 * - 不访问数据库（委托 LoginService）
 * - 不访问网络
 * - 不访问 Controller
 */
@Component
public class HeartbeatCommandHandler implements CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(HeartbeatCommandHandler.class);

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final LoginService loginService;

    public HeartbeatCommandHandler(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.HEARTBEAT;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        if (context == null) {
            return CommandResult.error(BInterfacePkType.HEARTBEAT, "5001", "命令上下文为空");
        }

        try {
            // 1. 提取 Info XML
            String infoXml = context.getSoapMessage() != null
                    ? context.getSoapMessage().getInfo() : null;
            if (infoXml == null || infoXml.isEmpty()) {
                log.warn("HEARTBEAT 失败: Info 为空");
                return CommandResult.error(BInterfacePkType.HEARTBEAT, "2001", "缺少 Info");
            }

            // 2. 提取 FSUCode
            String fsuCode = LoginCommandHandler.extractFsuCode(infoXml);
            if (fsuCode == null) {
                log.warn("HEARTBEAT 失败: Info 中未找到 FSUCode");
                return CommandResult.error(BInterfacePkType.HEARTBEAT, "2001", "缺少 FSUCode");
            }

            // 3. 校验 FSU 是否已登录
            if (!loginService.isLoggedIn(fsuCode)) {
                log.warn("HEARTBEAT 失败: FSU 未登录 fsuCode={}", fsuCode);
                return CommandResult.error(BInterfacePkType.HEARTBEAT, "1002",
                        "FSU 未登录: " + fsuCode);
            }

            // 4. 更新最后活跃时间
            loginService.updateLastSeen(fsuCode);

            log.debug("HEARTBEAT 成功: fsuCode={}", fsuCode);

            // 5. 构造成功响应
            return buildSuccessResponse();

        } catch (Exception e) {
            log.error("HEARTBEAT Handler 异常", e);
            return CommandResult.error(BInterfacePkType.HEARTBEAT, "5001",
                    "心跳处理异常: " + e.getMessage());
        }
    }

    // ==================== 响应构造 ====================

    private CommandResult buildSuccessResponse() {
        Instant now = Instant.now();
        String serverTime = ISO_FORMATTER.format(now.atZone(ZoneId.systemDefault()));

        String infoXml = "<Result>1</Result>"
                + "<ServerTime>" + serverTime + "</ServerTime>";

        CommandResult result = new CommandResult();
        result.setSuccess(true);
        result.setResultCode("1");
        result.setResultDesc("心跳成功");
        result.setPkType(BInterfacePkType.HEARTBEAT);
        result.setImplemented(true);
        result.setResponseInfoXml(infoXml);
        result.setResponseXmlData(null); // HEARTBEAT 响应 xmlData 为空
        return result;
    }

    // ==================== 字段提取 ====================

    /**
     * 从 Info XML 中提取 SessionID。
     */
    public static String extractSessionId(String infoXml) {
        if (infoXml == null || infoXml.isEmpty()) return null;
        Pattern pattern = Pattern.compile("<SessionID[^>]*>(.*?)</SessionID>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(infoXml);
        if (matcher.find()) {
            String id = matcher.group(1).trim();
            return id.isEmpty() ? null : id;
        }
        return null;
    }
}
