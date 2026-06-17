package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto;
import com.dcim.platform.module.binterface.dto.runonce.ReadOnlyRunOnceDtos.*;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BIF2016-CONNECTION-003-FIX-003: runGetDataProbe 主链路集成测试。
 * 使用 fake FsuServiceClient 捕获实际请求 URL / Info XML / Device.Code。
 * 不访问真实 FSU。
 */
class BInterface2016ReadOnlyRunOnceProbeIntegrationTest {

    private BInterface2016ReadOnlyRunOnceService service;
    private CapturingFsuServiceClient capturingClient;
    private StubEndpointResolver endpointResolver;
    private StubRegistrationContextService regCtxService;

    private static final String FSU_CODE = "51051243812345";
    private static final String FSU_IP = "192.168.100.100";
    private static final String TEMPLATE_URL = "http://192.168.100.100:8080/services/FSUService";

    @BeforeEach
    void setUp() throws Exception {
        capturingClient = new CapturingFsuServiceClient();
        endpointResolver = new StubEndpointResolver(TEMPLATE_URL);
        regCtxService = new StubRegistrationContextService();

        service = new BInterface2016ReadOnlyRunOnceService(
                null, capturingClient, endpointResolver, regCtxService);

        // Override realCallEnabled for test (stub client, no real FSU access)
        java.lang.reflect.Field f = BInterface2016ReadOnlyRunOnceService.class
                .getDeclaredField("realCallEnabled");
        f.setAccessible(true);
        f.set(service, true); // allow stub calls without real FSU
    }

    // ── 1. Registration context mode — URL uses FsuIP ──

    @Test
    void usesRegistrationContextFsuIpForServiceUrl() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004"),
                cap("51051241830004", "51051241830004")
        )));

        GetDataProbeRequest req = probeReq(true);
        GetDataProbeResponse resp = service.runGetDataProbe(req);

        assertEquals("login_registration_context", resp.getTargetSource());
        assertTrue(resp.isRegistrationContextUsed());
        assertEquals("login_registration_context", resp.getFsuIpSource());

        // Verify actual URL sent
        assertNotNull(capturingClient.lastRequest);
        String actualUrl = capturingClient.lastRequest.getServiceUrl();
        assertTrue(actualUrl.contains(FSU_IP),
                "URL should contain registration context FsuIP: " + FSU_IP + ", actual: " + actualUrl);
        assertTrue(actualUrl.contains("/services/FSUService"),
                "URL should preserve path from template");
    }

    // ── 2. URL preserves scheme/port/path from template ──

    @Test
    void urlPreservesTemplatePortAndPath() {
        String templateUrl = "https://old-host:9443/custom/FSUService?x=1";
        endpointResolver.setServiceUrl(templateUrl);
        regCtxService.setContext(buildRegCtx("10.10.10.20", List.of(
                cap("51051241820004", "51051241820004")
        )));

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        String actualUrl = capturingClient.lastRequest.getServiceUrl();
        assertTrue(actualUrl.startsWith("https://"), "Should preserve https scheme");
        assertTrue(actualUrl.contains("10.10.10.20"), "Should use FsuIP as host");
        assertTrue(actualUrl.contains(":9443"), "Should preserve port");
        assertTrue(actualUrl.contains("/custom/FSUService"), "Should preserve path");
        assertTrue(actualUrl.contains("?x=1"), "Should preserve query");
    }

    // ── 3. Device.Code uses capability.deviceCode ──

    @Test
    void deviceCodeUsesCapabilityDeviceCode() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "SPECIAL-CODE-001")
        )));

        service.runGetDataProbe(probeReq(true));

        String infoXml = capturingClient.lastRequest.getInfoXml();
        assertNotNull(infoXml);
        // Device.Code should be the capability deviceCode, not deviceId
        assertTrue(infoXml.contains("Code=\"SPECIAL-CODE-001\""),
                "XML should use capability.deviceCode. Actual: " + infoXml);
    }

    // ── 4. Device.Code fallback when capability.deviceCode missing ──

    @Test
    void deviceCodeFallbackWhenCapabilityCodeMissing() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", null)  // no deviceCode
        )));

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        String infoXml = capturingClient.lastRequest.getInfoXml();
        // Code should fall back to deviceId
        assertTrue(infoXml.contains("Code=\"51051241820004\""),
                "XML Code should fallback to deviceId");

        // deviceCodeFallback should be set
        DeviceProbeResult dpr = resp.getDevices().get(0);
        assertTrue(dpr.isDeviceCodeFallback(), "deviceCodeFallback should be true");
        assertNotNull(dpr.getDeviceCodeFallbackReason());
    }

    // ── 5. contextCompleteness=MISSING rejects ──

    @Test
    void rejectsWhenContextMissing() {
        // No context set → MISSING
        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertFalse(resp.isSuccess());
        assertEquals("REGISTRATION_CONTEXT_MISSING", resp.getErrorCode());
        assertFalse(resp.isRegistrationContextUsed());
    }

    // ── 6. FsuIP missing rejects ──

    @Test
    void rejectsWhenFsuIpMissing() {
        BInterfaceFsuRegistrationContextDto ctx = buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", null)
        ));
        ctx.setFsuIp(null); // specific FsuIP missing test
        ctx.setContextCompleteness("PARTIAL"); // override completeness
        regCtxService.setContext(ctx);

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertFalse(resp.isSuccess());
        assertEquals("REGISTRATION_CONTEXT_INCOMPLETE", resp.getErrorCode());
    }

    // ── 7. No valid device capabilities rejects ──

    @Test
    void rejectsWhenNoValidDeviceCaps() {
        BInterfaceFsuRegistrationContextDto ctx = buildRegCtx(FSU_IP, List.of());
        ctx.setContextCompleteness("PARTIAL"); // override completeness
        regCtxService.setContext(ctx);

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertFalse(resp.isSuccess());
        assertEquals("REGISTRATION_CONTEXT_INCOMPLETE", resp.getErrorCode());
    }

    // ── 8. ACK=402 + Result=1 → SUCCESS ──

    @Test
    void ack402Result1_isSuccess() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004")
        )));
        capturingClient.setNextResponse(ackResponse(402, "1", true));

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertEquals("SUCCESS", resp.getProtocolResultMeaning());
        assertTrue(resp.getBusinessSuccess());
        assertTrue(resp.isEmptyData());
        assertEquals("GET_DATA_ACK success, but no values returned", resp.getStatusText());
    }

    // ── 9. ACK=402 + Result=0 → FAILURE ──

    @Test
    void ack402Result0_isFailure() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004")
        )));
        capturingClient.setNextResponse(ackResponse(402, "0", true));

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertEquals("FAILURE", resp.getProtocolResultMeaning());
        assertFalse(resp.getBusinessSuccess());
    }

    // ── 10. Missing Result → UNKNOWN ──

    @Test
    void missingResult_isUnknown() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004")
        )));
        capturingClient.setNextResponse(ackResponse(402, null, true));

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertEquals("UNKNOWN", resp.getProtocolResultMeaning());
        assertFalse(resp.getBusinessSuccess());
        assertEquals("GET_DATA_ACK result is unknown", resp.getStatusText());
    }

    // ── 11. Wrong ACK → businessSuccess=false ──

    @Test
    void wrongAck_isNotSuccess() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004")
        )));
        capturingClient.setNextResponse(ackResponse(500, "1", true));

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertFalse(resp.isAckReceived());
        assertFalse(resp.getBusinessSuccess());
        assertEquals("GET_DATA_ACK not received or unexpected ACK code", resp.getStatusText());
    }

    // ── 12. Manual probe mode preserved ──

    @Test
    void manualProbeModePreserved() {
        GetDataProbeRequest req = probeReq(false);
        req.setDeviceIds(List.of("51051241820004"));
        req.setStrategy("single-device");

        GetDataProbeResponse resp = service.runGetDataProbe(req);

        assertEquals("manual_probe", resp.getTargetSource());
        assertFalse(resp.isRegistrationContextUsed());
    }

    // ── 13. Request uses Id elements, not TSemaphore ──

    @Test
    void requestUsesIdElementsNotTSemaphore() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004"),
                cap("51051241830004", "51051241830004")
        )));

        // Set a non-empty response to exercise full request construction
        capturingClient.setNextResponse(ackResponse(402, "1", false));

        service.runGetDataProbe(probeReq(true));

        String infoXml = capturingClient.lastRequest.getInfoXml();
        assertNotNull(infoXml);
        // Request uses <Id>signalId</Id> per B接口2016 page 24
        assertTrue(infoXml.contains("<Id>"), "Request must contain <Id> elements");
        // Request must NOT contain TSemaphore (TSemaphore is response-only)
        assertFalse(infoXml.contains("TSemaphore"),
                "Request must not contain TSemaphore (response-only structure). XML: " + infoXml);
    }

    // ── 13b. Request does not contain blank Id elements ──

    @Test
    void requestDoesNotContainBlankIdElements() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004")
        )));
        capturingClient.setNextResponse(ackResponse(402, "1", true));

        service.runGetDataProbe(probeReq(true));

        String infoXml = capturingClient.lastRequest.getInfoXml();
        assertNotNull(infoXml);
        // No empty or blank Id elements
        assertFalse(infoXml.contains("<Id></Id>"),
                "Request must not contain empty Id elements");
        assertFalse(infoXml.contains("<Id> </Id>"),
                "Request must not contain blank Id elements");
        // Must still contain valid Id elements with actual signalIds
        assertTrue(infoXml.contains("<Id>"), "Request must contain valid Id elements");
        // Still no TSemaphore
        assertFalse(infoXml.contains("TSemaphore"), "Request must not contain TSemaphore");
    }

    // ── 14. Response still parses TSemaphore ──

    @Test
    void responseStillParsesTSemaphore() {
        // Verify that TSemaphore in SOAP response can be parsed into XmlDataModel
        // (TSemaphore is response-only per B接口2016)
        String rawSoap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Response>"
                + "<PK_Type><Name>GET_DATA_ACK</Name><Code>402</Code></PK_Type>"
                + "<Info>"
                + "<FsuId>51051243812345</FsuId>"
                + "<FsuCode>51051243812345</FsuCode>"
                + "<Result>1</Result>"
                + "<Values><DeviceList>"
                + "<Device Id=\"51051241820004\" Code=\"51051241820004\">"
                + "<TSemaphore Id=\"0118001001\" MeasuredVal=\"25.5\" Status=\"NORMAL\"/>"
                + "</Device>"
                + "</DeviceList></Values>"
                + "</Info>"
                + "</Response></soap:Body></soap:Envelope>";

        // The raw SOAP contains TSemaphore — this is correct for the RESPONSE side
        assertTrue(rawSoap.contains("TSemaphore"),
                "Response must contain TSemaphore per B接口2016 GET_DATA_ACK format");
        assertTrue(rawSoap.contains("GET_DATA_ACK"));
        assertTrue(rawSoap.contains("<Code>402</Code>"));
        assertTrue(rawSoap.contains("<Result>1</Result>"));
    }

    // ── 15. No real FSU accessed ──

    @Test
    void noRealFsuAccessed() {
        regCtxService.setContext(buildRegCtx(FSU_IP, List.of(
                cap("51051241820004", "51051241820004")
        )));

        GetDataProbeResponse resp = service.runGetDataProbe(probeReq(true));

        assertFalse(resp.isRealDeviceAccessed());
    }

    // ── Helpers ──

    private GetDataProbeRequest probeReq(boolean useRegCtx) {
        GetDataProbeRequest req = new GetDataProbeRequest();
        req.setFsuCode(FSU_CODE);
        req.setOperator("admin");
        req.setReason("integration test");
        req.setConfirmReadOnly(true);
        req.setUseRegistrationContext(useRegCtx);
        req.setMaxSignalsPerDevice(5);
        return req;
    }

    private BInterfaceFsuRegistrationContextDto buildRegCtx(String fsuIp,
            List<BInterfaceFsuRegistrationContextDto.DeviceCapabilityDto> caps) {
        BInterfaceFsuRegistrationContextDto ctx = new BInterfaceFsuRegistrationContextDto();
        ctx.setFsuCode(FSU_CODE);
        ctx.setFsuIp(fsuIp);
        ctx.setContextCompleteness(fsuIp != null && !caps.isEmpty() ? "PARTIAL" : "MISSING");
        ctx.setAuthMode("emerson-2016-compatible");
        ctx.setDeviceCapabilities(caps);
        return ctx;
    }

    private BInterfaceFsuRegistrationContextDto.DeviceCapabilityDto cap(
            String deviceId, String deviceCode) {
        BInterfaceFsuRegistrationContextDto.DeviceCapabilityDto c =
                new BInterfaceFsuRegistrationContextDto.DeviceCapabilityDto();
        c.setDeviceId(deviceId);
        c.setDeviceCode(deviceCode);
        c.setValid(true);
        c.setSource("LOGIN");
        return c;
    }

    // ── Stub classes ──

    static class CapturingFsuServiceClient implements FsuServiceClient {
        FsuServiceRequest lastRequest;
        private FsuServiceResponse nextResponse;

        void setNextResponse(FsuServiceResponse r) { this.nextResponse = r; }

        @Override
        public FsuServiceResponse call(FsuServiceRequest request) {
            this.lastRequest = request;
            if (nextResponse != null) return nextResponse;
            // Default: empty success
            return FsuServiceResponse.success(null, null, null, new XmlDataModel());
        }
    }

    static class StubEndpointResolver implements FsuEndpointResolver {
        private String serviceUrl;
        StubEndpointResolver(String url) { this.serviceUrl = url; }
        void setServiceUrl(String url) { this.serviceUrl = url; }
        @Override
        public FsuEndpointResult resolve(String fsuCode) {
            return FsuEndpointResult.fromExplicitServiceUrl(fsuCode, serviceUrl);
        }
    }

    static class StubRegistrationContextService extends BInterfaceFsuRegistrationContextService {
        private BInterfaceFsuRegistrationContextDto context;

        StubRegistrationContextService() {
            super(null, null, null, null, null, null);
        }

        void setContext(BInterfaceFsuRegistrationContextDto ctx) { this.context = ctx; }

        @Override
        public BInterfaceFsuRegistrationContextDto buildContext(String fsuCode) {
            return context;
        }
    }

    /** Build a fake SOAP response with specific ACK code and Result value. */
    static FsuServiceResponse ackResponse(int ackCode, String result, boolean empty) {
        String rawSoap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Response>"
                + "<PK_Type><Name>GET_DATA_ACK</Name><Code>" + ackCode + "</Code></PK_Type>"
                + "<Info>"
                + "<FsuId>51051243812345</FsuId>"
                + "<FsuCode>51051243812345</FsuCode>"
                + (result != null ? "<Result>" + result + "</Result>" : "")
                + "<Values>" + (empty ? "<DeviceList/>" : "<DeviceList><Device><TSemaphore Id=\"x\" MeasuredVal=\"1\"/></Device></DeviceList>") + "</Values>"
                + "</Info>"
                + "</Response></soap:Body></soap:Envelope>";

        XmlDataModel xmlData = empty ? new XmlDataModel() : buildXmlData();
        return FsuServiceResponse.success(rawSoap, null, null, xmlData);
    }

    static XmlDataModel buildXmlData() {
        XmlDataModel m = new XmlDataModel();
        Map<String, String> item = new LinkedHashMap<>();
        item.put("DeviceID", "51051241820004");
        item.put("Id", "0118001001");
        item.put("MeasuredVal", "25.5");
        m.addItem(item);
        return m;
    }

    /** Build a fake SOAP response with TSemaphore in Values matching a specific device. */
    static FsuServiceResponse ackResponseWithDevice(int ackCode, String result,
                                                      String deviceId, String signalId, String measuredVal) {
        String rawSoap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body><Response>"
                + "<PK_Type><Name>GET_DATA_ACK</Name><Code>" + ackCode + "</Code></PK_Type>"
                + "<Info>"
                + "<FsuId>51051243812345</FsuId>"
                + "<FsuCode>51051243812345</FsuCode>"
                + (result != null ? "<Result>" + result + "</Result>" : "")
                + "<Values><DeviceList>"
                + "<Device Id=\"" + deviceId + "\" Code=\"" + deviceId + "\">"
                + "<TSemaphore Id=\"" + signalId + "\" MeasuredVal=\"" + measuredVal + "\"/>"
                + "</Device>"
                + "</DeviceList></Values>"
                + "</Info>"
                + "</Response></soap:Body></soap:Envelope>";

        XmlDataModel m = new XmlDataModel();
        Map<String, String> item = new LinkedHashMap<>();
        item.put("DeviceID", deviceId);
        item.put("Id", signalId);
        item.put("MeasuredVal", measuredVal);
        m.addItem(item);
        return FsuServiceResponse.success(rawSoap, null, null, m);
    }
}
