package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import org.springframework.stereotype.Component;

/**
 * B接口 UNKNOWN 命令兜底处理器。
 *
 * 当 CommandDispatcher 找不到对应 PK_Type 的处理器时，使用此兜底。
 * 返回 ResultCode=1002（INVALID_FSU）。
 */
@Component
public class UnknownCommandHandler implements CommandHandler {

    @Override
    public BInterfacePkType getSupportedPkType() {
        return BInterfacePkType.UNKNOWN;
    }

    @Override
    public CommandResult handle(CommandContext context) {
        return CommandResult.error(BInterfacePkType.UNKNOWN, "1002",
                "未知命令码: " + (context != null && context.getPkType() != null
                        ? context.getPkType() : "null"));
    }
}
