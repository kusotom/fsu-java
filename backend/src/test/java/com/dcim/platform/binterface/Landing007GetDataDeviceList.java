package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.compat.BInterfaceCommand2016;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRpcAdapter;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
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

/**
 * LANDING-007: 基于 B接口2016 DeviceID 的 GET_DATA 真实点位查询。
 *
 * <p>使用从 GET_LOGININFO(1501) 获取的 4 个真实 DeviceID，
 * 按 B接口2016 协议 DeviceList 格式构造 GET_DATA(401) 请求。</p>
 */
@Tag("real-fsu")
@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")
public class Landing007GetDataDeviceList {

    private static final String FSUID = "51051243812345";
    private static final String SERVICE_URL = "http://192.168.100.100:8080/services/FSUService";
    private static final Path RAW_DIR = Paths.get("docs/landing/raw-samples");
    private static final Path CANDIDATE_DIR = Paths.get("docs/landing/candidate-points");

    // 4 real DeviceIDs from GET_LOGININFO (1501)
    private static final String[] DEVICE_IDS = {
        "51051241820004", "51051241830004", "51051241840004", "51051240700002"
    };

    // TSemaphore IDs from Python probe logs (known signal IDs for these devices)
    private static final String[][] TSEMAPHORE_IDS = {
        {"418002001"},                                          // Device 1
        {"418004001", "418007001", "418101001", "418102001"},   // Device 2
        {"418001001"},                                          // Device 3
        {"407102001", "407103001", "407107001"}                 // Device 4
    };

    @Test
    void executeLanding007() throws Exception {
        main(null);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("\n============================================================");
        System.out.println("  LANDING-007: GET_DATA with DeviceList (B-2016 Code=401)");
        System.out.println("  FSUID: " + FSUID);
        System.out.println("  Devices: " + String.join(", ", DEVICE_IDS));
        System.out.println("  Time: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        System.out.println("============================================================\n");

        Files.createDirectories(RAW_DIR);
        Files.createDirectories(CANDIDATE_DIR);

        SoapMessageHandler soap = new SoapMessageHandler();
        XmlDataParser xmlParser = new XmlDataParser();
        FsuServiceRpcAdapter rpc = new FsuServiceRpcAdapter();

        // Test 1: DeviceList only (query all signals for all devices)
        System.out.println("---------- Test 1: DeviceList (all devices, no TSemaphore) ----------");
        String info1 = buildDeviceListInfo(false);
        executeGetData(soap, rpc, info1, "landing007-devicelist-all");

        // Test 2: DeviceList with TSemaphore (known signal IDs)
        System.out.println("\n---------- Test 2: DeviceList + TSemaphore (known signal IDs) ----------");
        String info2 = buildDeviceListInfo(true);
        executeGetData(soap, rpc, info2, "landing007-devicelist-tsemaphore");

        // Test 3: Single device query (first device only, all signals)
        System.out.println("\n---------- Test 3: Single Device (51051241820004, all signals) ----------");
        String info3 = "<FsuId>" + FSUID + "</FsuId>"
                + "<FsuCode>" + FSUID + "</FsuCode>"
                + "<DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\"/>"
                + "</DeviceList>";
        executeGetData(soap, rpc, info3, "landing007-devicelist-single");

        // Test 4: DeviceID=全9 通配 (query ALL devices under this FSU)
        System.out.println("\n---------- Test 4: DeviceID 通配 (全9) ----------");
        String info4 = "<FsuId>" + FSUID + "</FsuId>"
                + "<FsuCode>" + FSUID + "</FsuCode>"
                + "<DeviceList>"
                + "<Device Id=\"999999999999\" Code=\"999999999999\"/>"
                + "</DeviceList>";
        executeGetData(soap, rpc, info4, "landing007-wildcard-all9");

        System.out.println("\n============================================================");
        System.out.println("  LANDING-007 执行完成");
        System.out.println("  原始报文: " + RAW_DIR);
        System.out.println("============================================================");
    }

    static void executeGetData(SoapMessageHandler soap, FsuServiceRpcAdapter rpc,
                               String infoXml, String prefix) throws Exception {
        // Build inner Request payload: PK_Type Name+Code with Code=401 (2016 GET_DATA)
        String requestPayload = soap.buildRequest("GET_DATA", 401, infoXml, null);

        // RPC wrap
        String rpcRequest = rpc.wrapRequestPayload(requestPayload);

        // HTTP POST
        System.out.println("  [HTTP] GET_DATA (Code=401) → " + SERVICE_URL);
        String rpcResponse = doHttpPost(SERVICE_URL, rpcRequest);

        // RPC unwrap
        String docResponse = rpc.unwrapResponsePayload(rpcResponse);

        // Save
        writeFile(RAW_DIR.resolve(prefix + "-request-payload.xml"), requestPayload);
        writeFile(RAW_DIR.resolve(prefix + "-rpc-request.xml"), rpcRequest);
        writeFile(RAW_DIR.resolve(prefix + "-rpc-response.xml"), rpcResponse);
        writeFile(RAW_DIR.resolve(prefix + "-doc-response.xml"), docResponse);

        // Analyze
        System.out.println("  Request Info size: " + infoXml.length() + " bytes");
        System.out.println("  RPC response size: " + rpcResponse.length() + " bytes");
        System.out.println("  Doc response size: " + docResponse.length() + " bytes");

        // Check if response contains data
        boolean hasDeviceList = docResponse.contains("DeviceList");
        boolean hasTSemaphore = docResponse.contains("TSemaphore");
        boolean hasMeasuredVal = docResponse.contains("MeasuredVal");
        boolean emptyDeviceList = docResponse.contains("<DeviceList/>");
        System.out.println("  DeviceList present: " + hasDeviceList + " | TSemaphore: " + hasTSemaphore
                + " | MeasuredVal: " + hasMeasuredVal + " | empty: " + emptyDeviceList);
        if (emptyDeviceList || (!hasTSemaphore && !hasMeasuredVal)) {
            System.out.println("  => NO signal data in response");
        }
    }

    static String buildDeviceListInfo(boolean withTSemaphore) {
        StringBuilder sb = new StringBuilder();
        sb.append("<FsuId>").append(FSUID).append("</FsuId>");
        sb.append("<FsuCode>").append(FSUID).append("</FsuCode>");
        sb.append("<DeviceList>");
        for (int i = 0; i < DEVICE_IDS.length; i++) {
            sb.append("<Device Id=\"").append(DEVICE_IDS[i])
                    .append("\" Code=\"").append(DEVICE_IDS[i]).append("\"");
            if (withTSemaphore && i < TSEMAPHORE_IDS.length && TSEMAPHORE_IDS[i].length > 0) {
                sb.append(">");
                for (String tsId : TSEMAPHORE_IDS[i]) {
                    sb.append("<TSemaphore Id=\"").append(tsId)
                            .append("\" Code=\"").append(tsId).append("\"/>");
                }
                sb.append("</Device>");
            } else {
                sb.append("/>");
            }
        }
        sb.append("</DeviceList>");
        return sb.toString();
    }

    static String doHttpPost(String urlStr, String soapXml) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            conn.setRequestProperty("SOAPAction", "");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = soapXml.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                return new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            }
        } finally {
            conn.disconnect();
        }
    }

    static void writeFile(Path path, String content) throws Exception {
        if (content != null && !content.isEmpty()) {
            Files.writeString(path, content);
            System.out.println("  已保存: " + path.getFileName());
        }
    }
}
