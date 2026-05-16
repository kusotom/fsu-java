package com.dcim.platform.module.binterface.model;

import com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * PK_Type 解析描述符。
 *
 * <p>BIF-P4-011: 支持旧格式 ({@code <PK_Type>GET_DATA</PK_Type>}) 和
 * 2024 标准格式 ({@code <PK_Type><Name>GET_DATA</Name><Code>501</Code></PK_Type>}) 的
 * 双格式解析，包含 Name+Code 校验和旧命令名归一化。</p>
 */
public class PkTypeDescriptor {

    public enum Format {
        /** 旧格式: {@code <PK_Type>GET_DATA</PK_Type>} */
        LEGACY_TEXT,
        /** 2024 格式: Name + Code 双字段 */
        NAME_CODE,
        /** 仅有 Name，无 Code */
        NAME_ONLY,
        /** 仅有 Code，无 Name */
        CODE_ONLY,
        /** 无法识别的格式 */
        INVALID
    }

    private final Format format;
    private final String originalText;        // LEGACY_TEXT 时的原始文本
    private final String originalName;        // NAME_CODE/NAME_ONLY 时的 Name
    private final Integer originalCode;       // NAME_CODE/CODE_ONLY 时的 Code
    private final boolean nameCodeConsistent; // Name 与 Code 是否一致
    private final String validationMessage;

    private final BInterfacePkType legacyPkType;
    private final BInterfaceCommand2024 normalized2024;
    private final boolean compatCommand;
    private final boolean aliasApplied;

    private final List<String> warnings = new ArrayList<>();

    private PkTypeDescriptor(Format format, String originalText, String originalName,
                             Integer originalCode, boolean nameCodeConsistent,
                             String validationMessage,
                             BInterfacePkType legacyPkType, BInterfaceCommand2024 normalized2024,
                             boolean compatCommand, boolean aliasApplied) {
        this.format = format;
        this.originalText = originalText;
        this.originalName = originalName;
        this.originalCode = originalCode;
        this.nameCodeConsistent = nameCodeConsistent;
        this.validationMessage = validationMessage;
        this.legacyPkType = legacyPkType;
        this.normalized2024 = normalized2024;
        this.compatCommand = compatCommand;
        this.aliasApplied = aliasApplied;
    }

    // ==================== 工厂方法 ====================

    /** 旧格式: {@code <PK_Type>GET_DATA</PK_Type>} */
    public static PkTypeDescriptor fromLegacyText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new PkTypeDescriptor(Format.INVALID, text, null, null, false,
                    "PK_Type 为空", null, null, false, false);
        }
        String trimmed = text.trim();
        BInterfacePkType pkType = parseLegacy(trimmed);
        BInterfaceCommand2024 cmd2024 = BInterfaceCommandAliasMapper.to2024(trimmed).orElse(null);
        boolean compat = BInterfaceCommandAliasMapper.isCompatCommand(trimmed);
        boolean applied = cmd2024 != null && !cmd2024.getName().equals(trimmed);
        return new PkTypeDescriptor(Format.LEGACY_TEXT, trimmed, null, null, true, null,
                pkType, cmd2024, compat, applied);
    }

    /** 2024 格式: Name + Code */
    public static PkTypeDescriptor fromNameCode(String name, Integer code) {
        if (name == null && code == null) {
            return new PkTypeDescriptor(Format.INVALID, null, null, null, false,
                    "Name 和 Code 均为空", null, null, false, false);
        }
        if (name != null && code == null) {
            return fromNameOnly(name);
        }
        if (name == null && code != null) {
            return fromCodeOnly(code);
        }
        // Both present
        BInterfaceCommand2024 byName = BInterfaceCommand2024.findByName(name.trim()).orElse(null);
        BInterfaceCommand2024 byCode = BInterfaceCommand2024.findByCode(code).orElse(null);
        boolean consistent = byName != null && byName == byCode;
        String msg = consistent ? null :
                "Name=" + name + " 与 Code=" + code + " 不一致: "
                + (byName != null ? "Name→" + byName.getName() : "Name未知")
                + ", " + (byCode != null ? "Code→" + byCode.getName() : "Code未知");

        BInterfaceCommand2024 normalized = byCode != null ? byCode : byName;
        BInterfacePkType legacy = parseLegacy(name.trim());
        boolean compat = BInterfaceCommandAliasMapper.isCompatCommand(name.trim());
        boolean alias = normalized != null && !normalized.getName().equals(name.trim());

        PkTypeDescriptor d = new PkTypeDescriptor(Format.NAME_CODE, null, name.trim(), code,
                consistent, msg, legacy, normalized, compat, alias);
        if (!consistent) d.warnings.add(msg);
        return d;
    }

    /** 仅有 Name */
    public static PkTypeDescriptor fromNameOnly(String name) {
        String n = name != null ? name.trim() : "";
        BInterfaceCommand2024 cmd = BInterfaceCommand2024.findByName(n).orElse(null);
        String msg = cmd == null ? "未知命令名: " + n : "仅有 Name，缺少 Code";
        BInterfacePkType legacy = parseLegacy(n);
        boolean compat = BInterfaceCommandAliasMapper.isCompatCommand(n);
        PkTypeDescriptor d = new PkTypeDescriptor(Format.NAME_ONLY, null, n, null, true, msg,
                legacy, cmd, compat, false);
        if (cmd == null) d.warnings.add(msg);
        return d;
    }

    /** 仅有 Code */
    public static PkTypeDescriptor fromCodeOnly(int code) {
        BInterfaceCommand2024 cmd = BInterfaceCommand2024.findByCode(code).orElse(null);
        String msg = cmd == null ? "未知命令码: " + code : "仅有 Code，缺少 Name";
        PkTypeDescriptor d = new PkTypeDescriptor(Format.CODE_ONLY, null, null, code, true, msg,
                cmd != null ? parseLegacy(cmd.getName()) : null, cmd, false, false);
        if (cmd == null) d.warnings.add(msg);
        return d;
    }

    // ==================== Getters ====================

    public Format getFormat() { return format; }
    public String getOriginalText() { return originalText; }
    public String getOriginalName() { return originalName; }
    public Integer getOriginalCode() { return originalCode; }
    public boolean isNameCodeConsistent() { return nameCodeConsistent; }
    public String getValidationMessage() { return validationMessage; }
    public BInterfacePkType getLegacyPkType() { return legacyPkType; }
    public Optional<BInterfaceCommand2024> getNormalized2024() { return Optional.ofNullable(normalized2024); }
    public boolean isCompatCommand() { return compatCommand; }
    public boolean isAliasApplied() { return aliasApplied; }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public boolean isValid() { return format != Format.INVALID && legacyPkType != null; }

    /** 获取有效的命令名（优先 2024 标准名，回退旧名）。 */
    public String getEffectiveCommandName() {
        if (normalized2024 != null) return normalized2024.getName();
        if (originalName != null) return originalName;
        return originalText;
    }

    public int getEffectiveCommandCode() {
        if (normalized2024 != null) return normalized2024.getCode();
        return originalCode != null ? originalCode : 0;
    }

    @Override
    public String toString() {
        return "PkTypeDescriptor{format=" + format
                + ", name=" + originalName + ", code=" + originalCode
                + ", consistent=" + nameCodeConsistent
                + ", normalized=" + (normalized2024 != null ? normalized2024.getName() : null)
                + "}";
    }

    // ==================== 内部 ====================

    private static BInterfacePkType parseLegacy(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        try {
            return BInterfacePkType.valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return BInterfacePkType.UNKNOWN;
        }
    }
}
