package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.dto.runonce.ReadOnlyRunOnceDtos.*;
import com.dcim.platform.module.binterface.service.BInterface2016ReadOnlyRunOnceService;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FAST-REALDATA-001: ReadOnlyRunOnceService 单元测试。
 * 测试参数校验、DeviceType 推断、CSV 过滤、DTO 结构。
 */
class BInterface2016ReadOnlyRunOnceServiceTest {

    // ==================== DeviceType inference ====================

    @Test
    void inferDeviceTypeCode_18_from_51051241820004() {
        assertEquals("18", BInterface2016ReadOnlyRunOnceService.inferDeviceTypeCode("51051241820004"));
    }

    @Test
    void inferDeviceTypeCode_07_from_51051240700002() {
        assertEquals("07", BInterface2016ReadOnlyRunOnceService.inferDeviceTypeCode("51051240700002"));
    }

    @Test
    void inferDeviceTypeCode_38_from_51051243812345() {
        assertEquals("38", BInterface2016ReadOnlyRunOnceService.inferDeviceTypeCode("51051243812345"));
    }

    @Test
    void inferDeviceTypeCode_null_returns_00() {
        assertEquals("00", BInterface2016ReadOnlyRunOnceService.inferDeviceTypeCode(null));
    }

    @Test
    void inferDeviceTypeCode_short_returns_00() {
        assertEquals("00", BInterface2016ReadOnlyRunOnceService.inferDeviceTypeCode("12345"));
    }

    // ==================== CSV filtering ====================

    @Test
    void filterByDeviceType_matches_signal_id_device_type_position() {
        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> dict = new ArrayList<>();
        BInterface2016ReadOnlyRunOnceService.CsvSignalRow r1 = new BInterface2016ReadOnlyRunOnceService.CsvSignalRow();
        r1.signalId = "0106001001"; // substring(2,4) = "06"
        r1.signalName = "开关电源信号";
        r1.signalType = "遥信";
        dict.add(r1);

        BInterface2016ReadOnlyRunOnceService.CsvSignalRow r2 = new BInterface2016ReadOnlyRunOnceService.CsvSignalRow();
        r2.signalId = "0118001001"; // substring(2,4) = "18"
        r2.signalName = "环境信号";
        r2.signalType = "遥测";
        dict.add(r2);

        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> result =
                BInterface2016ReadOnlyRunOnceService.filterByDeviceType(dict, "18", 30);

        assertEquals(1, result.size());
        assertEquals("环境信号", result.get(0).signalName);
    }

    @Test
    void filterByDeviceType_respects_max_limit() {
        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> dict = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BInterface2016ReadOnlyRunOnceService.CsvSignalRow r = new BInterface2016ReadOnlyRunOnceService.CsvSignalRow();
            r.signalId = String.format("01180%03d", i + 1);
            r.signalName = "信号" + (i + 1);
            dict.add(r);
        }

        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> result =
                BInterface2016ReadOnlyRunOnceService.filterByDeviceType(dict, "18", 3);

        assertEquals(3, result.size());
    }

    @Test
    void filterByDeviceType_empty_for_unmatched_type() {
        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> dict = new ArrayList<>();
        BInterface2016ReadOnlyRunOnceService.CsvSignalRow r = new BInterface2016ReadOnlyRunOnceService.CsvSignalRow();
        r.signalId = "0106001001";
        r.signalName = "开关电源信号";
        dict.add(r);

        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> result =
                BInterface2016ReadOnlyRunOnceService.filterByDeviceType(dict, "99", 30);

        assertTrue(result.isEmpty());
    }

    @Test
    void filterByDeviceType_handles_single_digit_type() {
        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> dict = new ArrayList<>();
        BInterface2016ReadOnlyRunOnceService.CsvSignalRow r = new BInterface2016ReadOnlyRunOnceService.CsvSignalRow();
        r.signalId = "0107001001"; // substring(2,4) = "07"
        r.signalName = "设备07信号";
        dict.add(r);

        // "7" should be normalized to "07"
        List<BInterface2016ReadOnlyRunOnceService.CsvSignalRow> result =
                BInterface2016ReadOnlyRunOnceService.filterByDeviceType(dict, "7", 30);

        assertEquals(1, result.size());
        assertEquals("设备07信号", result.get(0).signalName);
    }

    // ==================== DTO structures ====================

    @Test
    void fsuInfoRequest_default_values() {
        FsuInfoRunOnceRequest req = new FsuInfoRunOnceRequest();
        assertEquals("51051243812345", req.getFsuCode());
        assertFalse(req.isConfirmReadOnly());
    }

    @Test
    void fsuInfoResponse_default_readOnly_true() {
        FsuInfoRunOnceResponse resp = new FsuInfoRunOnceResponse();
        assertTrue(resp.isReadOnly());
        assertEquals("real-fsu-get-fsuinfo-2016", resp.getSource());
    }

    @Test
    void getDataProbeRequest_default_values() {
        GetDataProbeRequest req = new GetDataProbeRequest();
        assertEquals("51051243812345", req.getFsuCode());
        assertEquals(30, req.getMaxSignalsPerDevice());
        assertFalse(req.isConfirmReadOnly());
    }

    @Test
    void getDataProbeResponse_default_readOnly_true() {
        GetDataProbeResponse resp = new GetDataProbeResponse();
        assertTrue(resp.isReadOnly());
        assertTrue(resp.getDevices().isEmpty());
    }

    @Test
    void deviceProbeResult_default_confidence_medium() {
        DeviceProbeResult dpr = new DeviceProbeResult();
        assertEquals("medium", dpr.getConfidence());
        assertEquals("protocol-deviceid-inference", dpr.getDeviceTypeSource());
    }

    // ── BIF2016-CONNECTION-003: registration context metadata ──

    @Test
    void probeRequest_default_useRegistrationContext_true() {
        GetDataProbeRequest req = new GetDataProbeRequest();
        assertTrue(req.isUseRegistrationContext()); // default = true (优先注册上下文)
    }

    @Test
    void probeRequest_manual_mode() {
        GetDataProbeRequest req = new GetDataProbeRequest();
        req.setUseRegistrationContext(false);
        assertFalse(req.isUseRegistrationContext());
    }

    @Test
    void probeResponse_targetSource_fields() {
        GetDataProbeResponse resp = new GetDataProbeResponse();
        resp.setTargetSource("login_registration_context");
        resp.setRegistrationContextUsed(true);
        resp.setFsuIpSource("login_registration_context");
        resp.setDeviceListSource("login_registration_context");
        resp.setDeviceCapabilityCount(5);

        assertEquals("login_registration_context", resp.getTargetSource());
        assertTrue(resp.isRegistrationContextUsed());
        assertEquals("login_registration_context", resp.getFsuIpSource());
        assertEquals("login_registration_context", resp.getDeviceListSource());
        assertEquals(5, resp.getDeviceCapabilityCount());
    }

    @Test
    void probeResponse_errorCode_fields() {
        GetDataProbeResponse resp = new GetDataProbeResponse();
        resp.setSuccess(false);
        resp.setTargetSource("login_registration_context");
        resp.setRegistrationContextUsed(false);
        resp.setErrorCode("REGISTRATION_CONTEXT_INCOMPLETE");
        resp.setResultDesc("FsuIP missing from registration context.");

        assertFalse(resp.isSuccess());
        assertEquals("REGISTRATION_CONTEXT_INCOMPLETE", resp.getErrorCode());
        assertFalse(resp.isRegistrationContextUsed());
    }

    @Test
    void probeResponse_manualFallback_fields() {
        GetDataProbeResponse resp = new GetDataProbeResponse();
        resp.setManualFallbackUsed(true);
        resp.setManualFallbackReason("context MISSING");
        resp.setTargetSource("manual_probe");

        assertTrue(resp.isManualFallbackUsed());
        assertEquals("context MISSING", resp.getManualFallbackReason());
        assertEquals("manual_probe", resp.getTargetSource());
    }

    @Test
    void probeResponse_regContext_missingFields() {
        GetDataProbeResponse resp = new GetDataProbeResponse();
        resp.setRegistrationContextCompleteness("PARTIAL");
        resp.setRegistrationContextMissingFields(List.of("FsuIP", "DeviceList"));

        assertEquals("PARTIAL", resp.getRegistrationContextCompleteness());
        assertEquals(2, resp.getRegistrationContextMissingFields().size());
        assertTrue(resp.getRegistrationContextMissingFields().contains("FsuIP"));
    }
}
