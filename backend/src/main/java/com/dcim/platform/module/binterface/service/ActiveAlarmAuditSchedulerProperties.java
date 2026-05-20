package com.dcim.platform.module.binterface.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 活动告警审计 Scheduler 配置属性 (BIF-P4-021)。
 *
 * <p>所有默认值均为安全值——默认关闭，不访问真实设备。</p>
 */
@Component
@ConfigurationProperties(prefix = "b-interface.active-alarm-audit")
public class ActiveAlarmAuditSchedulerProperties {

    /** Scheduler 是否启用，默认 false */
    private boolean schedulerEnabled = false;
    /** 定时延迟（毫秒），默认 300000 (5分钟) */
    private long fixedDelayMs = 300_000;
    /** 初始延迟（毫秒），默认 60000 (1分钟) */
    private long initialDelayMs = 60_000;
    /** 目标 FSU 编码 */
    private String fsuCode;

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean v) { this.schedulerEnabled = v; }

    public long getFixedDelayMs() { return fixedDelayMs; }
    public void setFixedDelayMs(long v) { this.fixedDelayMs = v; }

    public long getInitialDelayMs() { return initialDelayMs; }
    public void setInitialDelayMs(long v) { this.initialDelayMs = v; }

    public String getFsuCode() { return fsuCode; }
    public void setFsuCode(String v) { this.fsuCode = v; }
}
