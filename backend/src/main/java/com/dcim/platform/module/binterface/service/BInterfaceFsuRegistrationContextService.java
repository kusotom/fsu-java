package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto;
import com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto.DeviceCapabilityDto;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * BIF2016-AUTH-002: FSU 注册上下文只读查询服务。
 *
 * 从 BInterfaceMessageLog (最新 LOGIN raw message) 解析注册字段，
 * 合并 session / status / device 数据，构建只读注册上下文。
 * 不写数据库。
 */
@Service
public class BInterfaceFsuRegistrationContextService {

    private static final Logger log = LoggerFactory.getLogger(BInterfaceFsuRegistrationContextService.class);

    private final BInterfaceMessageLogService messageLogService;
    private final BInterfaceFsuStatusRepository fsuStatusRepository;
    private final BInterfaceSessionRepository sessionRepository;
    private final FsuDeviceRepository fsuDeviceRepository;
    private final SoapMessageHandler soapHandler;
    private final XmlDataParser xmlDataParser;

    public BInterfaceFsuRegistrationContextService(
            BInterfaceMessageLogService messageLogService,
            BInterfaceFsuStatusRepository fsuStatusRepository,
            BInterfaceSessionRepository sessionRepository,
            FsuDeviceRepository fsuDeviceRepository,
            SoapMessageHandler soapHandler,
            XmlDataParser xmlDataParser) {
        this.messageLogService = messageLogService;
        this.fsuStatusRepository = fsuStatusRepository;
        this.sessionRepository = sessionRepository;
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.soapHandler = soapHandler;
        this.xmlDataParser = xmlDataParser;
    }

    /**
     * 根据 fsuCode 构建只读注册上下文。
     */
    public BInterfaceFsuRegistrationContextDto buildContext(String fsuCode) {
        BInterfaceFsuRegistrationContextDto ctx = new BInterfaceFsuRegistrationContextDto();
        ctx.setFsuCode(fsuCode);
        ctx.setContextSource("latest-login-message-log");

        List<String> missing = new ArrayList<>();

        // 1. 从 message log 获取最新 LOGIN raw
        BInterfaceMessageLogEntity latestLogin = findLatestLogin(fsuCode);
        if (latestLogin != null) {
            ctx.setRawLoginMessageId(latestLogin.getId());
            ctx.setRawLoginCreatedAt(latestLogin.getCreatedAt());
            parseLoginFields(latestLogin.getRawMessage(), ctx, missing);
        } else {
            ctx.setContextCompleteness("MISSING");
            ctx.setMissingFields(List.of("LOGIN raw message"));
            ctx.setAuthMode("unknown");
            return ctx;
        }

        // 2. 从 session 合并
        FsuDeviceEntity device = fsuDeviceRepository.findByFsuCode(fsuCode).orElse(null);
        Long fsuId = device != null ? device.getId() : null;
        if (fsuId != null) {
            mergeSession(fsuId, ctx, missing);
        }

        // 3. 从 fsu_status 合并
        mergeFsuStatus(fsuCode, ctx, missing);

        // 4. 设置 FsuId
        if (device != null) {
            ctx.setFsuId(String.valueOf(device.getId()));
        }

        // 5. 判断 authMode
        determineAuthMode(ctx);

        // 6. 推断 DeviceType
        inferDeviceTypes(ctx);

        // 7. 计算完整性
        computeCompleteness(ctx, missing);

        return ctx;
    }

    // ── LOGIN raw 解析 ──

    private BInterfaceMessageLogEntity findLatestLogin(String fsuCode) {
        List<BInterfaceMessageLogEntity> logs = messageLogService.findByFsuCode(fsuCode);
        if (logs == null) return null;
        for (BInterfaceMessageLogEntity log : logs) {
            if ("LOGIN".equalsIgnoreCase(log.getCommand())
                    || "INBOUND".equalsIgnoreCase(log.getDirection())) {
                // 优先匹配 command=LOGIN 的入站消息
                if ("LOGIN".equalsIgnoreCase(log.getCommand())) return log;
            }
        }
        // 回退: 任意 INBOUND 消息
        for (BInterfaceMessageLogEntity log : logs) {
            if ("INBOUND".equalsIgnoreCase(log.getDirection())) return log;
        }
        return null;
    }

    void parseLoginFields(String rawMessage, BInterfaceFsuRegistrationContextDto ctx,
                          List<String> missing) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            missing.add("rawMessage");
            return;
        }

        try {
            var message = soapHandler.parse(rawMessage);
            String infoXml = message.getInfo();
            if (infoXml == null || infoXml.isEmpty()) {
                missing.add("Info XML");
                ctx.setContextSource("raw-message-parse-failed");
                return;
            }

            Map<String, String> fields = xmlDataParser.parseFields(infoXml);

            ctx.setFsuCode(or(ctx.getFsuCode(),
                    field(fields, "FsuCode"), field(fields, "FSUCode")));
            ctx.setFsuId(or(ctx.getFsuId(),
                    field(fields, "FsuId"), field(fields, "FSUID")));
            ctx.setFsuIp(or(ctx.getFsuIp(),
                    field(fields, "FsuIP"), field(fields, "FsuIp"), field(fields, "FSUIP")));
            ctx.setMacId(or(ctx.getMacId(),
                    field(fields, "MacId"), field(fields, "MACID")));
            ctx.setVersion(or(ctx.getVersion(),
                    field(fields, "Version"), field(fields, "Vervion"))); // 协议原文拼写
            ctx.setAuthUsername(or(ctx.getAuthUsername(),
                    field(fields, "UserName"), field(fields, "USERNAME")));

            // DeviceList: 从 infoXml 原始 XML 中提取 <Device> 元素
            parseDeviceList(infoXml, ctx, missing);

            // 追踪缺失
            if (isBlank(ctx.getFsuIp())) missing.add("FsuIP");
            if (isBlank(ctx.getMacId())) missing.add("MacId");
            if (isBlank(ctx.getVersion())) missing.add("Version");
            if (isBlank(ctx.getAuthUsername())) missing.add("UserName");
            if (ctx.getDeviceCapabilities().isEmpty()) missing.add("DeviceList");

        } catch (Exception e) {
            log.warn("LOGIN raw 解析失败: fsuCode={}", ctx.getFsuCode(), e);
            missing.add("parse error: " + e.getMessage());
            ctx.setContextSource("raw-message-parse-failed");
        }
    }

    /**
     * 从 Info XML 中解析 DeviceList (package-private 供测试直接验证)。
     * 支持:
     * 1. 属性形式: <Device Id="xxx" Code="xxx"/> (含 DeviceID/DeviceId/deviceId 变体)
     * 2. 子节点形式: <Device><Id>xxx</Id><Code>xxx</Code></Device> (含 DeviceID/DeviceCode 变体)
     * 大小写不敏感。
     */
    public void parseDeviceList(String infoXml, BInterfaceFsuRegistrationContextDto ctx,
                         List<String> missing) {
        if (infoXml == null || infoXml.isEmpty()) return;

        String dlContent = extractXmlSection(infoXml, "DeviceList");
        if (dlContent == null || dlContent.isEmpty()) return;

        java.util.regex.Pattern devicePattern = java.util.regex.Pattern.compile(
                "<Device\\b([^>]*)>(.*?)</Device>|<Device\\b([^>]*)/>",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher dm = devicePattern.matcher(dlContent);

        // 设备ID字段的候选名 (B接口2016 + 常见变体), 按优先级排列
        String[] idNames = {"Id", "ID", "DeviceID", "DeviceId", "deviceId", "DEVICEID"};
        String[] codeNames = {"Code", "CODE", "DeviceCode", "DeviceCODE", "deviceCode"};

        while (dm.find()) {
            String attrsStr = dm.group(1) != null ? dm.group(1) : dm.group(3);
            String innerXml = dm.group(2);

            DeviceCapabilityDto cap = new DeviceCapabilityDto();
            cap.setSource("LOGIN");
            cap.setConfidence("high");
            cap.setValid(true);
            List<String> capMissing = new ArrayList<>();

            // 从属性解析: 尝试所有 Id/Code 候选名, 同时保留原始属性
            if (attrsStr != null) {
                for (String name : idNames) {
                    String v = extractAttr(attrsStr, name);
                    if (v != null) { cap.setDeviceId(v); cap.getRawAttributes().put(name, v); break; }
                }
                for (String name : codeNames) {
                    String v = extractAttr(attrsStr, name);
                    if (v != null) { cap.setDeviceCode(v); cap.getRawAttributes().put(name, v); break; }
                }
            }

            // 从子节点解析 (覆盖属性值，子节点优先)
            if (innerXml != null && !innerXml.trim().isEmpty()) {
                for (String name : idNames) {
                    String v = extractXmlValue(innerXml, name);
                    if (v != null) { cap.setDeviceId(v); cap.getRawAttributes().put(name, v); break; }
                }
                for (String name : codeNames) {
                    String v = extractXmlValue(innerXml, name);
                    if (v != null) { cap.setDeviceCode(v); cap.getRawAttributes().put(name, v); break; }
                }
            }

            // 验证
            if (isBlank(cap.getDeviceId())) {
                cap.setValid(false);
                capMissing.add("Device.Id");
            } else {
                if (cap.getDeviceId().length() >= 9) {
                    cap.setDeviceTypeCode(cap.getDeviceId().substring(7, 9));
                    cap.setDeviceTypeSource("device_id_inference");
                    cap.setConfidence("medium");
                }
            }
            if (isBlank(cap.getDeviceCode())) {
                capMissing.add("Device.Code");
            }

            ctx.getDeviceCapabilities().add(cap);
            if (!capMissing.isEmpty()) {
                missing.add("Device(" + cap.getDeviceId() + "): " + String.join(",", capMissing));
            }
        }
    }

    /** 从 XML 中提取叶子标签值 (大小写不敏感)。 */
    private String extractXmlValue(String xml, String tagName) {
        if (xml == null || tagName == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<" + tagName + ">([^<]*)</" + tagName + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }

    /** 提取 XML 段落内容 (大小写不敏感)。 */
    private String extractXmlSection(String xml, String tagName) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<" + tagName + "\\b[^>]*>(.*?)</" + tagName + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(xml);
        return m.find() ? m.group(1) : null;
    }

    /** 从 XML 属性字符串中提取值 (大小写不敏感)。 */
    private String extractAttr(String attrs, String attrName) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "\\b" + attrName + "\\s*=\\s*\"([^\"]*)\"",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(attrs);
        return m.find() ? m.group(1).trim() : null;
    }

    // ── 会话合并 ──

    private void mergeSession(Long fsuId, BInterfaceFsuRegistrationContextDto ctx,
                               List<String> missing) {
        try {
            Optional<BInterfaceSessionEntity> sessionOpt =
                    sessionRepository.findByFsuIdAndStatus(fsuId, "ACTIVE");
            if (sessionOpt.isPresent()) {
                BInterfaceSessionEntity s = sessionOpt.get();
                ctx.setSessionId(s.getSessionId());
                ctx.setExpireSeconds(s.getExpireSeconds());
                ctx.setLastLoginTime(s.getLoginTime());
                if (s.getRemoteAddr() != null && ctx.getFsuIp() == null) {
                    ctx.setFsuIp(s.getRemoteAddr()); // fallback from TCP source IP
                }
            } else {
                missing.add("ACTIVE session");
            }
        } catch (Exception e) {
            log.warn("Session 查询失败: fsuId={}", fsuId, e);
            missing.add("session query error");
        }
    }

    // ── 状态合并 ──

    private void mergeFsuStatus(String fsuCode, BInterfaceFsuRegistrationContextDto ctx,
                                 List<String> missing) {
        try {
            Optional<BInterfaceFsuStatusEntity> statusOpt =
                    fsuStatusRepository.findByFsuCode(fsuCode);
            if (statusOpt.isPresent()) {
                BInterfaceFsuStatusEntity st = statusOpt.get();
                ctx.setLoginStatus(st.getLoginStatus());
                ctx.setOnlineStatus(st.getOnlineStatus());
                if (ctx.getSessionId() == null && st.getSessionId() != null) {
                    ctx.setSessionId(st.getSessionId());
                }
                if (ctx.getLastLoginTime() == null && st.getLastLoginTime() != null) {
                    ctx.setLastLoginTime(st.getLastLoginTime());
                }
            } else {
                missing.add("FSU status");
            }
        } catch (Exception e) {
            log.warn("FSU status 查询失败: fsuCode={}", fsuCode, e);
            missing.add("fsu_status query error");
        }
    }

    // ── authMode 判断 ──

    void determineAuthMode(BInterfaceFsuRegistrationContextDto ctx) {
        boolean hasUser = !isBlank(ctx.getAuthUsername());
        boolean hasFsuIp = !isBlank(ctx.getFsuIp());
        boolean hasMacId = !isBlank(ctx.getMacId());
        boolean hasDeviceList = !ctx.getDeviceCapabilities().isEmpty();

        if (hasUser) {
            ctx.setAuthMode("strict-2016");
            ctx.setAuthStatus("AUTHENTICATED"); // 有用户名即视为已认证
        } else if (hasFsuIp || hasMacId || hasDeviceList) {
            ctx.setAuthMode("emerson-2016-compatible");
            ctx.setAuthStatus("COMPATIBLE");
        } else {
            ctx.setAuthMode("unknown");
            ctx.setAuthStatus("UNKNOWN");
        }
    }

    // ── DeviceType 推断 ──

    void inferDeviceTypes(BInterfaceFsuRegistrationContextDto ctx) {
        for (DeviceCapabilityDto cap : ctx.getDeviceCapabilities()) {
            if (cap.getDeviceTypeCode() == null && cap.getDeviceId() != null
                    && cap.getDeviceId().length() >= 9) {
                cap.setDeviceTypeCode(cap.getDeviceId().substring(7, 9));
                cap.setDeviceTypeSource("protocol-deviceid-inference");
            }
        }
    }

    // ── 完整性计算 ──

    void computeCompleteness(BInterfaceFsuRegistrationContextDto ctx, List<String> missing) {
        ctx.setMissingFields(missing);

        // 过滤出核心缺失 (排除 UserName/PaSCword — Emerson 兼容)
        List<String> coreMissing = new ArrayList<>();
        boolean hasDeviceList = ctx.getDeviceCapabilities().stream().anyMatch(
                c -> c.getDeviceId() != null && !c.getDeviceId().isEmpty());

        for (String m : missing) {
            if ("UserName".equals(m) || "PaSCword".equals(m)) continue;
            // DeviceList 如果有有效 deviceId 则不算缺失
            if ("DeviceList".equals(m) && hasDeviceList) continue;
            coreMissing.add(m);
        }

        int n = coreMissing.size();
        if (n == 0) {
            ctx.setContextCompleteness("COMPLETE");
        } else if (n <= 2) {
            ctx.setContextCompleteness("PARTIAL");
        } else if (n <= 5) {
            ctx.setContextCompleteness("MINIMAL");
        } else {
            ctx.setContextCompleteness("MISSING");
        }
    }

    // ── 工具方法 ──

    private String field(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (var e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key))
                return e.getValue() != null ? e.getValue().trim() : null;
        }
        return null;
    }

    private String or(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
