package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.Test;

import static com.dcim.platform.binterface.BInterface2016StandardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-TEST-P0-001: SCService 入站命令协议测试。
 *
 * 覆盖 FSU -> SC 的 LOGIN / LOGOUT / SEND_ALARM；TIME_CHECK 在 SPEC-P0-001
 * 命令矩阵中为 SC -> FSU，因此此处只记录为非 SCService 入站命令。
 */
class BInterface2016ScServiceCommandTest {

    private final SoapMessageHandler soap = new SoapMessageHandler();

    @Test
    void loginRequestShouldParseAsStandard2016InboundMessage() {
        assertInboundRequest("fixtures/b_interface_2016_standard/login_success.request.xml",
                "LOGIN", 101);
    }

    @Test
    void logoutRequestShouldParseAsStandard2016InboundMessageEvenIfBusinessHandlerIsMissing() {
        assertInboundRequest("fixtures/b_interface_2016_standard/logout_success.request.xml",
                "LOGOUT", 103);
    }

    @Test
    void sendAlarmRequestShouldParseAsStandard2016InboundMessage() {
        assertInboundRequest("fixtures/b_interface_2016_standard/send_alarm_success.request.xml",
                "SEND_ALARM", 501);
    }

    @Test
    void timeCheckIsNotAStandardScServiceInboundCommandInSpecP0001Matrix() {
        CommandSpec spec = spec("TIME_CHECK");
        assertEquals(Direction.SC_TO_FSU, spec.direction(),
                "SPEC-P0-001 command matrix defines TIME_CHECK as SC -> FSU");
    }

    @Test
    void scServiceAckXmlShouldUseAckPkTypeNameCodeAndResultField() {
        CommandResult ackResult = CommandResult.success(BInterfacePkType.LOGIN_ACK);
        String actualAck = soap.buildResponseWithCode(
                ackResult.getPkType().name(), 102,
                ackResult.toInfoXml(),
                ackResult.toXmlDataXml());

        var doc = parse(actualAck);
        assertEquals("LOGIN_ACK", text(doc, "Name"),
                "LOGIN ACK must use standard 2016 PK_Type Name=LOGIN_ACK");
        assertEquals(102, intText(doc, "Code"),
                "LOGIN ACK must use standard 2016 Code=102");
        assertEquals("1", text(doc, "Result"),
                "LOGIN ACK success must use Result=1");
        assertFalse(containsResultCode(actualAck),
                "SCService standard 2016 ACK must not depend on ResultCode");
    }

    private void assertInboundRequest(String resource, String expectedName, int expectedCode) {
        String xml = readResource(resource);
        BInterfaceMessage message = soap.parse(xml);
        var doc = parse(xml);

        assertEquals("Request", message.getRequest());
        assertEquals(expectedName, message.getPkType().name());
        assertEquals(expectedName, text(doc, "Name"));
        assertEquals(expectedCode, intText(doc, "Code"));
        assertNotNull(message.getInfo());
        assertFalse(containsResultCode(xml));
    }
}
