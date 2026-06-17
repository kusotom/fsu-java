package com.dcim.platform.binterface;

import org.junit.jupiter.api.Test;

import static com.dcim.platform.binterface.BInterface2016StandardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-TEST-P0-001: B接口2016 Result 语义测试。
 *
 * 依据:
 * - SPEC-2016-RESULT-001: EnumResult FAILURE=0, SUCCESS=1
 * - SPEC-2016-RESULT-002: ResultCode 不是本 2016 docx 标准字段
 * - SPEC-2016-PROFILE-EMERSON-002: Emerson 空 DeviceList 只能作为 profile 行为
 */
class BInterface2016ResultSemanticsTest {

    @Test
    void resultOneShouldMeanSuccess() {
        String response = soapEnvelope(responsePayload("GET_DATA_ACK", 402, "1", ""));

        assertEquals("1", resultOf(response));
        assertTrue(is2016Success(response));
    }

    @Test
    void resultZeroShouldMeanFailure() {
        String response = soapEnvelope(responsePayload("GET_DATA_ACK", 402, "0", ""));

        assertEquals("0", resultOf(response));
        assertFalse(is2016Success(response));
    }

    @Test
    void resultCodeMustNotOverride2016ResultField() {
        String response = soapEnvelope(
                "<Response><PK_Type><Name>GET_DATA_ACK</Name><Code>402</Code></PK_Type>"
                        + "<Info><Result>1</Result><ResultCode>9999</ResultCode></Info></Response>");

        assertTrue(is2016Success(response),
                "2016 success/failure must be decided by Result, not by ResultCode");
        assertTrue(containsResultCode(response),
                "This fixture intentionally contains ResultCode as a non-standard compatibility field");
    }

    @Test
    void resultCodeOnlyShouldBeUnknownForStrict2016Semantics() {
        String response = soapEnvelope(
                "<Response><PK_Type><Name>GET_DATA_ACK</Name><Code>402</Code></PK_Type>"
                        + "<Info><ResultCode>0</ResultCode></Info></Response>");

        assertNull(resultOf(response), "Strict B接口2016 standard parsing has no Result field here");
        assertFalse(is2016Success(response),
                "ResultCode-only response must not be silently treated as 2016 standard success");
    }

    @Test
    void getDataAckResultOneWithEmptyDeviceListIsAckSuccessButNoPointValues() {
        String response = readResource(
                "fixtures/b_interface_2016_standard/get_data_success.response.xml");
        GetDataAckClassification classification = classifyGetDataAck(response);

        assertTrue(classification.ackSuccess());
        assertTrue(classification.deviceResponded());
        assertTrue(classification.emptyPointValues());
        assertFalse(classification.protocolFailure());
        assertEquals("ACK_SUCCESS_EMPTY_VALUES", classification.status());
    }

    private GetDataAckClassification classifyGetDataAck(String responseXml) {
        var doc = parse(responseXml);
        boolean ackCodeOk = "GET_DATA_ACK".equals(text(doc, "Name")) && intText(doc, "Code") == 402;
        boolean resultSuccess = "1".equals(text(doc, "Result"));
        boolean deviceListPresent = first(doc, "DeviceList") != null;
        boolean hasSemaphore = first(doc, "TSemaphore") != null;
        boolean emptyValues = deviceListPresent && !hasSemaphore;
        boolean ackSuccess = ackCodeOk && resultSuccess;

        return new GetDataAckClassification(
                ackSuccess,
                ackCodeOk,
                emptyValues,
                !ackSuccess,
                ackSuccess && emptyValues ? "ACK_SUCCESS_EMPTY_VALUES" : "OTHER");
    }

    private record GetDataAckClassification(boolean ackSuccess,
                                            boolean deviceResponded,
                                            boolean emptyPointValues,
                                            boolean protocolFailure,
                                            String status) {
    }
}
