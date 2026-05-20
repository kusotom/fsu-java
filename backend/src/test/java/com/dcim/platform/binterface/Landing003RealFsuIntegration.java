package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceCommand2024;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.compat.BInterfaceCommand2016;
import com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper;
import com.dcim.platform.module.binterface.service.GetActiveAlarmResult;
import com.dcim.platform.module.binterface.service.GetActiveAlarmService;
import com.dcim.platform.module.binterface.service.GetDataResult;
import com.dcim.platform.module.binterface.service.GetDataService;
import com.dcim.platform.module.binterface.service.GetLoginInfoResult;
import com.dcim.platform.module.binterface.service.GetLoginInfoService;
import com.dcim.platform.module.binterface.service.GetSpConfigOptionResult;
import com.dcim.platform.module.binterface.service.GetSpConfigOptionService;
import com.dcim.platform.module.binterface.service.GetSuInfoResult;
import com.dcim.platform.module.binterface.service.GetSuInfoService;
import com.dcim.platform.module.binterface.service.GetThresholdResult;
import com.dcim.platform.module.binterface.service.GetThresholdService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRpcAdapter;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LANDING-003/004: 真实 FSU 只读联调采集。
 *
 * <p>默认禁用，需显式启用: -DrealFsuTest.enabled=true</p>
 *
 * <h3>安全边界</h3>
 * <ul>
 *   <li>仅调用只读命令</li>
 *   <li>严禁任何 SET_ 命令</li>
 *   <li>不启用 Scheduler</li>
 *   <li>不修改 alarm_record 状态</li>
 * </ul>
 */
@Tag("real-fsu")
@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")
public class Landing003RealFsuIntegration {

    /** 真实 FSUID (管理方确认: 51051243812345, StationName=1) */
    private static final String FSUID = "51051243812345";
    private static final String PLATFORM_FSU_CODE = "FSU-001";
    private static final String SERVICE_URL = "http://192.168.100.100:8080/services/FSUService";

    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath();
    private static final Path RAW_SAMPLES_DIR = PROJECT_ROOT.resolve("docs/landing/raw-samples");
    private static final Path REPORT_PATH = PROJECT_ROOT.resolve("docs/landing/REAL-FSU-READONLY-CALL-ANALYSIS.md");

    private static final SoapMessageHandler soapHandler = new SoapMessageHandler();
    private static final XmlDataParser xmlDataParser = new XmlDataParser();
    private static final FsuServiceRpcAdapter rpcAdapter = new FsuServiceRpcAdapter();
    private static final CaptureFsuServiceClient client =
            new CaptureFsuServiceClient(soapHandler, xmlDataParser, rpcAdapter, 5000, 10000);

    // 采集状态
    private static String discoveredSuid = null;
    private static final List<String> discoveredSignalIds = new ArrayList<>();
    private static final List<String> discoveredDeviceIds = new ArrayList<>();
    private static final Map<String, String> structuredResults = new LinkedHashMap<>();
    private static final Map<String, String> legacyResults = new LinkedHashMap<>();
    private static final Map<String, String> legacy2016Results = new LinkedHashMap<>();

    @Test
    void executeLanding004() throws Exception {
        main(null);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("\n============================================================");
        System.out.println("  LANDING-006: B接口2016码表只读联调");
        System.out.println("  目标: " + SERVICE_URL);
        System.out.println("  FSUID: " + FSUID + "  StationName: 1");
        System.out.println("  时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("============================================================\n");

        Files.createDirectories(RAW_SAMPLES_DIR);

        // Phase 1: structured (2024 Name+Code)
        System.out.println("\n########## PHASE 1: 2024 Code (structured) ##########\n");
        runCommand("GET_DATA",       BInterfacePkType.GET_DATA,       "<FSUCode>" + FSUID + "</FSUCode>", signalXml(), "structured", structuredResults);
        runCommand("GET_LOGININFO",  BInterfacePkType.GET_LOGININFO,  "<FSUCode>" + FSUID + "</FSUCode>", null, "structured", structuredResults);
        runCommand("GET_FTP",        BInterfacePkType.GET_FTP,        "<FSUCode>" + FSUID + "</FSUCode>", null, "structured", structuredResults);
        runCommand("GET_FSUINFO",    BInterfacePkType.GET_FSUINFO,    "<FSUCode>" + FSUID + "</FSUCode>", null, "structured", structuredResults);

        // Phase 2: legacy-text (plain text, no codes)
        System.out.println("\n########## PHASE 2: Legacy Text (plain PK_Type) ##########\n");
        runCommand("GET_DATA",       BInterfacePkType.GET_DATA,       "<FSUCode>" + FSUID + "</FSUCode>", signalXml(), "legacy-text", legacyResults);
        runCommand("GET_LOGININFO",  BInterfacePkType.GET_LOGININFO,  "<FSUCode>" + FSUID + "</FSUCode>", null, "legacy-text", legacyResults);
        runCommand("GET_FTP",        BInterfacePkType.GET_FTP,        "<FSUCode>" + FSUID + "</FSUCode>", null, "legacy-text", legacyResults);
        runCommand("GET_FSUINFO",    BInterfacePkType.GET_FSUINFO,    "<FSUCode>" + FSUID + "</FSUCode>", null, "legacy-text", legacyResults);

        // Phase 3: legacy-2016 (Name+Code with B-2016 codes)
        System.out.println("\n########## PHASE 3: B-2016 Code (legacy-2016) ##########\n");
        runCommand("GET_DATA",       BInterfacePkType.GET_DATA,       "<FSUCode>" + FSUID + "</FSUCode>", signalXml(), "legacy-2016", legacy2016Results);
        runCommand("GET_LOGININFO",  BInterfacePkType.GET_LOGININFO,  "<FSUCode>" + FSUID + "</FSUCode>", null, "legacy-2016", legacy2016Results);
        runCommand("GET_FTP",        BInterfacePkType.GET_FTP,        "<FSUCode>" + FSUID + "</FSUCode>", null, "legacy-2016", legacy2016Results);
        runCommand("GET_FSUINFO",    BInterfacePkType.GET_FSUINFO,    "<FSUCode>" + FSUID + "</FSUCode>", null, "legacy-2016", legacy2016Results);
        System.out.println("  目标: " + SERVICE_URL);
        System.out.println("  FSUID: " + FSUID + "  StationName: 1  平台临时 fsuCode: " + PLATFORM_FSU_CODE);
        System.out.println("  时间: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("============================================================\n");

        Files.createDirectories(RAW_SAMPLES_DIR);

        // Phase 1: structured (2024 Name+Code) 格式
        System.out.println("\n########## PHASE 1: STRUCTURED (Name+Code) + FSUID=" + FSUID + " ##########\n");
        runCommand("GET_SUINFO",     BInterfacePkType.GET_SUINFO,     "<SUID>" + FSUID + "</SUID>", null, "structured", structuredResults);
        runCommand("GET_DATA",       BInterfacePkType.GET_DATA,       "<FSUCode>" + FSUID + "</FSUCode>", signalXml(), "structured", structuredResults);
        runCommand("GET_ACTIVEALARM", BInterfacePkType.GET_ACTIVEALARM, "<SUID>" + FSUID + "</SUID>", null, "structured", structuredResults);
        runCommand("GET_SPCONFIGOPTION", BInterfacePkType.GET_SPCONFIGOPTION, "<SUID>" + FSUID + "</SUID>", null, "structured", structuredResults);
        runCommand("GET_THRESHOLD",  BInterfacePkType.GET_THRESHOLD,  "<FSUCode>" + FSUID + "</FSUCode>", signalXml(), "structured", structuredResults);

        // Phase 2: legacy-text (2016 纯文本) 格式
        System.out.println("\n########## PHASE 2: LEGACY (plain text PK_Type) + FSUID=" + FSUID + " ##########\n");
        runCommand("GET_SUINFO",     BInterfacePkType.GET_SUINFO,     "<SUID>" + FSUID + "</SUID>", null, "legacy-text", legacyResults);
        runCommand("GET_DATA",       BInterfacePkType.GET_DATA,       "<FSUCode>" + FSUID + "</FSUCode>", signalXml(), "legacy-text", legacyResults);
        runCommand("GET_ACTIVEALARM", BInterfacePkType.GET_ACTIVEALARM, "<SUID>" + FSUID + "</SUID>", null, "legacy-text", legacyResults);
        runCommand("GET_SPCONFIGOPTION", BInterfacePkType.GET_SPCONFIGOPTION, "<SUID>" + FSUID + "</SUID>", null, "legacy-text", legacyResults);
        runCommand("GET_THRESHOLD",  BInterfacePkType.GET_THRESHOLD,  "<FSUCode>" + FSUID + "</FSUCode>", signalXml(), "legacy-text", legacyResults);

        generateComparisonReport();

        System.out.println("\n============================================================");
        System.out.println("  LANDING-004 执行完成");
        System.out.println("  原始报文: " + RAW_SAMPLES_DIR);
        System.out.println("  分析报告: " + REPORT_PATH);
        System.out.println("============================================================");
    }

    static void runCommand(String name, BInterfacePkType pkType, String infoXml,
                           String xmlDataXml, String format, Map<String, String> results) throws Exception {
        System.out.println("---------- " + name + " (" + format + ") ----------");

        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode(FSUID)
                .serviceUrl(SERVICE_URL)
                .pkType(pkType)
                .infoXml(infoXml)
                .xmlDataXml(xmlDataXml)
                .pkTypeFormat(format)
                .build();

        FsuServiceResponse response = client.call(request);

        String reqPayload = client.getLastRequestPayload();
        String rpcReq = client.getLastRpcRequest();
        String rpcResp = client.getLastRpcResponse();
        String docResp = client.getLastDocResponse();

        // 打印
        System.out.println("  success: " + response.isSuccess());
        System.out.println("  resultCode: " + response.getResultCode());
        System.out.println("  resultDesc: " + response.getResultDesc());
        System.out.println("  realCall: " + response.isRealCall());
        System.out.println("  PK_Type format: " + format);

        // 打印响应关键字段
        if (reqPayload != null) {
            System.out.println("  Request PK_Type snippet: "
                    + reqPayload.replaceAll("\\s+", " ").substring(0, Math.min(200, reqPayload.replaceAll("\\s+", " ").length())));
        }
        if (client.getLastXmlData() != null && !client.getLastXmlData().isEmpty()) {
            List<Map<String, String>> items = client.getLastXmlData().getItems();
            if (items != null) System.out.println("  Items count: " + items.size());
        }

        // 保存
        String prefix = "landing006-" + format + "-" + name.toLowerCase().replace("_", "");
        writeIfNotNull(RAW_SAMPLES_DIR.resolve(prefix + "-request-payload.xml"), reqPayload);
        writeIfNotNull(RAW_SAMPLES_DIR.resolve(prefix + "-rpc-request.xml"), rpcReq);
        writeIfNotNull(RAW_SAMPLES_DIR.resolve(prefix + "-rpc-response.xml"), rpcResp);
        writeIfNotNull(RAW_SAMPLES_DIR.resolve(prefix + "-doc-response.xml"), docResp);

        // 记录结果
        String resultEntry = response.isSuccess() ? "SUCCESS" : "FAILED: " + response.getResultCode() + " " + response.getResultDesc();
        if (client.getLastXmlData() != null && !client.getLastXmlData().isEmpty()) {
            List<Map<String, String>> items = client.getLastXmlData().getItems();
            if (items != null) resultEntry += " (" + items.size() + " items)";
        }
        results.put(name, resultEntry);
    }

    static String signalXml() {
        if (discoveredSignalIds.isEmpty()) {
            return "<SignalID>TEMP-R01</SignalID><SignalID>HUMI-R01</SignalID>";
        }
        StringBuilder sb = new StringBuilder();
        for (String id : discoveredSignalIds) {
            sb.append("<SignalID>").append(id).append("</SignalID>");
        }
        return sb.toString();
    }

    static void generateComparisonReport() throws Exception {
        System.out.println("\n---------- 生成对比报告 ----------");

        StringBuilder sb = new StringBuilder();
        sb.append("# LANDING-005 执行报告 (更新)\n\n");
        sb.append("> 更新时间: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        sb.append("> **真实 FSUID**: `51051243812345` (管理方确认, StationName=1)\n");
        sb.append("> **平台临时 fsuCode**: `FSU-001` (仅用于平台内部标识)\n\n");

        sb.append("## LANDING-005: Structured vs Legacy + 真实 FSUID 对比\n\n");

        sb.append("### Structured (Name+Code) 结果\n\n");
        sb.append("| 命令 | 结果 |\n");
        sb.append("|------|------|\n");
        for (Map.Entry<String, String> e : structuredResults.entrySet()) {
            sb.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n");
        }
        sb.append("\n");

        sb.append("### Legacy (纯文本 PK_Type) 结果\n\n");
        sb.append("| 命令 | 结果 |\n");
        sb.append("|------|------|\n");
        for (Map.Entry<String, String> e : legacyResults.entrySet()) {
            sb.append("| ").append(e.getKey()).append(" | ").append(e.getValue()).append(" |\n");
        }
        sb.append("\n");

        sb.append("### 格式对比分析\n\n");
        sb.append("| 命令 | Structured | Legacy | 差异 |\n");
        sb.append("|------|-----------|--------|------|\n");
        for (String cmd : structuredResults.keySet()) {
            String s = structuredResults.get(cmd);
            String l = legacyResults.getOrDefault(cmd, "N/A");
            String diff = s != null && l != null && s.equals(l) ? "一致" : "不同";
            sb.append("| ").append(cmd).append(" | ").append(s).append(" | ").append(l).append(" | ").append(diff).append(" |\n");
        }
        sb.append("\n");

        sb.append("### 与 LANDING-004 (FSU-001) 对比\n\n");
        sb.append("| 命令 | LANDING-004 (FSU-001) | LANDING-005 (").append(FSUID).append(") | 差异 |\n");
        sb.append("|------|------------------------|------------------------------|------|\n");
        sb.append("| GET_SUINFO | 空响应 | ").append(structuredResults.getOrDefault("GET_SUINFO", "N/A")).append(" | 待对比 |\n");
        sb.append("| GET_DATA | 空/SEND_ALARM | ").append(structuredResults.getOrDefault("GET_DATA", "N/A")).append(" | 待对比 |\n");
        sb.append("| GET_ACTIVEALARM | 空响应 | ").append(structuredResults.getOrDefault("GET_ACTIVEALARM", "N/A")).append(" | 待对比 |\n");
        sb.append("| GET_SPCONFIGOPTION | 空响应 | ").append(structuredResults.getOrDefault("GET_SPCONFIGOPTION", "N/A")).append(" | 待对比 |\n");
        sb.append("| GET_THRESHOLD | 空响应 | ").append(structuredResults.getOrDefault("GET_THRESHOLD", "N/A")).append(" | 待对比 |\n");
        sb.append("\n");
        sb.append("> **注意**: LANDING-004 使用临时 fsuCode `FSU-001`，LANDING-005 使用真实 FSUID `").append(FSUID).append("`。\n");
        sb.append("> 如果结果有差异，说明 FSU 对 SUID/FSUID 有校验。\n\n");

        sb.append("### 结论\n\n");
        boolean structuredAnySuccess = structuredResults.values().stream().anyMatch(v -> v.startsWith("SUCCESS"));
        boolean legacyAnySuccess = legacyResults.values().stream().anyMatch(v -> v.startsWith("SUCCESS"));
        if (legacyAnySuccess && !structuredAnySuccess) {
            sb.append("**Legacy 格式更接近设备实际支持。** 建议后续使用 legacy-text 格式进行联调。\n\n");
        } else if (structuredAnySuccess && !legacyAnySuccess) {
            sb.append("**Structured 格式更接近设备实际支持。**\n\n");
        } else {
            sb.append("两种格式表现相同或各有差异，需进一步分析。\n\n");
        }

        sb.append("### 安全边界\n\n");
        sb.append("- [x] 未执行任何 SET_ 命令\n");
        sb.append("- [x] 未启用 Scheduler\n");
        sb.append("- [x] 未修改 alarm_record 状态\n");
        sb.append("- [x] 仅调用只读命令\n");

        // 追加到现有报告
        Path reportPath = PROJECT_ROOT.resolve("docs/landing/REAL-FSU-READONLY-CALL-ANALYSIS.md");
        String existing = Files.exists(reportPath) ? Files.readString(reportPath) : "";
        Files.writeString(reportPath, existing + "\n---\n\n" + sb.toString());
        System.out.println("  对比报告已追加到: " + reportPath);
    }

    // ==================== 辅助 ====================

    static void writeIfNotNull(Path path, String content) throws Exception {
        if (content != null && !content.isEmpty()) {
            Files.writeString(path, content);
            System.out.println("  已保存: " + path.getFileName());
        }
    }

    // ==================== CaptureFsuServiceClient ====================

    static class CaptureFsuServiceClient implements FsuServiceClient {
        private static final String SOAP_CONTENT_TYPE = "text/xml; charset=utf-8";
        private static final String SOAP_ACTION = "";

        private final SoapMessageHandler soapMessageHandler;
        private final XmlDataParser xmlDataParser;
        private final FsuServiceRpcAdapter rpcAdapter;
        private final int connectTimeoutMs;
        private final int readTimeoutMs;

        private String lastRequestPayload;
        private String lastRpcRequest;
        private String lastRpcResponse;
        private String lastDocResponse;
        private XmlDataModel lastXmlData;

        CaptureFsuServiceClient(SoapMessageHandler smh, XmlDataParser xdp,
                                FsuServiceRpcAdapter rpa, int ct, int rt) {
            this.soapMessageHandler = smh;
            this.xmlDataParser = xdp;
            this.rpcAdapter = rpa;
            this.connectTimeoutMs = ct;
            this.readTimeoutMs = rt;
        }

        @Override
        public FsuServiceResponse call(FsuServiceRequest request) {
            lastRequestPayload = null; lastRpcRequest = null;
            lastRpcResponse = null; lastDocResponse = null; lastXmlData = null;

            if (request == null) return FsuServiceResponse.error("请求参数为空");
            if (request.getServiceUrl() == null || request.getServiceUrl().trim().isEmpty())
                return FsuServiceResponse.fail("2001", "FSU 服务地址为空");

            System.out.println("  [HTTP] " + request.getPkType() + " (" + request.getPkTypeFormat() + ") → " + request.getServiceUrl());

            try {
                String pkTypeName = request.getPkType().name();
                boolean forceLegacyText = "legacy-text".equals(request.getPkTypeFormat());
                boolean forceLegacy2016 = "legacy-2016".equals(request.getPkTypeFormat());

                if (forceLegacyText) {
                    lastRequestPayload = soapMessageHandler.buildRequest(pkTypeName,
                            request.getInfoXml(), request.getXmlDataXml());
                } else if (forceLegacy2016) {
                    Integer code2016 = BInterfaceCommand2016.codeFor(pkTypeName).orElse(null);
                    lastRequestPayload = soapMessageHandler.buildRequest(pkTypeName, code2016,
                            request.getInfoXml(), request.getXmlDataXml());
                } else {
                    BInterfaceCommand2024 cmd2024 = BInterfaceCommandAliasMapper.to2024(request.getPkType()).orElse(null);
                    if (cmd2024 != null) {
                        lastRequestPayload = soapMessageHandler.buildRequest(cmd2024.getName(), cmd2024.getCode(),
                                request.getInfoXml(), request.getXmlDataXml());
                    } else {
                        lastRequestPayload = soapMessageHandler.buildRequest(pkTypeName,
                                request.getInfoXml(), request.getXmlDataXml());
                    }
                }

                lastRpcRequest = rpcAdapter.wrapRequestPayload(lastRequestPayload);
                lastRpcResponse = doHttpPost(request.getServiceUrl(), lastRpcRequest);
                lastDocResponse = rpcAdapter.unwrapResponsePayload(lastRpcResponse);

                BInterfaceMessage message = soapMessageHandler.parse(lastDocResponse);
                String infoXml = message.getInfo();
                String rawXmlData = message.getXmlData();
                lastXmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

                String resultCode = "0";
                if (infoXml != null && xmlDataParser.parseFields(infoXml).containsKey("ResultCode")) {
                    resultCode = xmlDataParser.parseFields(infoXml).get("ResultCode");
                }

                if ("0".equals(resultCode)) {
                    return FsuServiceResponse.successReal(lastDocResponse, infoXml, rawXmlData, lastXmlData);
                } else {
                    return FsuServiceResponse.fail(resultCode, "FSU 返回错误: " + resultCode);
                }
            } catch (java.net.ConnectException e) {
                System.err.println("  [ERROR] 连接失败: " + e.getMessage());
                return FsuServiceResponse.fail("5001", "FSU 连接失败: " + e.getMessage());
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("  [ERROR] 请求超时: " + e.getMessage());
                return FsuServiceResponse.fail("5001", "FSU 请求超时: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("  [ERROR] HTTP 调用异常: " + e.getMessage());
                return FsuServiceResponse.error("FSU HTTP 调用失败: " + e.getMessage());
            }
        }

        private String doHttpPost(String urlStr, String soapXml) throws Exception {
            URL url = URI.create(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            try {
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", SOAP_CONTENT_TYPE);
                conn.setRequestProperty("SOAPAction", SOAP_ACTION);
                conn.setConnectTimeout(connectTimeoutMs);
                conn.setReadTimeout(readTimeoutMs);
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = soapXml.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                } else {
                    String errorBody = new String(
                            conn.getErrorStream() != null
                                    ? conn.getErrorStream().readAllBytes()
                                    : new byte[0],
                            StandardCharsets.UTF_8);
                    throw new RuntimeException("HTTP " + responseCode + ": " + errorBody);
                }
            } finally {
                conn.disconnect();
            }
        }

        String getLastRequestPayload() { return lastRequestPayload; }
        String getLastRpcRequest() { return lastRpcRequest; }
        String getLastRpcResponse() { return lastRpcResponse; }
        String getLastDocResponse() { return lastDocResponse; }
        XmlDataModel getLastXmlData() { return lastXmlData; }
    }
}
