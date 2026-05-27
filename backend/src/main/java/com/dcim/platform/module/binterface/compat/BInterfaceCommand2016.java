package com.dcim.platform.module.binterface.compat;

import java.util.Map;
import java.util.Optional;

/**
 * B接口 2016 协议命令码表。
 *
 * <p>LANDING-006: 真实 FSU 使用 2016 码表 (SEND_ALARM=501 而非 2024 的 601)。
 * BIF2016-P1-001: 补齐全部 15 命令 + 15 ACK。</p>
 *
 * <p>来源: B接口协议 2016 码表，经真实 FSU 响应验证。</p>
 */
public final class BInterfaceCommand2016 {

    private BInterfaceCommand2016() {}

    // ==================== 2016 全命令码表 (BIF2016-P1-001) ====================

    /** BInterfacePkType 名称 → 2016 Code */
    private static final Map<String, Integer> NAME_TO_CODE_2016 = Map.ofEntries(
            // === 连接管理 ===
            e("LOGIN", 101),
            e("LOGIN_ACK", 102),
            e("LOGOUT", 103),
            e("LOGOUT_ACK", 104),

            // === 实时数据 ===
            e("GET_DATA", 401),
            e("GET_DATA_ACK", 402),
            e("GET_HISDATA", 403),
            e("GET_HISDATA_ACK", 404),

            // === 告警 ===
            e("SEND_ALARM", 501),
            e("SEND_ALARM_ACK", 502),

            // === 遥控遥调 ===
            e("SET_POINT", 1001),
            e("SET_POINT_ACK", 1002),

            // === 时间同步 ===
            e("TIME_CHECK", 1301),
            e("TIME_CHECK_ACK", 1302),

            // === 登录信息 ===
            e("GET_LOGININFO", 1501),
            e("GET_LOGININFO_ACK", 1502),
            e("SET_LOGININFO", 1503),
            e("SET_LOGININFO_ACK", 1504),

            // === FTP ===
            e("GET_FTP", 1601),
            e("GET_FTP_ACK", 1602),
            e("SET_FTP", 1603),
            e("SET_FTP_ACK", 1604),

            // === FSU 信息 ===
            e("GET_FSUINFO", 1701),
            e("GET_FSUINFO_ACK", 1702),

            // === 远程控制 ===
            e("SET_FSUREBOOT", 1801),
            e("SET_FSUREBOOT_ACK", 1802),

            // === 门限 ===
            e("GET_THRESHOLD", 1901),
            e("GET_THRESHOLD_ACK", 1902),
            e("SET_THRESHOLD", 2001),
            e("SET_THRESHOLD_ACK", 2002)
    );

    /**
     * 根据 BInterfacePkType 名称查找 2016 Code。
     *
     * @param pkTypeName 命令名 (如 "GET_DATA", "GET_DATA_ACK")
     * @return 2016 Code，未找到返回 empty
     */
    public static Optional<Integer> codeFor(String pkTypeName) {
        if (pkTypeName == null) return Optional.empty();
        return Optional.ofNullable(NAME_TO_CODE_2016.get(pkTypeName));
    }

    /**
     * 判断给定命令名是否在 2016 码表中有定义。
     */
    public static boolean isDefined(String pkTypeName) {
        return pkTypeName != null && NAME_TO_CODE_2016.containsKey(pkTypeName);
    }

    /**
     * 返回 2016 码表中的命令总数。
     */
    public static int size() {
        return NAME_TO_CODE_2016.size();
    }

    private static Map.Entry<String, Integer> e(String key, Integer value) {
        return Map.entry(key, value);
    }
}
