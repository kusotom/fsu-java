package com.dcim.platform.module.binterface.compat;

import java.util.Map;
import java.util.Optional;

/**
 * B接口 2016 协议命令码表。
 *
 * <p>LANDING-006: 真实 FSU 使用 2016 码表 (SEND_ALARM=501 而非 2024 的 601)。
 * 本类提供 2016 命令名→Code 的正向查找。</p>
 *
 * <h3>与 2024 的关键差异</h3>
 * <ul>
 *   <li>GET_DATA: 2016=401, 2024=501</li>
 *   <li>SEND_ALARM: 2016=501, 2024=601</li>
 *   <li>GET_LOGININFO: 2016=1501, 2024 无独立命令</li>
 *   <li>GET_FTP: 2016=1601, 2024=801 (GET_SUFTP)</li>
 *   <li>GET_FSUINFO: 2016=1701, 2024=1001 (GET_SUINFO)</li>
 * </ul>
 *
 * <p>来源: B接口协议 2016 码表，经 LANDING-005 真实 FSU 响应验证。</p>
 */
public final class BInterfaceCommand2016 {

    private BInterfaceCommand2016() {}

    // ==================== 2016 命令码表 ====================

    /** BInterfacePkType 名称 → 2016 Code */
    private static final Map<String, Integer> NAME_TO_CODE_2016 = Map.ofEntries(
            // 实时数据
            e("GET_DATA", 401),
            e("GET_DATA_ACK", 402),
            // 告警
            e("SEND_ALARM", 501),
            e("SEND_ALARM_ACK", 502),
            // 登录信息
            e("GET_LOGININFO", 1501),
            // FTP
            e("GET_FTP", 1601),
            // FSU 信息
            e("GET_FSUINFO", 1701)
    );

    /**
     * 根据 BInterfacePkType 名称查找 2016 Code。
     *
     * @param pkTypeName 命令名 (如 "GET_DATA")
     * @return 2016 Code (如 401)，未找到返回 empty
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

    private static Map.Entry<String, Integer> e(String key, Integer value) {
        return Map.entry(key, value);
    }
}
