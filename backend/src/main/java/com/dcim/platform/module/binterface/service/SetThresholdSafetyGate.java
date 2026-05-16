package com.dcim.platform.module.binterface.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * SET_THRESHOLD 安全门禁。
 *
 * <p>SET_THRESHOLD 是高风险配置下发命令，必须通过安全门禁检查：</p>
 * <ul>
 *   <li>{@code b-interface.set-threshold.enabled} — 功能总开关，默认 false</li>
 *   <li>{@code b-interface.set-threshold.require-confirmation} — 是否要求二次确认，默认 true</li>
 * </ul>
 */
@Component
public class SetThresholdSafetyGate {

    private static final Logger log = LoggerFactory.getLogger(SetThresholdSafetyGate.class);

    private final boolean enabled;
    private final boolean requireConfirmation;

    public SetThresholdSafetyGate(
            @Value("${b-interface.set-threshold.enabled:false}") boolean enabled,
            @Value("${b-interface.set-threshold.require-confirmation:true}") boolean requireConfirmation) {
        this.enabled = enabled;
        this.requireConfirmation = requireConfirmation;
        log.info("SetThresholdSafetyGate 初始化: enabled={}, requireConfirmation={}", enabled, requireConfirmation);
    }

    /**
     * 检查 SET_THRESHOLD 操作是否允许执行。
     *
     * @param fsuCode    目标 FSU 编码
     * @param confirmed  用户是否已二次确认
     * @return 安全门禁决策
     */
    public SetThresholdSafetyDecision check(String fsuCode, boolean confirmed) {
        if (!enabled) {
            log.warn("SET_THRESHOLD 安全门禁拒绝: 功能未启用 fsuCode={}", fsuCode);
            return SetThresholdSafetyDecision.deny("3001", "SET_THRESHOLD 功能未启用");
        }

        if (requireConfirmation && !confirmed) {
            log.warn("SET_THRESHOLD 安全门禁拒绝: 缺少二次确认 fsuCode={}", fsuCode);
            return SetThresholdSafetyDecision.deny("3002", "缺少二次确认");
        }

        log.debug("SET_THRESHOLD 安全门禁通过: fsuCode={}", fsuCode);
        return SetThresholdSafetyDecision.allow();
    }
}
