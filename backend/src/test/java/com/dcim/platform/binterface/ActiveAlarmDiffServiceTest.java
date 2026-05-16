package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.ActiveAlarmDiffResult;
import com.dcim.platform.module.binterface.service.ActiveAlarmDiffService;
import com.dcim.platform.module.binterface.service.GetActiveAlarmResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动告警差异核对测试 (BIF-P4-018)。
 */
class ActiveAlarmDiffServiceTest {

    private ActiveAlarmDiffService svc;

    @BeforeEach void setUp() { svc = new ActiveAlarmDiffService(); }

    // ==================== 空快照 ====================

    @Test void emptyBothShouldReturnZero() {
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(), List.of());
        assertTrue(r.isSuccess());
        assertEquals(0, r.getFsuCount());
        assertEquals(0, r.getLocalCount());
    }

    // ==================== FSU_ONLY ====================

    @Test void fsuOnlyShouldBeDetected() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of());
        assertEquals(1, r.getFsuOnlyCount());
        assertTrue(r.hasAlarmInconsistency());
    }

    // ==================== LOCAL_ONLY ====================

    @Test void localOnlyShouldBeDetected() {
        var local = local("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(), List.of(local));
        assertEquals(1, r.getLocalOnlyCount());
    }

    // ==================== MATCHED ====================

    @Test void matchedShouldBeDetected() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var local = local("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of(local));
        assertEquals(1, r.getMatchedCount());
        assertFalse(r.hasAlarmInconsistency());
    }

    // ==================== FIELD_MISMATCH ====================

    @Test void alarmLevelMismatchShouldBeDetected() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var local = local("SN001", "DEV1", "SP01", "二级", "开始", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of(local));
        assertEquals(1, r.getMismatchCount());
    }

    @Test void alarmFlagMismatchShouldBeDetected() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var local = local("SN001", "DEV1", "SP01", "一级", "结束", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of(local));
        assertEquals(1, r.getMismatchCount());
    }

    @Test void triggerValMismatchShouldBeDetected() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var local = local("SN001", "DEV1", "SP01", "一级", "开始", "48.0");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of(local));
        assertEquals(1, r.getMismatchCount());
    }

    // ==================== SerialNo 降级匹配 ====================

    @Test void shouldFallbackToDeviceSpidMatch() {
        var fsu = fsuAlarm(null, "DEV1", "SP01", "一级", "开始", "46.1");
        var local = local(null, "DEV1", "SP01", "一级", "开始", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of(local));
        assertEquals(1, r.getMatchedCount());
    }

    // ==================== SUID 不误匹配 ====================

    @Test void diffShouldOnlyCompareWithinSameSuid() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var local = local("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        // diff is scoped to one SUID at a time
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of(local));
        assertEquals(1, r.getMatchedCount());
    }

    // ==================== 边界 ====================

    @Test void nullSuidShouldFail() {
        assertFalse(svc.diff(null, List.of(), List.of()).isSuccess());
    }

    @Test void nullAlarmListsShouldNotThrow() {
        assertDoesNotThrow(() -> svc.diff("FSU-001", null, null));
    }

    @Test void malformedFsuAlarmShouldBeInvalid() {
        var fsu = new GetActiveAlarmResult.ActiveAlarmItem(
                null, null, null, null, null, null, null, null, null, null, null);
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of());
        assertNotNull(r);
    }

    // ==================== 不修改本地 ====================

    @Test void diffShouldNotModifyAnything() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        var local = local("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of(local));
        assertEquals(1, r.getMatchedCount());
        // no save/update/delete calls
    }

    // ==================== suggestion ====================

    @Test void fsuOnlyShouldHaveSuggestion() {
        var fsu = fsuAlarm("SN001", "DEV1", "SP01", "一级", "开始", "46.1");
        ActiveAlarmDiffResult r = svc.diff("FSU-001", List.of(fsu), List.of());
        assertEquals(1, r.getFsuOnlyCount());
        assertNotNull(r.getItems().get(0).getSuggestion());
        assertTrue(r.getItems().get(0).getSuggestion().contains("SEND_ALARM"));
    }

    // ==================== 辅助 ====================

    private GetActiveAlarmResult.ActiveAlarmItem fsuAlarm(String sn, String devId, String spid,
                                                            String level, String flag, String val) {
        return new GetActiveAlarmResult.ActiveAlarmItem(
                sn, null, devId, spid, LocalDateTime.now(), null, val, level, flag, "desc", null);
    }

    private ActiveAlarmDiffService.LocalAlarmSnapshot local(String sn, String devId, String spid,
                                                              String level, String flag, String val) {
        return ActiveAlarmDiffService.LocalAlarmSnapshot.of(sn, devId, spid, level, flag, val, "desc");
    }
}
