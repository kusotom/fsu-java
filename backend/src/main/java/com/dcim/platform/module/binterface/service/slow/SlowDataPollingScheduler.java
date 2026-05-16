package com.dcim.platform.module.binterface.service.slow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 慢数据通道轮询定时调度器。
 *
 * <p>默认 disabled（scheduler-enabled=false），通过配置启用：
 * <pre>
 * b-interface.slow-polling.scheduler-enabled=true
 * </pre>
 * </p>
 *
 * <p>Scheduler 不包含任何业务逻辑，只触发 {@link SlowDataPollingService#runOnce()}。</p>
 */
@Component
public class SlowDataPollingScheduler {

    private static final Logger log = LoggerFactory.getLogger(SlowDataPollingScheduler.class);

    private final SlowDataPollingService pollingService;
    private final SlowDataPollingProperties properties;

    public SlowDataPollingScheduler(SlowDataPollingService pollingService,
                                    SlowDataPollingProperties properties) {
        this.pollingService = pollingService;
        this.properties = properties;
    }

    /**
     * 定时轮询入口。默认 disabled，仅当 scheduler-enabled=true 且 enabled=true 时触发。
     */
    @Scheduled(fixedDelayString = "#{@slowDataPollingProperties.intervalSeconds * 1000}")
    public void scheduledPoll() {
        if (!properties.isEnabled() || !properties.isSchedulerEnabled()) {
            return;
        }

        log.info("慢数据轮询调度开始");
        try {
            SlowDataPollingResult result = pollingService.runOnce();
            log.info("慢数据轮询调度完成: {}", result);
        } catch (Exception e) {
            log.error("慢数据轮询调度异常", e);
        }
    }
}
