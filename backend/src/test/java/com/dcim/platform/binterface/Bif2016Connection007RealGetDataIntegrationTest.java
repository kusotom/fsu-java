package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.fsu.FsuServiceRpcAdapter;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BIF2016-CONNECTION-007: corrected <Id> GET_DATA real readonly retest.
 *
 * <pre>
 * mvn test -Dtest='Bif2016Connection007RealGetDataIntegrationTest' -DrealFsuTest.enabled=true
 * </pre>
 *
 * Only GET_DATA is executed. No SET, scheduler, DB, or polling path is involved.
 */
@Tag("real-fsu")
@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")
class Bif2016Connection007RealGetDataIntegrationTest {

    private static final String DEFAULT_FSU_CODE = "51051243812345";
    private static final String DEFAULT_SERVICE_URL = "http://192.168.100.100:8080/services/FSUService";
    private static final Path RAW_DIR = Path.of("../..", "docs/landing/raw-samples").normalize();

    private final SoapMessageHandler soapMessageHandler = new SoapMessageHandler();
    private final FsuServiceRpcAdapter rpcAdapter = new FsuServiceRpcAdapter();

    @Test
    void realReadonlyGetDataRetestWithCorrectedIdRequestStructure() throws Exception {
        assumeTrue(Boolean.getBoolean("realFsuTest.enabled"),
                "真实 FSU 测试需显式启用: -DrealFsuTest.enabled=true");

        Files.createDirectories(RAW_DIR);
        String fsuCode = System.getProperty("realFsuTest.fsuCode", DEFAULT_FSU_CODE);
        String serviceUrl = System.getProperty("realFsuTest.serviceUrl", DEFAULT_SERVICE_URL);

        StrategyResult allDevices = invokeGetData(serviceUrl, "all-devices", allDevicesInfo(fsuCode));
        StrategyResult singleDevice = invokeGetData(serviceUrl, "single-device", singleDeviceInfo(fsuCode));
        StrategyResult minimal = invokeGetData(serviceUrl, "minimal", minimalWildcardInfo(fsuCode));

        List<StrategyResult> results = List.of(allDevices, singleDevice, minimal);
        for (StrategyResult result : results) {
            assertTrue(result.requestXml().contains("<Name>GET_DATA</Name>"));
            assertTrue(result.requestXml().contains("<Code>401</Code>"));
            assertTrue(result.requestXml().contains("<Id>"));
            assertFalse(result.requestXml().contains("<TSemaphore"),
                    result.strategy() + " request must not contain TSemaphore");

            System.out.println("CONNECTION-007 strategy=" + result.strategy()
                    + " http=" + result.httpStatus()
                    + " ack=" + result.ackName() + "/" + result.ackCode()
                    + " result=" + result.result()
                    + " deviceListPresent=" + result.deviceListPresent()
                    + " valuesReturned=" + result.valuesReturned()
                    + " emptyData=" + (result.valuesReturned() == 0)
                    + " error=" + result.errorMessage());
        }
        assertStrategySuccess(allDevices);
        assertStrategySuccess(singleDevice);
        if (minimal.httpStatus() == 200) {
            assertStrategySuccess(minimal);
        }
    }

    private StrategyResult invokeGetData(String serviceUrl, String strategy, String infoXml) throws Exception {
        String requestPayload = soapMessageHandler.buildRequest("GET_DATA", 401, infoXml, null);
        String rpcRequest = rpcAdapter.wrapRequestPayload(requestPayload);
        assertFalse(requestPayload.contains("<TSemaphore"), "GET_DATA request payload must use <Id>, not TSemaphore");

        String prefix = "bif2016-connection-007-get-data-" + strategy;
        Files.writeString(RAW_DIR.resolve(prefix + "-request-info.xml"), rpcRequest, StandardCharsets.UTF_8);

        HttpExchange http;
        try {
            http = postSoap(serviceUrl, rpcRequest);
        } catch (Exception e) {
            String errorBody = "<connection-error strategy=\"" + strategy + "\">"
                    + escape(e.getClass().getSimpleName() + ": " + e.getMessage())
                    + "</connection-error>\n";
            Files.writeString(RAW_DIR.resolve(prefix + "-response.xml"), errorBody, StandardCharsets.UTF_8);
            Files.writeString(RAW_DIR.resolve(prefix + "-unwrapped.xml"),
                    "NO_UNWRAPPED_XML_DATA: " + e.getClass().getSimpleName() + ": " + e.getMessage() + "\n",
                    StandardCharsets.UTF_8);
            return new StrategyResult(strategy, 0, requestPayload, null,
                    null, -1, null, false, 0, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Files.writeString(RAW_DIR.resolve(prefix + "-response.xml"), http.body(), StandardCharsets.UTF_8);

        String unwrapped = rpcAdapter.unwrapResponsePayload(http.body());
        Files.writeString(RAW_DIR.resolve(prefix + "-unwrapped.xml"), unwrapped, StandardCharsets.UTF_8);

        Document doc = BInterface2016StandardTestSupport.parse(unwrapped);
        String ackName = BInterface2016StandardTestSupport.text(doc, "Name");
        int ackCode = BInterface2016StandardTestSupport.intText(doc, "Code");
        String result = BInterface2016StandardTestSupport.text(doc, "Result");
        boolean deviceListPresent = BInterface2016StandardTestSupport.hasTag(unwrapped, "DeviceList");
        int valuesReturned = countTag(unwrapped, "TSemaphore");

        return new StrategyResult(strategy, http.status(), requestPayload, unwrapped,
                ackName, ackCode, result, deviceListPresent, valuesReturned, null);
    }

    private void assertStrategySuccess(StrategyResult result) {
        assertEquals(200, result.httpStatus(), result.strategy() + " HTTP must be 200");
        assertEquals("GET_DATA_ACK", result.ackName(), result.strategy() + " ACK name");
        assertEquals(402, result.ackCode(), result.strategy() + " ACK code");
        assertEquals("1", result.result(), result.strategy() + " Result=1 means SUCCESS in Binterface 2016");
    }

    private HttpExchange postSoap(String url, String soapXml) throws Exception {
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
            return new HttpExchange(status, response);
        } finally {
            conn.disconnect();
        }
    }

    private String allDevicesInfo(String fsuCode) {
        return "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\"><Id>0418002001</Id></Device>"
                + "<Device Id=\"51051241830004\" Code=\"51051241830004\">"
                + "<Id>0418004001</Id><Id>0418007001</Id><Id>0418101001</Id><Id>0418102001</Id>"
                + "</Device>"
                + "<Device Id=\"51051241840004\" Code=\"51051241840004\"><Id>0418001001</Id></Device>"
                + "<Device Id=\"51051240700002\" Code=\"51051240700002\">"
                + "<Id>0407102001</Id><Id>0407103001</Id><Id>0407107001</Id>"
                + "</Device>"
                + "</DeviceList>";
    }

    private String singleDeviceInfo(String fsuCode) {
        return "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList><Device Id=\"51051240700002\" Code=\"51051240700002\">"
                + "<Id>0407102001</Id><Id>0407107001</Id>"
                + "</Device></DeviceList>";
    }

    private String minimalWildcardInfo(String fsuCode) {
        return "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList><Device Id=\"99999999999999\" Code=\"99999999999999\">"
                + "<Id>9999999999</Id>"
                + "</Device></DeviceList>";
    }

    private int countTag(String xml, String tagName) {
        return BInterface2016StandardTestSupport.parse(xml)
                .getElementsByTagName(tagName).getLength();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record HttpExchange(int status, String body) {
    }

    private record StrategyResult(String strategy, int httpStatus, String requestXml, String unwrappedXml,
                                  String ackName, int ackCode, String result,
                                  boolean deviceListPresent, int valuesReturned, String errorMessage) {
    }
}
