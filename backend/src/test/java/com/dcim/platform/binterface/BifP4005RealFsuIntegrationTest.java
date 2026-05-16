package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.GetDataResult;
import com.dcim.platform.module.binterface.service.GetDataService;
import com.dcim.platform.module.binterface.service.GetFtpResult;
import com.dcim.platform.module.binterface.service.GetFtpService;
import com.dcim.platform.module.binterface.service.GetLoginInfoResult;
import com.dcim.platform.module.binterface.service.GetLoginInfoService;
import com.dcim.platform.module.binterface.service.GetThresholdResult;
import com.dcim.platform.module.binterface.service.GetThresholdService;
import com.dcim.platform.module.binterface.service.TimeCheckResult;
import com.dcim.platform.module.binterface.service.TimeCheckService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRpcAdapter;
import com.dcim.platform.module.binterface.service.fsu.RealHttpFsuServiceClient;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BIF-P4-005: 单台真实 FSU 只读联调测试。
 *
 * <p>不依赖 Spring 容器（手动 wiring），直接向真实 FSU 发送 SOAP 请求。
 * 执行前需确认：
 * <ol>
 *   <li>application.yml 中 real-call-enabled=true</li>
 *   <li>目标 FSU (192.168.100.100:80) 网络可达</li>
 * </ol>
 *
 * <p>执行顺序：GET_LOGININFO → TIME_CHECK → GET_DATA → GET_THRESHOLD → GET_FTP</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BifP4005RealFsuIntegrationTest {

    private static final String FSU_CODE = "51051243812345";
    private static final String SERVICE_URL = "http://192.168.100.100:8080/services/FSUService";

    private static final SoapMessageHandler soapHandler = new SoapMessageHandler();
    private static final XmlDataParser xmlDataParser = new XmlDataParser();
    private static final FsuServiceRpcAdapter rpcAdapter = new FsuServiceRpcAdapter();
    private static final RealHttpFsuServiceClient client =
            new RealHttpFsuServiceClient(soapHandler, xmlDataParser, rpcAdapter, 5000, 10000);

    // ==================== Step 1: GET_LOGININFO ====================

    @Test
    @Order(1)
    void step1_getLoginInfo() {
        System.out.println("\n========== STEP 1: GET_LOGININFO ==========");

        GetLoginInfoService service = new GetLoginInfoService(client, null);
        GetLoginInfoResult result = service.execute(FSU_CODE, SERVICE_URL);

        System.out.println("success: " + result.isSuccess());
        System.out.println("resultCode: " + result.getResultCode());
        System.out.println("resultDesc: " + result.getResultDesc());
        System.out.println("fsuCode: " + result.getFsuCode());
        System.out.println("loginStatus: " + result.getLoginStatus());
        System.out.println("onlineStatus: " + result.getOnlineStatus());
        System.out.println("sessionId: " + result.getSessionId());
        System.out.println("loginTime: " + result.getLoginTime());
        System.out.println("lastHeartbeat: " + result.getLastHeartbeat());

        // 验证：不因网络异常崩溃，返回结构化结果
        assertNotNull(result);
        System.out.println("GET_LOGININFO 完成 (success=" + result.isSuccess() + ")");
    }

    // ==================== Step 2: TIME_CHECK ====================

    @Test
    @Order(2)
    void step2_timeCheck() {
        System.out.println("\n========== STEP 2: TIME_CHECK ==========");

        TimeCheckService service = new TimeCheckService(client, null);
        String standardTime = Instant.now().toString();
        System.out.println("standardTime: " + standardTime);

        TimeCheckResult result = service.execute(FSU_CODE, SERVICE_URL, standardTime);

        System.out.println("success: " + result.isSuccess());
        System.out.println("resultCode: " + result.getResultCode());
        System.out.println("resultDesc: " + result.getResultDesc());
        System.out.println("fsuCode: " + result.getFsuCode());
        System.out.println("standardTime: " + result.getStandardTime());
        System.out.println("fsuTime: " + result.getFsuTime());

        assertNotNull(result);
        System.out.println("TIME_CHECK 完成 (success=" + result.isSuccess() + ")");
    }

    // ==================== Step 3: GET_DATA ====================

    @Test
    @Order(3)
    void step3_getData() {
        System.out.println("\n========== STEP 3: GET_DATA ==========");

        GetDataService service = new GetDataService(client, null);
        List<String> signalIds = List.of("TEMP-R01", "HUMI-R01");
        System.out.println("signalIds: " + signalIds);

        GetDataResult result = service.execute(FSU_CODE, SERVICE_URL, signalIds);

        System.out.println("success: " + result.isSuccess());
        System.out.println("resultCode: " + result.getResultCode());
        System.out.println("resultDesc: " + result.getResultDesc());
        System.out.println("fsuCode: " + result.getFsuCode());

        if (result.getSignals() != null) {
            System.out.println("signal count: " + result.getSignals().size());
            for (GetDataResult.SignalValue sv : result.getSignals()) {
                System.out.println("  Signal: id=" + sv.getSignalId()
                        + ", value=" + sv.getValue()
                        + ", quality=" + sv.getQuality()
                        + ", status=" + sv.getStatus()
                        + ", time=" + sv.getCollectTime());
            }
        }

        assertNotNull(result);
        System.out.println("GET_DATA 完成 (success=" + result.isSuccess() + ")");
    }

    // ==================== Step 4: GET_THRESHOLD ====================

    @Test
    @Order(4)
    void step4_getThreshold() {
        System.out.println("\n========== STEP 4: GET_THRESHOLD ==========");

        GetThresholdService service = new GetThresholdService(client, null);
        List<String> signalIds = List.of("TEMP-R01", "HUMI-R01");
        System.out.println("signalIds: " + signalIds);

        GetThresholdResult result = service.execute(FSU_CODE, SERVICE_URL, signalIds);

        System.out.println("success: " + result.isSuccess());
        System.out.println("resultCode: " + result.getResultCode());
        System.out.println("resultDesc: " + result.getResultDesc());
        System.out.println("fsuCode: " + result.getFsuCode());

        if (result.getThresholds() != null) {
            System.out.println("threshold count: " + result.getThresholds().size());
            for (GetThresholdResult.ThresholdValue tv : result.getThresholds()) {
                System.out.println("  Threshold: id=" + tv.getSignalId()
                        + ", upper=" + tv.getAlarmUpper()
                        + ", lower=" + tv.getAlarmLower()
                        + ", upperUrgent=" + tv.getAlarmUpperUrgent()
                        + ", lowerUrgent=" + tv.getAlarmLowerUrgent());
            }
        }

        assertNotNull(result);
        System.out.println("GET_THRESHOLD 完成 (success=" + result.isSuccess() + ")");
    }

    // ==================== Step 5: GET_FTP ====================

    @Test
    @Order(5)
    void step5_getFtp() {
        System.out.println("\n========== STEP 5: GET_FTP ==========");

        GetFtpService service = new GetFtpService(client, null);
        System.out.println("fileType: " + null + " (查询全部)");

        GetFtpResult result = service.execute(FSU_CODE, SERVICE_URL, null);

        System.out.println("success: " + result.isSuccess());
        System.out.println("resultCode: " + result.getResultCode());
        System.out.println("resultDesc: " + result.getResultDesc());
        System.out.println("fsuCode: " + result.getFsuCode());
        System.out.println("host: " + result.getHost());
        System.out.println("port: " + result.getPort());
        System.out.println("username: " + (result.getUsername() != null
                ? result.getUsername().substring(0, 1) + "****" : null));
        System.out.println("passiveMode: " + result.isPassiveMode());
        System.out.println("basePath: " + result.getBasePath());

        assertNotNull(result);
        System.out.println("GET_FTP 完成 (success=" + result.isSuccess() + ")");
    }
}
