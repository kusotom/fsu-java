package com.dcim.platform.binterface.service.slow;

import com.dcim.platform.module.binterface.service.slow.SlowDataPollingProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlowDataPollingPropertiesTest {

    @Test
    void shouldHaveDefaultEnabledFalse() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertFalse(props.isEnabled());
    }

    @Test
    void shouldHaveDefaultSchedulerEnabledFalse() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertFalse(props.isSchedulerEnabled());
    }

    @Test
    void shouldHaveDefaultAllowRealCallFalse() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertFalse(props.isAllowRealCall());
    }

    @Test
    void shouldHaveDefaultPollGetDataTrue() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertTrue(props.isPollGetData());
    }

    @Test
    void shouldHaveDefaultPollGetThresholdFalse() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertFalse(props.isPollGetThreshold());
    }

    @Test
    void shouldHaveDefaultIntervalSeconds300() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertEquals(300, props.getIntervalSeconds());
    }

    @Test
    void shouldHaveDefaultMaxFsuPerRun50() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        assertEquals(50, props.getMaxFsuPerRun());
    }

    @Test
    void shouldSupportSettingValues() {
        SlowDataPollingProperties props = new SlowDataPollingProperties();
        props.setEnabled(true);
        props.setSchedulerEnabled(true);
        props.setAllowRealCall(true);
        props.setPollGetData(false);
        props.setPollGetThreshold(true);
        props.setIntervalSeconds(600);
        props.setMaxFsuPerRun(100);

        assertTrue(props.isEnabled());
        assertTrue(props.isSchedulerEnabled());
        assertTrue(props.isAllowRealCall());
        assertFalse(props.isPollGetData());
        assertTrue(props.isPollGetThreshold());
        assertEquals(600, props.getIntervalSeconds());
        assertEquals(100, props.getMaxFsuPerRun());
    }
}
