package com.dcim.platform.module.binterface.compat;

import com.dcim.platform.module.binterface.model.BInterfaceCommand2024;
import com.dcim.platform.module.binterface.model.BInterfacePkType;

import java.util.Map;
import java.util.Optional;

/**
 * B接口命令旧命名 → 2024 标准命名映射器。
 *
 * <p>BIF-P4-010: 将 2016 协议旧命令名映射到 2024 标准命令。
 * 不删除旧命名，仅提供查找路径。兼容命令标记 {@code compatCommand=true}。</p>
 *
 * <h3>映射规则</h3>
 * <ul>
 *   <li>同名命令 (GET_DATA, LOGIN 等) 直接映射</li>
 *   <li>重命名命令 (TIME_CHECK→SET_TIME) 记录别名</li>
 *   <li>2024 不存在但设备已接受的命令 (GET_THRESHOLD/SET_THRESHOLD) 保留为兼容命令</li>
 * </ul>
 */
public final class BInterfaceCommandAliasMapper {

    private BInterfaceCommandAliasMapper() {}

    // ==================== 旧 PK_Type → 2024 标准命令 ====================

    /** 将旧 BInterfacePkType 映射到 2024 标准命令。 */
    public static Optional<BInterfaceCommand2024> to2024(BInterfacePkType oldType) {
        if (oldType == null) return Optional.empty();
        return to2024(oldType.name());
    }

    /** 将旧命令名字符串映射到 2024 标准命令。 */
    public static Optional<BInterfaceCommand2024> to2024(String oldName) {
        if (oldName == null) return Optional.empty();
        return Optional.ofNullable(OLD_TO_2024.get(oldName));
    }

    /** 判断旧命令是否为兼容命令（2024 标准中不存在或不完全相同）。 */
    public static boolean isCompatCommand(BInterfacePkType oldType) {
        if (oldType == null) return false;
        return isCompatCommand(oldType.name());
    }

    public static boolean isCompatCommand(String oldName) {
        if (oldName == null) return false;
        return COMPAT_ONLY.contains(oldName);
    }

    // ==================== 映射表 ====================

    /** 旧命令名 → 2024 命令名（用于查找）。 */
    private static final Map<String, BInterfaceCommand2024> OLD_TO_2024 = Map.ofEntries(
            // 同名命令
            e("LOGIN",            BInterfaceCommand2024.LOGIN),
            e("GET_DATA",         BInterfaceCommand2024.GET_DATA),
            e("SEND_DATA",        BInterfaceCommand2024.GET_DATA),      // 2024 中 SEND_DATA 移到附录
            e("SEND_ALARM",       BInterfaceCommand2024.SEND_ALARM),
            // 重命名命令
            e("HEARTBEAT",        BInterfaceCommand2024.GET_SUINFO),    // 心跳 → GET_SUINFO
            e("TIME_CHECK",       BInterfaceCommand2024.SET_TIME),      // 时间同步 → SET_TIME
            e("GET_FTP",          BInterfaceCommand2024.GET_SUFTP),     // FTP 查询 → GET_SUFTP
            e("SET_FTP",          BInterfaceCommand2024.SET_SUFTP),     // FTP 设置 → SET_SUFTP
            e("SET_POINT",        BInterfaceCommand2024.SET_RMCTRLCMD), // 遥控遥调 → SET_RMCTRLCMD
            e("GET_FSUINFO",      BInterfaceCommand2024.GET_SUINFO),    // FSU信息 → GET_SUINFO
            e("SET_FSUREBOOT",    BInterfaceCommand2024.SET_SUREBOOT),  // 重启 → SET_SUREBOOT
            e("SET_DATA",         BInterfaceCommand2024.SET_RMCTRLCMD)  // 设置数据 → SET_RMCTRLCMD
    );

    /** 仅在 2016 协议中存在但在 2024 标准表中不存在的兼容命令。 */
    private static final java.util.Set<String> COMPAT_ONLY = java.util.Set.of(
            "GET_THRESHOLD",      // 门限查询：2024 无对应标准命令，但真实设备已接受
            "SET_THRESHOLD",      // 门限设置：2024 无对应标准命令
            "GET_LOGININFO",      // 登录信息查询：2024 无独立命令
            "GET_HISTORY_DATA"    // 历史数据：2024 用 ASK_TODAYHISDATA 替代
    );

    private static Map.Entry<String, BInterfaceCommand2024> e(String key, BInterfaceCommand2024 value) {
        return Map.entry(key, value);
    }
}
