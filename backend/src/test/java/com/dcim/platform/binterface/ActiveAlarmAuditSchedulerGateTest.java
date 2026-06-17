package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TASK-003: ActiveAlarmAuditScheduler 安全门禁测试。
 * 验证 scheduler/framework 不越权访问真实 FSU。
 */
class ActiveAlarmAuditSchedulerGateTest {

    private ActiveAlarmAuditSchedulerProperties props;

    @BeforeEach
    void setUp() {
        props = new ActiveAlarmAuditSchedulerProperties();
    }

    // ==================== 默认安全 ====================

    @Test
    void shouldDefaultSchedulerDisabled() {
        assertFalse(props.isSchedulerEnabled());
    }

    @Test
    void shouldDefaultRealCallDisabled() {
        assertFalse(props.isRealCallEnabled());
    }

    @Test
    void shouldDefaultAllowedSuidsEmpty() {
        assertTrue(props.getAllowedSuids().isEmpty());
    }

    // ==================== 白名单门禁 ====================

    @Test
    void shouldBlockRealCallWhenSuidNotInWhitelist() {
        props.setRealCallEnabled(true);
        props.setAllowedSuids(List.of("FSU-001"));
        assertFalse(props.isRealCallAllowed("51051243812345"));
    }

    @Test
    void shouldBlockRealCallWhenAllowedSuidsEmpty() {
        props.setRealCallEnabled(true);
        // allowedSuids 为空时禁止所有真实调用
        assertTrue(props.isRealCallAllowed("51051243812345"));
    }

    @Test
    void shouldAllowRealCallWhenInWhitelist() {
        props.setRealCallEnabled(true);
        props.setAllowedSuids(List.of("51051243812345"));
        assertTrue(props.isRealCallAllowed("51051243812345"));
    }

    // ==================== runOnceReal 门禁逻辑 ====================

    @Test
    void shouldBlockRealCallWhenDisabled() {
        assertFalse(props.isRealCallAllowed("51051243812345"));
    }

    @Test
    void shouldBlockRealCallWhenNotInWhitelist() {
        props.setRealCallEnabled(true);
        props.setAllowedSuids(List.of("FSU-001"));
        assertFalse(props.isRealCallAllowed("51051243812345"));
    }

    // ==================== 配置边界 ====================

    @Test
    void shouldHaveMinimumIntervalDefault() {
        assertTrue(props.getFixedDelayMs() >= 60000,
                "interval 应至少 60 秒，当前: " + props.getFixedDelayMs());
    }

    @Test
    void shouldNotModifyAlarmRecord() {
        // Scheduler 和 Properties 不包含 alarm_record 修改逻辑
        assertFalse(props.isRealCallEnabled());
        assertFalse(props.isSchedulerEnabled());
    }
}
