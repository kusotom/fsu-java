package com.dcim.platform.binterface.service.slow;

import com.dcim.platform.module.binterface.service.slow.SlowDataPollingProperties;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingResult;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingScheduler;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlowDataPollingSchedulerTest {

    private SlowDataPollingProperties properties;
    private SlowDataPollingService pollingService;
    private SlowDataPollingScheduler scheduler;

    @BeforeEach
    void setUp() {
        properties = new SlowDataPollingProperties();
        pollingService = new SlowDataPollingService(properties, null, null, null, null, null) {
            @Override
            public SlowDataPollingResult runOnce() {
                return SlowDataPollingResult.completed(0, 0, 0, 0, 0, 0, 0, 0,
                        java.util.List.of(), java.util.List.of(), false);
            }
        };
        scheduler = new SlowDataPollingScheduler(pollingService, properties);
    }

    @Test
    void shouldNotRunWhenDisabled() {
        properties.setEnabled(false);
        properties.setSchedulerEnabled(true);

        scheduler.scheduledPoll();
    }

    @Test
    void shouldNotRunWhenSchedulerDisabled() {
        properties.setEnabled(true);
        properties.setSchedulerEnabled(false);

        scheduler.scheduledPoll();
    }

    @Test
    void shouldRunWhenBothEnabled() {
        properties.setEnabled(true);
        properties.setSchedulerEnabled(true);

        scheduler.scheduledPoll();
    }

    @Test
    void shouldNotThrowWhenRunOnceFails() {
        properties.setEnabled(true);
        properties.setSchedulerEnabled(true);
        pollingService = new SlowDataPollingService(properties, null, null, null, null, null) {
            @Override
            public SlowDataPollingResult runOnce() {
                throw new RuntimeException("模拟异常");
            }
        };
        scheduler = new SlowDataPollingScheduler(pollingService, properties);

        scheduler.scheduledPoll();
    }
}
