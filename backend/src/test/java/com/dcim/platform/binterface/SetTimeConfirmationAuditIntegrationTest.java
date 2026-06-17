package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.safety.*;
import com.dcim.platform.module.binterface.service.SetTimeExecutionRequest;
import com.dcim.platform.module.binterface.service.SetTimeResult;
import com.dcim.platform.module.binterface.service.SetTimeService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SET_TIME confirmationToken + Audit 主流程集成测试 (BIF-P4-SAFE-004)。
 */
class SetTimeConfirmationAuditIntegrationTest {

    private SetCommandSafetyProperties props;
    private SetCommandSafetyGate gate;
    private SetCommandAuditService audit;
    private ConfirmationTokenService tokenSvc;
    private SoapMessageHandler soapHandler;
    private XmlDataParser xmlDataParser;

    @BeforeEach
    void setUp() {
        props = new SetCommandSafetyProperties();
        gate = new SetCommandSafetyGate(props, null);
        audit = new SetCommandAuditService();
        tokenSvc = new ConfirmationTokenService();
        soapHandler = new SoapMessageHandler();
        xmlDataParser = new XmlDataParser();
    }

    private SetTimeService createService() {
        return new SetTimeService(stubClient(), null, gate, tokenSvc, audit);
    }

    private SetTimeService createServiceNoAudit() {
        return new SetTimeService(stubClient(), null, gate, tokenSvc, null);
    }

    private FsuServiceClient stubClient() {
        return req -> {
            String soap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                    + "<soap:Body><Response>"
                    + "<PK_Type><Name>SET_TIME_ACK</Name><Code>902</Code></PK_Type>"
                    + "<Info><ResultCode>0</ResultCode></Info>"
                    + "</Response></soap:Body></soap:Envelope>";
            BInterfaceMessage msg = soapHandler.parse(soap);
            XmlDataModel xd = (msg.getXmlData() != null) ? xmlDataParser.parse(msg.getXmlData()) : new XmlDataModel();
            return FsuServiceResponse.success(soap, msg.getInfo(), msg.getXmlData(), xd);
        };
    }

    // ==================== Token 缺失拒绝 ====================

    @Test void missingTokenShouldReject() {
        SetTimeService svc = createService();
        SetTimeResult r = svc.executeWithSafety(req(null));
        assertFalse(r.isSuccess());
        assertTrue(r.getSafetyReasonCode() != null
                || r.getResultDesc().contains("GLOBAL_DISABLED"));
    }

    // ==================== 错误 Token 拒绝 ====================

    @Test void wrongTokenShouldReject() {
        enableAll();
        SetTimeService svc = createService();
        SetTimeResult r = svc.executeWithSafety(req("wrong-token"));
        assertFalse(r.isSuccess());
        // Token service validates first
    }

    // ==================== 过期 Token 拒绝 ====================

    @Test void expiredTokenShouldReject() throws Exception {
        enableAll();
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test", 1);
        Thread.sleep(1100);
        SetTimeService svc = createService();
        SetTimeResult r = svc.executeWithSafety(req(raw));
        assertFalse(r.isSuccess());
    }

    // ==================== 有效 Token + 门禁通过 → dry-run ====================

    @Test void validTokenShouldAllowDryRun() {
        enableAll();
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        SetTimeService svc = createService();
        SetTimeResult r = svc.executeWithSafety(req(raw));
        // dry-run (allow-real-call=false)
        assertTrue(r.isSuccess() || r.isDryRun());
    }

    // ==================== dry-run 审计 ====================

    @Test void dryRunShouldWriteAudit() {
        enableAll();
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        SetTimeService svc = createService();
        svc.executeWithSafety(req(raw));
        assertTrue(audit.count() > 0);
    }

    // ==================== 已用 Token 拒绝 ====================

    @Test void usedTokenShouldReject() {
        enableAllWithReal();
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        SetTimeService svc = createService();
        // First call with realCallRequested=true → executes & consumes token
        var realReq = SetTimeExecutionRequest.builder()
                .suid("FSU-001").targetTime("2026-05-16 10:00:00")
                .confirmationToken(raw).requestedBy("admin").source("TEST")
                .schedulerTriggered(false).realCallRequested(true).targetCount(1)
                .operationSummary("test").build();
        SetTimeResult r1 = svc.executeWithSafety(realReq);
        assertTrue(r1.isSuccess());
        // Second call should fail (token already used)
        SetTimeResult r2 = svc.executeWithSafety(realReq);
        assertFalse(r2.isSuccess());
    }

    // ==================== Token 验证失败写审计 ====================

    @Test void tokenFailShouldWriteAudit() {
        enableAll();
        SetTimeService svc = createService();
        svc.executeWithSafety(req("bogus"));
        assertTrue(audit.count() > 0);
    }

    // ==================== auditService null 不 NPE ====================

    @Test void nullAuditShouldNotThrow() {
        enableAll();
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        SetTimeService svc = createServiceNoAudit();
        assertDoesNotThrow(() -> svc.executeWithSafety(req(raw)));
    }

    // ==================== token 不泄露 ====================

    @Test void resultShouldNotContainPlaintextToken() {
        enableAll();
        String raw = tokenSvc.generateToken("SET_TIME", "FSU-001", "admin", "test");
        SetTimeService svc = createService();
        SetTimeResult r = svc.executeWithSafety(req(raw));
        assertFalse(r.toString().contains(raw));
    }

    // ==================== 旧 execute 兼容 ====================

    @Test void oldExecuteShouldStillWork() {
        SetTimeService svc = createService();
        SetTimeResult r = svc.execute("FSU-001", null, "2026-05-16 10:00:00");
        // old method without gate params defaults to null token
        assertNotNull(r);
    }

    // ==================== 辅助 ====================

    private SetTimeExecutionRequest req(String token) {
        return SetTimeExecutionRequest.builder()
                .suid("FSU-001").targetTime("2026-05-16 10:00:00")
                .confirmationToken(token).requestedBy("admin").source("TEST")
                .schedulerTriggered(false).realCallRequested(false).targetCount(1)
                .operationSummary("test").build();
    }

    private void enableAll() {
        props.setEnabled(true);
        SetCommandSafetyProperties.CommandSafetyConfig c = new SetCommandSafetyProperties.CommandSafetyConfig();
        c.setEnabled(true); c.setAllowRealCall(false); c.setRequireConfirmation(true); c.setAuditRequired(true);
        props.getCommands().put("SET_TIME", c);
    }

    private void enableAllWithReal() {
        props.setEnabled(true);
        SetCommandSafetyProperties.CommandSafetyConfig c = new SetCommandSafetyProperties.CommandSafetyConfig();
        c.setEnabled(true); c.setAllowRealCall(true); c.setRequireConfirmation(true); c.setAuditRequired(true);
        props.getCommands().put("SET_TIME", c);
    }
}
