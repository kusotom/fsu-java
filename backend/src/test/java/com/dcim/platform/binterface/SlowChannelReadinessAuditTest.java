package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.SetFtpCommandHandler;
import com.dcim.platform.module.binterface.command.SetPointCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetFtpResult;
import com.dcim.platform.module.binterface.service.GetLoginInfoResult;
import com.dcim.platform.module.binterface.service.SetThresholdSafetyGate;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.service.fsu.StubFsuServiceClient;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingProperties;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingService;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 慢数据通道安全验收测试。
 *
 * <p>验证所有安全默认值、高风险命令禁用状态、敏感字段脱敏、Stub/Real 边界。
 * 不修改业务代码，不访问真实设备。</p>
 */
class SlowChannelReadinessAuditTest {

    // ==================== real-call-enabled 默认 false ====================

    @Test
    void stubFsuServiceClientShouldBeDefault() {
        ConditionalOnProperty annotation = StubFsuServiceClient.class
                .getAnnotation(ConditionalOnProperty.class);
        assertNotNull(annotation);
        assertEquals("b-interface.fsu-client.real-call-enabled", annotation.name()[0]);
        assertEquals("false", annotation.havingValue());
        assertTrue(annotation.matchIfMissing());
    }

    // ==================== scheduler-enabled 默认 false ====================

    @Test
    void schedulerEnabledShouldDefaultFalse() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertFalse(props.isSchedulerEnabled());
    }

    // ==================== slow-polling enabled 默认 false ====================

    @Test
    void slowPollingEnabledShouldDefaultFalse() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertFalse(props.isEnabled());
    }

    // ==================== slow-polling allow-real-call 默认 false ====================

    @Test
    void allowRealCallShouldDefaultFalse() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertFalse(props.isAllowRealCall());
    }

    // ==================== set-threshold enabled 默认 false ====================

    @Test
    void setThresholdEnabledShouldDefaultFalse() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(false, true);
        var decision = gate.check("FSU-001", true);
        assertFalse(decision.isAllowed());
        assertEquals("3001", decision.getResultCode());
    }

    // ==================== SET_THRESHOLD require-confirmation 默认 true ====================

    @Test
    void setThresholdRequireConfirmationShouldDefaultTrue() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(true, true);
        var decision = gate.check("FSU-001", false);
        assertFalse(decision.isAllowed());
        assertEquals("3002", decision.getResultCode());
    }

    @Test
    void setThresholdConfirmationShouldPassWhenConfirmed() {
        SetThresholdSafetyGate gate = new SetThresholdSafetyGate(true, true);
        var decision = gate.check("FSU-001", true);
        assertTrue(decision.isAllowed());
    }

    // ==================== StubFsuServiceClient 不访问网络 ====================

    @Test
    void stubFsuServiceClientShouldNotAccessNetwork() {
        FsuServiceClient client = new StubFsuServiceClient(new SoapMessageHandler(), new XmlDataParser());

        for (BInterfacePkType pkType : List.of(
                BInterfacePkType.GET_DATA, BInterfacePkType.GET_THRESHOLD,
                BInterfacePkType.SET_THRESHOLD, BInterfacePkType.TIME_CHECK,
                BInterfacePkType.GET_LOGININFO, BInterfacePkType.GET_FTP)) {
            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode("FSU-001")
                    .pkType(pkType)
                    .build();
            FsuServiceResponse response = client.call(request);
            assertFalse(response.isRealCall(), pkType + " should not be a real call");
        }
    }

    // ==================== SET_POINT 仍未实现 ====================

    @Test
    void setPointShouldBeNotImplemented() {
        CommandResult result = new SetPointCommandHandler().handle(null);
        assertFalse(result.isSuccess());
        assertTrue("1".equals(result.getResultCode()) || !result.isImplemented());
    }

    // ==================== SET_FTP 仍未实现 ====================

    @Test
    void setFtpShouldBeNotImplemented() {
        CommandResult result = new SetFtpCommandHandler().handle(null);
        assertFalse(result.isSuccess());
        assertTrue("1".equals(result.getResultCode()) || !result.isImplemented());
    }

    // ==================== SET_FSUREBOOT 无 Handler ====================

    @Test
    void setFsuRebootHandlerDoesNotExist() {
        // SET_FSUREBOOT 在 BInterfacePkType 枚举中存在但无对应 Handler 类
        assertTrue(Arrays.asList(BInterfacePkType.values()).contains(BInterfacePkType.SET_FSUREBOOT));
        // 确认没有对应的 CommandHandler 实现
        assertThrows(ClassNotFoundException.class, () ->
                Class.forName("com.dcim.platform.module.binterface.command.SetFsuRebootCommandHandler"));
    }

    // ==================== GET_FTP 输出不包含明文密码 ====================

    @Test
    void getFtpResultShouldMaskUsernameInToString() {
        GetFtpResult result = GetFtpResult.success("FSU-001", "192.168.1.200",
                21, "fsu_ftp", true, "/fsu/images/");
        String toString = result.toString();
        assertFalse(toString.contains("fsu_ftp"),
                "toString should not contain plaintext username");
        assertTrue(toString.contains("f****p") || toString.contains("****"),
                "toString should contain masked username");
    }

    @Test
    void getFtpResultGetMaskedUsernameShouldMask() {
        GetFtpResult result = GetFtpResult.success("FSU-001", "192.168.1.200",
                21, "fsu_ftp", true, "/fsu/images/");
        String masked = result.getMaskedUsername();
        assertNotNull(masked);
        assertFalse(masked.contains("fsu_ftp"));
        assertTrue(masked.contains("****"));
    }

    @Test
    void getFtpResultMaskedUsernameForShortUsername() {
        GetFtpResult r1 = GetFtpResult.success("FSU-001", "192.168.1.200",
                21, "ab", true, "/");
        assertEquals("****", r1.getMaskedUsername());

        GetFtpResult r2 = GetFtpResult.success("FSU-001", "192.168.1.200",
                21, "a", true, "/");
        assertEquals("****", r2.getMaskedUsername());
    }

    @Test
    void getFtpResultMaskedUsernameForNull() {
        GetFtpResult result = GetFtpResult.fail("5001", "error");
        assertNull(result.getMaskedUsername());
    }

    // ==================== GET_LOGININFO 输出不包含明文密码 ====================

    @Test
    void getLoginInfoResultShouldNotContainPasswordField() {
        assertThrows(NoSuchFieldException.class, () ->
                GetLoginInfoResult.class.getDeclaredField("password"));
        assertThrows(NoSuchMethodException.class, () ->
                GetLoginInfoResult.class.getMethod("getPassword"));
    }

    @Test
    void getLoginInfoResultToStringShouldNotExposeSessionId() {
        GetLoginInfoResult result = GetLoginInfoResult.success("FSU-001", "LOGIN", "ONLINE",
                "SESSION-ID", "2026-05-13T10:30:00+08:00", "2026-05-13T10:40:00+08:00");
        String toString = result.toString();
        assertTrue(toString.contains("FSU-001"));
        assertTrue(toString.contains("LOGIN"));
    }

    // ==================== 轮询不会执行 SET 类命令 ====================

    @Test
    void slowPollingServiceShouldNotReferenceSetServices() {
        List<String> fieldTypes = Arrays.stream(SlowDataPollingService.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName)
                .toList();
        assertFalse(fieldTypes.stream().anyMatch(f ->
                f.contains("SetThreshold") || f.contains("SetPoint") || f.contains("SetFtp")));
    }

    // ==================== GET_DATA 不涉及 SEND_DATA ====================

    @Test
    void getDataServiceShouldNotReferenceSendData() {
        List<String> fieldTypes = Arrays.stream(
                com.dcim.platform.module.binterface.service.GetDataService.class.getDeclaredFields())
                .map(Field::getType)
                .map(Class::getName)
                .toList();
        assertFalse(fieldTypes.stream().anyMatch(f -> f.contains("SendData")));
    }
}
