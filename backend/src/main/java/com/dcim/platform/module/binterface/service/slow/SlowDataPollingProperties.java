package com.dcim.platform.module.binterface.service.slow;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "b-interface.slow-polling")
public class SlowDataPollingProperties {

    private boolean enabled = false;
    private boolean schedulerEnabled = false;
    private boolean allowRealCall = false;
    private boolean pollGetData = true;
    private boolean pollGetThreshold = false;
    private int intervalSeconds = 300;
    private int maxFsuPerRun = 50;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }

    public boolean isAllowRealCall() { return allowRealCall; }
    public void setAllowRealCall(boolean allowRealCall) { this.allowRealCall = allowRealCall; }

    public boolean isPollGetData() { return pollGetData; }
    public void setPollGetData(boolean pollGetData) { this.pollGetData = pollGetData; }

    public boolean isPollGetThreshold() { return pollGetThreshold; }
    public void setPollGetThreshold(boolean pollGetThreshold) { this.pollGetThreshold = pollGetThreshold; }

    public int getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(int intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    public int getMaxFsuPerRun() { return maxFsuPerRun; }
    public void setMaxFsuPerRun(int maxFsuPerRun) { this.maxFsuPerRun = maxFsuPerRun; }
}
