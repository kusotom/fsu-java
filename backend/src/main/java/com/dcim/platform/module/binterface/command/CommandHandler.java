package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;

/**
 * B接口命令处理器接口。
 *
 * 所有 PK_Type 对应的业务处理器必须实现此接口。
 * CommandDispatcher 通过 getSupportedPkType() 自动注册路由。
 *
 * 实现约定：
 * - handle() 接收已构建的 {@link CommandContext}，返回 {@link CommandResult}
 * - getSupportedPkType() 返回该处理器负责的命令码
 * - 一个 Handler 只负责一个 PK_Type（UNKNOWN 负责兜底）
 * - Handler 不应当抛出异常，异常由 CommandDispatcher 统一捕获
 *
 * @see CommandDispatcher
 * @see CommandContext
 * @see CommandResult
 */
public interface CommandHandler {

    /**
     * @return 该处理器支持的 PK_Type
     */
    BInterfacePkType getSupportedPkType();

    /**
     * 处理 B接口命令。
     *
     * @param context 命令上下文（含已解析的 pkType / xmlData 等）
     * @return 处理结果（success / resultCode / responseXmlData）
     */
    CommandResult handle(CommandContext context);
}
