package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import org.springframework.stereotype.Component;

/**
 * B接口 SET_FTP 命令处理器（桩）。
 *
 * SC 设置 FSU 的 FTP 配置参数。
 * 业务逻辑待 BIF-P1 阶段实现。
 */
@Component
public class SetFtpCommandHandler implements CommandHandler {

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.SET_FTP;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        return CommandResult.notImplemented(BInterfacePkType.SET_FTP);
    }
}
