package com.dcim.platform.module.binterface.service.slow;

import com.dcim.platform.module.binterface.model.BInterfacePkType;

public class SignalPollingTarget {

    private final String fsuCode;
    private final String deviceId;
    private final String signalId;
    private final BInterfacePkType commandType;
    private final boolean enabled;

    public SignalPollingTarget(String fsuCode, String deviceId, String signalId,
                                BInterfacePkType commandType, boolean enabled) {
        this.fsuCode = fsuCode;
        this.deviceId = deviceId;
        this.signalId = signalId;
        this.commandType = commandType;
        this.enabled = enabled;
    }

    public String getFsuCode() { return fsuCode; }
    public String getDeviceId() { return deviceId; }
    public String getSignalId() { return signalId; }
    public BInterfacePkType getCommandType() { return commandType; }
    public boolean isEnabled() { return enabled; }

    @Override
    public String toString() {
        return "SignalPollingTarget{fsuCode=" + fsuCode + ", signalId=" + signalId
                + ", commandType=" + commandType + ", enabled=" + enabled + "}";
    }
}
