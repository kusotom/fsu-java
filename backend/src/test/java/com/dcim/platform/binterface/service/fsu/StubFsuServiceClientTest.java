package com.dcim.platform.binterface.service.fsu;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.service.fsu.StubFsuServiceClient;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StubFsuServiceClient 单元测试。
 */
class StubFsuServiceClientTest {

    private FsuServiceClient client;

    @BeforeEach
    void setUp() {
        client = new StubFsuServiceClient(new SoapMessageHandler(), new XmlDataParser());
    }

    @Test
    void shouldReturnGetDataResponse() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_DATA)
                .build();

        FsuServiceResponse response = client.call(request);

        assertTrue(response.isSuccess());
        assertEquals("0", response.getResultCode());
        assertNotNull(response.getXmlData());
        assertEquals(5, response.getXmlData().itemCount());
    }

    @Test
    void shouldReturnGetDataWithCorrectSignalIds() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_DATA)
                .build();

        FsuServiceResponse response = client.call(request);
        var signalIds = response.getXmlData().getItemFieldValues("SignalID");

        assertTrue(signalIds.contains("TEMP-001"));
        assertTrue(signalIds.contains("HUMI-001"));
        assertTrue(signalIds.contains("VOLT-001"));
        assertTrue(signalIds.contains("DOOR-001"));
        assertTrue(signalIds.contains("WATER-001"));
    }

    @Test
    void shouldReturnErrorForUnsupportedPkType() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.SET_FTP)
                .build();

        FsuServiceResponse response = client.call(request);

        assertFalse(response.isSuccess());
        assertEquals("1", response.getResultCode());
    }

    @Test
    void shouldReturnErrorForNullRequest() {
        FsuServiceResponse response = client.call(null);

        assertFalse(response.isSuccess());
        assertEquals("5001", response.getResultCode());
    }

    @Test
    void shouldNotAccessNetwork() {
        // 验证 StubFsuServiceClient 不会访问网络
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_DATA)
                .build();

        // 不应抛出任何网络异常
        FsuServiceResponse response = client.call(request);
        assertFalse(response.isRealCall());
        assertTrue(response.isSuccess());
    }

    @Test
    void responseShouldContainRawSoap() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_DATA)
                .build();

        FsuServiceResponse response = client.call(request);

        assertNotNull(response.getRawSoap());
        assertTrue(response.getRawSoap().contains("soap:Envelope"));
        assertTrue(response.getRawSoap().contains("GET_DATA"));
    }

    @Test
    void responseShouldContainInfoXml() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_DATA)
                .build();

        FsuServiceResponse response = client.call(request);

        assertNotNull(response.getInfoXml());
        assertTrue(response.getInfoXml().contains("ResultCode"));
    }

    // ==================== GET_THRESHOLD ====================

    @Test
    void shouldReturnGetThresholdResponse() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_THRESHOLD)
                .build();

        FsuServiceResponse response = client.call(request);

        assertTrue(response.isSuccess());
        assertEquals("0", response.getResultCode());
        assertNotNull(response.getXmlData());
        assertEquals(3, response.getXmlData().itemCount());
    }

    @Test
    void shouldReturnGetThresholdWithCorrectSignalIds() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_THRESHOLD)
                .build();

        FsuServiceResponse response = client.call(request);
        var signalIds = response.getXmlData().getItemFieldValues("SignalID");

        assertTrue(signalIds.contains("TEMP-001"));
        assertTrue(signalIds.contains("HUMI-001"));
        assertTrue(signalIds.contains("VOLT-001"));
    }

    @Test
    void getThresholdResponseShouldContainThresholdFields() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_THRESHOLD)
                .build();

        FsuServiceResponse response = client.call(request);
        var fields = response.getXmlData().getItemFieldValues("AlarmUpper");

        assertTrue(fields.contains("60.0"));
        assertTrue(fields.contains("90.0"));
        assertTrue(fields.contains("245.0"));
    }

    @Test
    void getThresholdShouldNotAccessNetwork() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_THRESHOLD)
                .build();

        FsuServiceResponse response = client.call(request);
        assertFalse(response.isRealCall());
        assertTrue(response.isSuccess());
    }

    // ==================== SET_THRESHOLD ====================

    @Test
    void shouldReturnSetThresholdSuccess() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.SET_THRESHOLD)
                .xmlDataXml("<Signal><SignalID>TEMP-001</SignalID><AlarmUpper>65.0</AlarmUpper></Signal>")
                .build();

        FsuServiceResponse response = client.call(request);

        assertTrue(response.isSuccess());
        assertEquals("0", response.getResultCode());
    }

    @Test
    void setThresholdResponseShouldContainCount() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.SET_THRESHOLD)
                .build();

        FsuServiceResponse response = client.call(request);

        assertNotNull(response.getInfoXml());
        assertTrue(response.getInfoXml().contains("Count") || response.getInfoXml().contains("ResultCode"));
    }

    @Test
    void setThresholdShouldNotAccessNetwork() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.SET_THRESHOLD)
                .build();

        FsuServiceResponse response = client.call(request);

        assertFalse(response.isRealCall());
        assertTrue(response.isSuccess());
    }

    @Test
    void setThresholdShouldContainRawSoap() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.SET_THRESHOLD)
                .build();

        FsuServiceResponse response = client.call(request);

        assertNotNull(response.getRawSoap());
        assertTrue(response.getRawSoap().contains("SET_THRESHOLD"));
    }

    // ==================== GET_LOGININFO ====================

    @Test
    void shouldReturnGetLoginInfoResponse() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_LOGININFO)
                .build();

        FsuServiceResponse response = client.call(request);

        assertTrue(response.isSuccess());
        assertEquals("0", response.getResultCode());
        assertNotNull(response.getXmlData());
    }

    @Test
    void getLoginInfoResponseShouldContainLoginStatus() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_LOGININFO)
                .build();

        FsuServiceResponse response = client.call(request);

        assertEquals("LOGIN", response.getXmlData().getFirstItemField("LoginStatus"));
        assertEquals("ONLINE", response.getXmlData().getFirstItemField("OnlineStatus"));
    }

    @Test
    void getLoginInfoShouldNotAccessNetwork() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_LOGININFO)
                .build();

        FsuServiceResponse response = client.call(request);
        assertFalse(response.isRealCall());
        assertTrue(response.isSuccess());
    }

    @Test
    void getLoginInfoResponseShouldContainRawSoap() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_LOGININFO)
                .build();

        FsuServiceResponse response = client.call(request);

        assertNotNull(response.getRawSoap());
        assertTrue(response.getRawSoap().contains("GET_LOGININFO"));
    }

    // ==================== GET_FTP ====================

    @Test
    void shouldReturnGetFtpResponse() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_FTP)
                .build();

        FsuServiceResponse response = client.call(request);

        assertTrue(response.isSuccess());
        assertEquals("0", response.getResultCode());
        assertNotNull(response.getXmlData());
    }

    @Test
    void getFtpResponseShouldContainFtpConfigFields() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_FTP)
                .build();

        FsuServiceResponse response = client.call(request);

        assertEquals("192.168.1.200", response.getXmlData().getFirstItemField("Host"));
        assertEquals("21", response.getXmlData().getFirstItemField("Port"));
        assertEquals("fsu_ftp", response.getXmlData().getFirstItemField("Username"));
    }

    @Test
    void getFtpShouldNotAccessNetwork() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_FTP)
                .build();

        FsuServiceResponse response = client.call(request);
        assertFalse(response.isRealCall());
        assertTrue(response.isSuccess());
    }

    @Test
    void getFtpResponseShouldContainRawSoap() {
        FsuServiceRequest request = FsuServiceRequest.builder()
                .fsuCode("FSU-001")
                .pkType(BInterfacePkType.GET_FTP)
                .build();

        FsuServiceResponse response = client.call(request);

        assertNotNull(response.getRawSoap());
        assertTrue(response.getRawSoap().contains("GET_FTP"));
    }
}
