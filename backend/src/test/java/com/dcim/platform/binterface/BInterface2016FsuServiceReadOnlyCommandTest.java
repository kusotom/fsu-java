package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static com.dcim.platform.binterface.BInterface2016StandardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-TEST-P0-001: FSUService 出站只读命令测试。
 *
 * 覆盖 SC -> FSU 的 GET_DATA / GET_LOGININFO / GET_FTP / GET_FSUINFO /
 * GET_THRESHOLD / TIME_CHECK 请求格式和当前服务请求对象。
 */
class BInterface2016FsuServiceReadOnlyCommandTest {

    private final SoapMessageHandler soap = new SoapMessageHandler();
    private final FsuServiceRpcAdapter rpcAdapter = new FsuServiceRpcAdapter();

    @Test
    void constructedStandard2016OutboundRequestsShouldUseNameCodePkType() {
        for (CommandSpec spec : List.of(
                spec("GET_DATA"),
                spec("GET_LOGININFO"),
                spec("GET_FTP"),
                spec("GET_FSUINFO"),
                spec("GET_THRESHOLD"),
                spec("TIME_CHECK"))) {
            String payload = soap.buildRequest(spec.name(), spec.code(),
                    "<FsuCode>" + FSU_CODE + "</FsuCode>",
                    spec.name().equals("GET_THRESHOLD") ? "<Id>0118001001</Id>" : null);
            BInterfaceMessage parsed = soap.parse(rpcAdapter.wrapRequestPayload(payload));
            var doc = parse(payload);

            assertEquals(spec.name(), parsed.getPkType().name(), spec.name());
            assertEquals(spec.name(), text(doc, "Name"), spec.name());
            assertEquals(spec.code(), intText(doc, "Code"), spec.name());
            assertFalse(payload.contains("GET_SUINFO"), "2016 standard must not use 2024 GET_SUINFO");
            assertFalse(payload.contains("GET_SUFTP"), "2016 standard must not use 2024 GET_SUFTP");
        }
    }

    @Test
    void currentReadOnlyServicesShouldRequestLegacy2016PkTypeFormat() {
        CapturingFsuClient client = new CapturingFsuClient();

        client.responseToReturn = loginInfoResponse();
        new GetLoginInfoService(client, null).execute(FSU_CODE, null);
        FsuServiceRequest getLoginInfo = client.lastRequest;

        client.responseToReturn = ftpResponse();
        new GetFtpService(client, null).execute(FSU_CODE, null, "IMAGE");
        FsuServiceRequest getFtp = client.lastRequest;

        client.responseToReturn = thresholdResponse();
        new GetThresholdService(client, null).execute(FSU_CODE, null, List.of("0118001001"));
        FsuServiceRequest getThreshold = client.lastRequest;

        client.responseToReturn = timeCheckResponse();
        new TimeCheckService(client, null).execute(FSU_CODE, null, "2026-05-26 10:00:00");
        FsuServiceRequest timeCheck = client.lastRequest;

        assertAll(
                () -> assertEquals("legacy-2016", getLoginInfo.getPkTypeFormat(),
                        "GET_LOGININFO must use B接口2016 Name+Code mapping 1501"),
                () -> assertEquals("legacy-2016", getFtp.getPkTypeFormat(),
                        "GET_FTP must use B接口2016 Name+Code mapping 1601"),
                () -> assertEquals("legacy-2016", getThreshold.getPkTypeFormat(),
                        "GET_THRESHOLD must use B接口2016 Name+Code mapping 1901"),
                () -> assertEquals("legacy-2016", timeCheck.getPkTypeFormat(),
                        "TIME_CHECK must use B接口2016 Name+Code mapping 1301"));
    }

    @Test
    void readOnlyServicesMustNotUse2024CommandNames() {
        CapturingFsuClient client = new CapturingFsuClient();
        client.responseToReturn = loginInfoResponse();
        new GetLoginInfoService(client, null).execute(FSU_CODE, null);
        assertEquals(BInterfacePkType.GET_LOGININFO, client.lastRequest.getPkType());
        assertNotEquals(BInterfacePkType.GET_SUINFO, client.lastRequest.getPkType());

        client.responseToReturn = ftpResponse();
        new GetFtpService(client, null).execute(FSU_CODE, null, "IMAGE");
        assertEquals(BInterfacePkType.GET_FTP, client.lastRequest.getPkType());
        assertNotEquals(BInterfacePkType.GET_SUFTP, client.lastRequest.getPkType());
    }

    private static class CapturingFsuClient implements FsuServiceClient {
        FsuServiceResponse responseToReturn;
        FsuServiceRequest lastRequest;

        @Override
        public FsuServiceResponse call(FsuServiceRequest request) {
            this.lastRequest = request;
            return responseToReturn;
        }
    }

    private static FsuServiceResponse loginInfoResponse() {
        XmlDataModel model = new XmlDataModel();
        model.setRootName("LoginInfo");
        var item = new LinkedHashMap<String, String>();
        item.put("LoginStatus", "LOGIN");
        item.put("OnlineStatus", "ONLINE");
        item.put("SessionID", "SESSION-001");
        item.put("LoginTime", "2026-05-26 09:00:00");
        item.put("LastHeartbeat", "2026-05-26 10:00:00");
        model.addItem(item);
        return FsuServiceResponse.success(
                readResource("fixtures/b_interface_2016_standard/get_logininfo_success.response.xml"),
                "<Result>1</Result>",
                "<LoginInfo/>",
                model);
    }

    private static FsuServiceResponse ftpResponse() {
        XmlDataModel model = new XmlDataModel();
        model.setRootName("FTPConfig");
        var item = new LinkedHashMap<String, String>();
        item.put("Host", "192.0.2.10");
        item.put("Port", "21");
        item.put("Username", "fsu_ftp");
        item.put("PassiveMode", "true");
        item.put("BasePath", "/fsu/");
        model.addItem(item);
        return FsuServiceResponse.success(
                readResource("fixtures/b_interface_2016_standard/get_ftp_success.response.xml"),
                "<Result>1</Result>",
                "<FTPConfig/>",
                model);
    }

    private static FsuServiceResponse thresholdResponse() {
        XmlDataModel model = new XmlDataModel();
        model.setRootName("Signal");
        var item = new LinkedHashMap<String, String>();
        item.put("SignalID", "0118001001");
        item.put("AlarmUpper", "30");
        item.put("AlarmLower", "10");
        item.put("AlarmUpperUrgent", "35");
        item.put("AlarmLowerUrgent", "5");
        model.addItem(item);
        return FsuServiceResponse.success(
                readResource("fixtures/b_interface_2016_standard/get_threshold_success.response.xml"),
                "<Result>1</Result>",
                "<Signal/>",
                model);
    }

    private static FsuServiceResponse timeCheckResponse() {
        return FsuServiceResponse.success(
                readResource("fixtures/b_interface_2016_standard/time_check_success.response.xml"),
                "<Result>1</Result><FSUTime>2026-05-26 10:00:01</FSUTime>",
                null,
                new XmlDataModel());
    }
}
