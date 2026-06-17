package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.fsu.FsuServiceRpcAdapter;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * SPEC-TEST-P0-001: B接口2016 真实 FSU 只读 run-once 测试。
 *
 * <p>默认跳过，必须显式启用：</p>
 * <pre>
 * mvn test -Dtest='BInterface2016ReadOnlyRealFsuRunOnceTest' -DrealFsuTest.enabled=true
 * </pre>
 *
 * <p>边界：只执行只读 GET 类命令；不执行 SET；不启动 Scheduler；不写业务数据库。</p>
 */
@Tag("real-fsu")
@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")
@TestMethodOrder(MethodOrderer.MethodName.class)
class BInterface2016ReadOnlyRealFsuRunOnceTest {

    private static final String DEFAULT_FSU_CODE = "51051243812345";
    private static final String DEFAULT_DEVICE_ID = "51051241820004";
    private static final String DEFAULT_SIGNAL_ID = "0118001001";
    private static final String DEFAULT_SERVICE_URL = "http://192.168.100.100:8080/services/FSUService";
    private static final Path RAW_DIR = Path.of("docs/landing/raw-samples");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SoapMessageHandler soapMessageHandler = new SoapMessageHandler();
    private final FsuServiceRpcAdapter rpcAdapter = new FsuServiceRpcAdapter();

    private String fsuCode;
    private String deviceId;
    private String signalId;
    private String serviceUrl;

    @BeforeEach
    void enableOnlyWhenExplicitlyRequested() throws Exception {
        assumeTrue(Boolean.getBoolean("realFsuTest.enabled"),
                "真实 FSU 测试需显式启用: -DrealFsuTest.enabled=true");

        fsuCode = System.getProperty("realFsuTest.fsuCode", DEFAULT_FSU_CODE);
        deviceId = System.getProperty("realFsuTest.deviceId", DEFAULT_DEVICE_ID);
        signalId = System.getProperty("realFsuTest.signalId", DEFAULT_SIGNAL_ID);
        serviceUrl = System.getProperty("realFsuTest.serviceUrl", DEFAULT_SERVICE_URL);

        assertTrue(serviceUrl.startsWith("http://") || serviceUrl.startsWith("https://"),
                "realFsuTest.serviceUrl 必须是 HTTP(S) 地址");
        Files.createDirectories(RAW_DIR);
    }

    @Test
    void getData_readOnly_emptyDeviceListIsAckSuccess() throws Exception {
        String infoXml = "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList><Device Id=\"" + escape(deviceId)
                + "\" Code=\"" + escape(deviceId) + "\"/></DeviceList>";

        RealFsuExchange exchange = invoke2016("GET_DATA", 401, infoXml);

        assertAck(exchange.unwrappedResponse(), "GET_DATA_ACK", 402);
        assertEquals("1", BInterface2016StandardTestSupport.text(
                BInterface2016StandardTestSupport.parse(exchange.unwrappedResponse()), "Result"),
                "B接口2016 GET_DATA_ACK Result=1 才表示 ACK 成功；空 DeviceList 不能判为协议失败");
    }

    @Test
    void getData_readOnly_knownSensorStatus() throws Exception {
        String infoXml = "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\"><Id>418002001</Id></Device>"
                + "<Device Id=\"51051241830004\" Code=\"51051241830004\">"
                + "<Id>418004001</Id><Id>418007001</Id><Id>418101001</Id><Id>418102001</Id>"
                + "</Device>"
                + "<Device Id=\"51051241840004\" Code=\"51051241840004\"><Id>418001001</Id></Device>"
                + "<Device Id=\"51051240700002\" Code=\"51051240700002\">"
                + "<Id>407102001</Id><Id>407103001</Id><Id>407107001</Id>"
                + "</Device>"
                + "</DeviceList>";

        RealFsuExchange exchange = invoke2016("GET_DATA_SENSOR_STATUS", "GET_DATA", 401, infoXml);

        assertAck(exchange.unwrappedResponse(), "GET_DATA_ACK", 402);
        assertEquals("1", BInterface2016StandardTestSupport.text(
                BInterface2016StandardTestSupport.parse(exchange.unwrappedResponse()), "Result"));
        assertTrue(BInterface2016StandardTestSupport.hasTag(exchange.unwrappedResponse(), "TSemaphore"),
                "传感器状态响应应包含 TSemaphore");
        assertTrue(exchange.unwrappedResponse().contains("MeasuredVal="),
                "传感器状态响应应包含 MeasuredVal");
    }

    @Test
    void getLoginInfo_readOnly() throws Exception {
        RealFsuExchange exchange = invoke2016("GET_LOGININFO", 1501,
                "<FSUCode>" + escape(fsuCode) + "</FSUCode>");

        assertAck(exchange.unwrappedResponse(), "GET_LOGININFO_ACK", 1502);
        if (!BInterface2016StandardTestSupport.hasTag(exchange.unwrappedResponse(), "Result")) {
            System.out.println("observed real-fsu profile difference: GET_LOGININFO_ACK has no Result field");
        } else {
            assertResultPresent("GET_LOGININFO", exchange.unwrappedResponse());
        }
        assertTrue(BInterface2016StandardTestSupport.hasTag(exchange.unwrappedResponse(), "DeviceList"),
                "真实 FSU GET_LOGININFO_ACK 应返回 DeviceList 能力清单");
    }

    @Test
    void getFtp_readOnly() throws Exception {
        RealFsuExchange exchange = invoke2016("GET_FTP", 1601,
                "<FSUCode>" + escape(fsuCode) + "</FSUCode>");

        assertAck(exchange.unwrappedResponse(), "GET_FTP_ACK", 1602);
        assertResultPresent("GET_FTP", exchange.unwrappedResponse());
    }

    @Test
    void getFsuInfo_readOnly_statusQuery() throws Exception {
        RealFsuExchange exchange = invoke2016("GET_FSUINFO", 1701,
                "<FSUCode>" + escape(fsuCode) + "</FSUCode>");

        assertAck(exchange.unwrappedResponse(), "GET_FSUINFO_ACK", 1702);
        String result = BInterface2016StandardTestSupport.text(
                BInterface2016StandardTestSupport.parse(exchange.unwrappedResponse()), "Result");
        assertTrue(result != null && !result.isBlank(),
                "真实 FSU GET_FSUINFO_ACK 必须返回 Result 字段");
        if (!"1".equals(result)) {
            System.out.println("observed real-fsu profile difference: GET_FSUINFO_ACK Result="
                    + result + " while standard-2016 SUCCESS=1");
        }
        assertTrue(BInterface2016StandardTestSupport.hasTag(exchange.unwrappedResponse(), "CPUUsage")
                        || BInterface2016StandardTestSupport.hasTag(exchange.unwrappedResponse(), "MEMUsage"),
                "真实 FSU GET_FSUINFO 响应应至少包含 CPUUsage 或 MEMUsage");
    }

    @Test
    void getThreshold_readOnly() throws Exception {
        String infoXml = "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList><Device Id=\"" + escape(deviceId)
                + "\" Code=\"" + escape(deviceId) + "\"><Id>"
                + escape(signalId) + "</Id></Device></DeviceList>";

        RealFsuExchange exchange = invoke2016("GET_THRESHOLD", 1901, infoXml);

        assertAck(exchange.unwrappedResponse(), "GET_THRESHOLD_ACK", 1902);
        assertResultPresent("GET_THRESHOLD", exchange.unwrappedResponse());
    }

    private RealFsuExchange invoke2016(String command, int code, String infoXml) throws Exception {
        return invoke2016(command, command, code, infoXml);
    }

    private RealFsuExchange invoke2016(String fileCommand, String protocolCommand,
                                       int code, String infoXml) throws Exception {
        String requestPayload = soapMessageHandler.buildRequest(protocolCommand, code, infoXml, null);
        String rpcRequest = rpcAdapter.wrapRequestPayload(requestPayload);
        String timestamp = LocalDateTime.now().format(TS);

        saveRaw(fileCommand, timestamp, "request.xml", rpcRequest);
        saveRaw(fileCommand, timestamp, "request-payload.xml", requestPayload);

        String rpcResponse = postSoap(serviceUrl, rpcRequest);
        saveRaw(fileCommand, timestamp, "response.xml", rpcResponse);

        String unwrapped = rpcAdapter.unwrapResponsePayload(rpcResponse);
        saveRaw(fileCommand, timestamp, "response-unwrapped.xml", unwrapped);

        System.out.println("real-fsu command=" + protocolCommand
                + " fsuCode=" + fsuCode
                + " serviceUrl=" + serviceUrl
                + " timestamp=" + timestamp);
        return new RealFsuExchange(rpcRequest, rpcResponse, unwrapped);
    }

    private String postSoap(String url, String soapXml) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(10_000);
            conn.setDoOutput(true);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(soapXml.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            InputStream body = status >= 200 && status < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();
            String response = body == null ? "" : new String(body.readAllBytes(), StandardCharsets.UTF_8);
            if (status != HttpURLConnection.HTTP_OK) {
                throw new AssertionError("FSU HTTP 状态异常: " + status + "\n" + response);
            }
            return response;
        } finally {
            conn.disconnect();
        }
    }

    private void assertAck(String soap, String expectedName, int expectedCode) {
        var doc = BInterface2016StandardTestSupport.parse(soap);
        assertEquals(expectedName, BInterface2016StandardTestSupport.text(doc, "Name"));
        assertEquals(expectedCode, BInterface2016StandardTestSupport.intText(doc, "Code"));
    }

    private void assertResultPresent(String command, String soap) {
        String result = BInterface2016StandardTestSupport.text(
                BInterface2016StandardTestSupport.parse(soap), "Result");
        assertTrue(result != null && !result.isBlank(),
                "真实 FSU " + command + "_ACK 必须返回 Result 字段");
        System.out.println("real-fsu command=" + command + " observed Result=" + result);
    }

    private void saveRaw(String command, String timestamp, String suffix, String content) throws Exception {
        String safeFsuCode = fsuCode.replaceAll("[^A-Za-z0-9_-]", "_");
        String fileName = "SPEC-TEST-P0-001-" + command + "-" + safeFsuCode
                + "-" + timestamp + "-" + suffix;
        Files.writeString(RAW_DIR.resolve(fileName), content, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record RealFsuExchange(String request, String response, String unwrappedResponse) {
    }
}
