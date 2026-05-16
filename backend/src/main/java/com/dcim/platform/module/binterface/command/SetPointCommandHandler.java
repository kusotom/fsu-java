package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import org.springframework.stereotype.Component;

/**
 * B接口 SET_POINT 命令处理器（桩）。
 *
 * SC 对 FSU 下发遥控/遥调命令（设置点）。
 * 业务逻辑待 BIF-P1 阶段实现。
 */
@Component
public class SetPointCommandHandler implements CommandHandler {

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.SET_POINT;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        return CommandResult.notImplemented(BInterfacePkType.SET_POINT);
    }
}
