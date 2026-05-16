package com.dcim.platform.module.binterface.compat;

import java.util.Map;
import java.util.Optional;

/**
 * B接口字段别名映射器（2016 → 2024 标准字段）。
 *
 * <p>BIF-P4-010: 将旧命名字段 (FSUCode, SignalID) 映射到 2024 标准字段 (SUID, SPID)。
 * 支持双向查找，兼容别名不影响旧字段解析。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 旧→新
 *   String standard = BInterfaceFieldAliasMapper.toStandard("FSUCode");  // "SUID"
 *
 *   // 判断是否为兼容别名
 *   boolean compat = BInterfaceFieldAliasMapper.isAlias("FSUCode");    // true
 * }</pre>
 */
public final class BInterfaceFieldAliasMapper {

    private BInterfaceFieldAliasMapper() {}

    /**
     * 将旧字段名映射到 2024 标准字段名。
     *
     * @param fieldName 旧字段名（大小写不敏感）
     * @return 标准字段名，如果无需映射则返回原值
     */
    public static String toStandard(String fieldName) {
        if (fieldName == null) return null;
        // 精确匹配
        String result = OLD_TO_STANDARD.get(fieldName);
        if (result != null) return result;
        // 大小写不敏感匹配
        for (Map.Entry<String, String> e : OLD_TO_STANDARD.entrySet()) {
            if (e.getKey().equalsIgnoreCase(fieldName)) return e.getValue();
        }
        return fieldName;
    }

    /** 标准字段 → 旧字段（主映射名，如 SUID → FSUCode）。 */
    private static final Map<String, String> STANDARD_TO_LEGACY = Map.of(
            "SUID", "FSUCode",
            "SPID", "SignalID",
            "SUPort", "FSUPort",
            "SUIP", "FSUIP"
    );

    /**
     * 将 2024 标准字段名映射回旧字段名。
     */
    public static Optional<String> toLegacy(String standardName) {
        if (standardName == null) return Optional.empty();
        String result = STANDARD_TO_LEGACY.get(standardName);
        if (result != null) return Optional.of(result);
        return Optional.empty();
    }

    /**
     * 判断字段名是否为需要映射的旧别名。
     */
    public static boolean isAlias(String fieldName) {
        if (fieldName == null) return false;
        return OLD_TO_STANDARD.containsKey(fieldName)
                || OLD_TO_STANDARD.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(fieldName));
    }

    // ==================== 映射表 ====================

    /** 旧字段名（大小写敏感）→ 2024 标准字段名。 */
    private static final Map<String, String> OLD_TO_STANDARD = Map.ofEntries(
            e("FSUCode",    "SUID"),
            e("FsuCode",    "SUID"),
            e("fsu_code",   "SUID"),
            e("SUCode",     "SUID"),
            e("SignalID",   "SPID"),
            e("SignalId",   "SPID"),
            e("signal_id",  "SPID"),
            e("PointID",    "SPID"),
            e("PointCode",  "SPID"),
            e("DeviceID",   "DeviceID"),    // 同名字段，显式列出以表明已审计
            e("DeviceId",   "DeviceID"),
            e("FSUID",      "SUID"),
            e("FsuID",      "SUID"),
            e("FSUPort",    "SUPort"),
            e("FSUIP",      "SUIP")
    );

    private static Map.Entry<String, String> e(String oldField, String standardField) {
        return Map.entry(oldField, standardField);
    }
}
