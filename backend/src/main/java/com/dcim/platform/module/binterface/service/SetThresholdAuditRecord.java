package com.dcim.platform.module.binterface.service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SET_THRESHOLD 操作审计记录。
 *
 * <p>记录每次 SET_THRESHOLD 操作的完整上下文，用于安全审计和追溯。</p>
 *
 * <p>当前为内存/日志实现，重启后丢失。
 * 后续可扩展为数据库持久化审计。</p>
 */
public class SetThresholdAuditRecord {

    private final String operationId;
    private final String commandType;
    private final String fsuCode;
    private final String signalId;
    private final String alarmUpper;
    private final String alarmLower;
    private final String alarmUpperUrgent;
    private final String alarmLowerUrgent;
    private final boolean confirmed;
    private final String operator;
    private final LocalDateTime requestTime;
    private final LocalDateTime completeTime;
    private final String resultCode;
    private final String resultDesc;
    private final boolean success;
    private final boolean realCall;
    private final List<String> errors;

    private SetThresholdAuditRecord(String operationId, String commandType,
                                    String fsuCode, String signalId,
                                    String alarmUpper, String alarmLower,
                                    String alarmUpperUrgent, String alarmLowerUrgent,
                                    boolean confirmed, String operator,
                                    LocalDateTime requestTime, LocalDateTime completeTime,
                                    String resultCode, String resultDesc,
                                    boolean success, boolean realCall,
                                    List<String> errors) {
        this.operationId = operationId;
        this.commandType = commandType;
        this.fsuCode = fsuCode;
        this.signalId = signalId;
        this.alarmUpper = alarmUpper;
        this.alarmLower = alarmLower;
        this.alarmUpperUrgent = alarmUpperUrgent;
        this.alarmLowerUrgent = alarmLowerUrgent;
        this.confirmed = confirmed;
        this.operator = operator;
        this.requestTime = requestTime;
        this.completeTime = completeTime;
        this.resultCode = resultCode;
        this.resultDesc = resultDesc;
        this.success = success;
        this.realCall = realCall;
        this.errors = errors;
    }

    public static Builder builder() { return new Builder(); }

    // Getters
    public String getOperationId() { return operationId; }
    public String getCommandType() { return commandType; }
    public String getFsuCode() { return fsuCode; }
    public String getSignalId() { return signalId; }
    public String getAlarmUpper() { return alarmUpper; }
    public String getAlarmLower() { return alarmLower; }
    public String getAlarmUpperUrgent() { return alarmUpperUrgent; }
    public String getAlarmLowerUrgent() { return alarmLowerUrgent; }
    public boolean isConfirmed() { return confirmed; }
    public String getOperator() { return operator; }
    public LocalDateTime getRequestTime() { return requestTime; }
    public LocalDateTime getCompleteTime() { return completeTime; }
    public String getResultCode() { return resultCode; }
    public String getResultDesc() { return resultDesc; }
    public boolean isSuccess() { return success; }
    public boolean isRealCall() { return realCall; }
    public List<String> getErrors() { return errors; }

    public static class Builder {
        private String operationId;
        private String commandType = "SET_THRESHOLD";
        private String fsuCode;
        private String signalId;
        private String alarmUpper;
        private String alarmLower;
        private String alarmUpperUrgent;
        private String alarmLowerUrgent;
        private boolean confirmed;
        private String operator = "SYSTEM";
        private LocalDateTime requestTime;
        private LocalDateTime completeTime;
        private String resultCode;
        private String resultDesc;
        private boolean success;
        private boolean realCall;
        private List<String> errors = List.of();

        Builder() {}

        public Builder operationId(String operationId) { this.operationId = operationId; return this; }
        public Builder commandType(String commandType) { this.commandType = commandType; return this; }
        public Builder fsuCode(String fsuCode) { this.fsuCode = fsuCode; return this; }
        public Builder signalId(String signalId) { this.signalId = signalId; return this; }
        public Builder alarmUpper(String alarmUpper) { this.alarmUpper = alarmUpper; return this; }
        public Builder alarmLower(String alarmLower) { this.alarmLower = alarmLower; return this; }
        public Builder alarmUpperUrgent(String alarmUpperUrgent) { this.alarmUpperUrgent = alarmUpperUrgent; return this; }
        public Builder alarmLowerUrgent(String alarmLowerUrgent) { this.alarmLowerUrgent = alarmLowerUrgent; return this; }
        public Builder confirmed(boolean confirmed) { this.confirmed = confirmed; return this; }
        public Builder operator(String operator) { this.operator = operator; return this; }
        public Builder requestTime(LocalDateTime requestTime) { this.requestTime = requestTime; return this; }
        public Builder completeTime(LocalDateTime completeTime) { this.completeTime = completeTime; return this; }
        public Builder resultCode(String resultCode) { this.resultCode = resultCode; return this; }
        public Builder resultDesc(String resultDesc) { this.resultDesc = resultDesc; return this; }
        public Builder success(boolean success) { this.success = success; return this; }
        public Builder realCall(boolean realCall) { this.realCall = realCall; return this; }
        public Builder errors(List<String> errors) { this.errors = errors; return this; }

        public SetThresholdAuditRecord build() {
            return new SetThresholdAuditRecord(operationId, commandType,
                    fsuCode, signalId, alarmUpper, alarmLower,
                    alarmUpperUrgent, alarmLowerUrgent,
                    confirmed, operator, requestTime, completeTime,
                    resultCode, resultDesc, success, realCall, errors);
        }
    }
}
