package com.dcim.platform.module.binterface.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * B接口 2024 标准命令枚举 (YD/T 通信行业标准)。
 *
 * <p>BIF-P4-010: 基于 B接口协议 2024 (PDF + MD 双校验) 表 6 命令全集。
 * 每条命令包含 Name + Code 双标识，符合 2024 协议 PK_Type 结构化格式。</p>
 *
 * <h3>设计原则</h3>
 * <ul>
 *   <li>Name 和 Code 按原 PDF 表 6 逐条录入，双校验通过</li>
 *   <li>不删除现有 {@link BInterfacePkType}，新旧枚举共存</li>
 *   <li>通过 {@code BInterfaceCommandAliasMapper} 做旧→新映射</li>
 *   <li>高风险命令 (SET_*) 标记 {@code highRisk=true}</li>
 * </ul>
 */
public enum BInterfaceCommand2024 {

    // ==================== 网络联接参数设置 ====================
    LOGIN("LOGIN", 101, Direction.FSU_TO_SC, Category.NETWORK, false),
    LOGIN_ACK("LOGIN_ACK", 102, Direction.SC_TO_FSU, Category.NETWORK, false),
    SUREADY("SUREADY", 103, Direction.FSU_TO_SC, Category.NETWORK, false),
    SUREADY_ACK("SUREADY_ACK", 104, Direction.SC_TO_FSU, Category.NETWORK, false),
    SET_SCIP("SET_SCIP", 105, Direction.SC_TO_FSU, Category.NETWORK, false),
    SET_SCIP_ACK("SET_SCIP_ACK", 106, Direction.FSU_TO_SC, Category.NETWORK, false),

    // ==================== 标准化配置文件 ====================
    ASK_SCHEMECONFIG("ASK_SCHEMECONFIG", 201, Direction.FSU_TO_SC, Category.SCHEME_CONFIG, false),
    ASK_SCHEMECONFIG_ACK("ASK_SCHEMECONFIG_ACK", 202, Direction.SC_TO_FSU, Category.SCHEME_CONFIG, false),
    GET_SCHEMECONFIG("GET_SCHEMECONFIG", 203, Direction.SC_TO_FSU, Category.SCHEME_CONFIG, false),
    GET_SCHEMECONFIG_ACK("GET_SCHEMECONFIG_ACK", 204, Direction.FSU_TO_SC, Category.SCHEME_CONFIG, false),
    SET_SCHEMECONFIG("SET_SCHEMECONFIG", 205, Direction.SC_TO_FSU, Category.SCHEME_CONFIG, true),
    SET_SCHEMECONFIG_ACK("SET_SCHEMECONFIG_ACK", 206, Direction.FSU_TO_SC, Category.SCHEME_CONFIG, false),

    // ==================== 厂家配置文件 ====================
    ASK_FACTORYCONFIG("ASK_FACTORYCONFIG", 301, Direction.FSU_TO_SC, Category.FACTORY_CONFIG, false),
    ASK_FACTORYCONFIG_ACK("ASK_FACTORYCONFIG_ACK", 302, Direction.SC_TO_FSU, Category.FACTORY_CONFIG, false),
    SEND_FACTORYCONFIG("SEND_FACTORYCONFIG", 303, Direction.FSU_TO_SC, Category.FACTORY_CONFIG, false),
    SEND_FACTORYCONFIG_ACK("SEND_FACTORYCONFIG_ACK", 304, Direction.SC_TO_FSU, Category.FACTORY_CONFIG, false),
    GET_FACTORYCONFIG("GET_FACTORYCONFIG", 305, Direction.SC_TO_FSU, Category.FACTORY_CONFIG, false),
    GET_FACTORYCONFIG_ACK("GET_FACTORYCONFIG_ACK", 306, Direction.FSU_TO_SC, Category.FACTORY_CONFIG, false),
    SET_FACTORYCONFIG("SET_FACTORYCONFIG", 307, Direction.SC_TO_FSU, Category.FACTORY_CONFIG, true),
    SET_FACTORYCONFIG_ACK("SET_FACTORYCONFIG_ACK", 308, Direction.FSU_TO_SC, Category.FACTORY_CONFIG, false),

    // ==================== FSU 监控点标准化配置模板选型 ====================
    GET_SPCONFIGOPTION("GET_SPCONFIGOPTION", 401, Direction.SC_TO_FSU, Category.SP_CONFIG, false),
    GET_SPCONFIGOPTION_ACK("GET_SPCONFIGOPTION_ACK", 402, Direction.FSU_TO_SC, Category.SP_CONFIG, false),
    SET_SPCONFIGOPTION("SET_SPCONFIGOPTION", 403, Direction.SC_TO_FSU, Category.SP_CONFIG, true),
    SET_SPCONFIGOPTION_ACK("SET_SPCONFIGOPTION_ACK", 404, Direction.FSU_TO_SC, Category.SP_CONFIG, false),

    // ==================== 实时监测数据与当天历史监测数据 ====================
    GET_DATA("GET_DATA", 501, Direction.SC_TO_FSU, Category.REALTIME_DATA, false),
    GET_DATA_ACK("GET_DATA_ACK", 502, Direction.FSU_TO_SC, Category.REALTIME_DATA, false),
    ASK_TODAYHISDATA("ASK_TODAYHISDATA", 503, Direction.SC_TO_FSU, Category.HISTORY_DATA, false),
    ASK_TODAYHISDATA_ACK("ASK_TODAYHISDATA_ACK", 504, Direction.FSU_TO_SC, Category.HISTORY_DATA, false),

    // ==================== 告警信息 ====================
    SEND_ALARM("SEND_ALARM", 601, Direction.FSU_TO_SC, Category.ALARM, false),
    SEND_ALARM_ACK("SEND_ALARM_ACK", 602, Direction.SC_TO_FSU, Category.ALARM, false),
    GET_ACTIVEALARM("GET_ACTIVEALARM", 603, Direction.SC_TO_FSU, Category.ALARM, false),
    GET_ACTIVEALARM_ACK("GET_ACTIVEALARM_ACK", 604, Direction.FSU_TO_SC, Category.ALARM, false),

    // ==================== 控制（遥调遥控）命令 ====================
    SET_RMCTRLCMD("SET_RMCTRLCMD", 701, Direction.SC_TO_FSU, Category.CONTROL, true),
    SET_RMCTRLCMD_ACK("SET_RMCTRLCMD_ACK", 702, Direction.FSU_TO_SC, Category.CONTROL, false),

    // ==================== 获取及设置 FTP 参数 ====================
    GET_SUFTP("GET_SUFTP", 801, Direction.SC_TO_FSU, Category.FTP, false),
    GET_SUFTP_ACK("GET_SUFTP_ACK", 802, Direction.FSU_TO_SC, Category.FTP, false),
    SET_SUFTP("SET_SUFTP", 803, Direction.SC_TO_FSU, Category.FTP, true),
    SET_SUFTP_ACK("SET_SUFTP_ACK", 804, Direction.FSU_TO_SC, Category.FTP, false),

    // ==================== 系统或辅助命令 ====================
    SET_TIME("SET_TIME", 901, Direction.SC_TO_FSU, Category.SYSTEM, false),
    SET_TIME_ACK("SET_TIME_ACK", 902, Direction.FSU_TO_SC, Category.SYSTEM, false),
    GET_SUINFO("GET_SUINFO", 1001, Direction.SC_TO_FSU, Category.SYSTEM, false),
    GET_SUINFO_ACK("GET_SUINFO_ACK", 1002, Direction.FSU_TO_SC, Category.SYSTEM, false),
    SET_SUREBOOT("SET_SUREBOOT", 1101, Direction.SC_TO_FSU, Category.SYSTEM, true),
    SET_SUREBOOT_ACK("SET_SUREBOOT_ACK", 1102, Direction.FSU_TO_SC, Category.SYSTEM, false);

    // ==================== 内部字段 ====================
    private final String name;
    private final int code;
    private final Direction direction;
    private final Category category;
    private final boolean highRisk;

    private static final Map<String, BInterfaceCommand2024> BY_NAME =
            EnumSet.allOf(BInterfaceCommand2024.class).stream()
                    .collect(Collectors.toUnmodifiableMap(BInterfaceCommand2024::getName, Function.identity()));

    private static final Map<Integer, BInterfaceCommand2024> BY_CODE =
            EnumSet.allOf(BInterfaceCommand2024.class).stream()
                    .collect(Collectors.toUnmodifiableMap(BInterfaceCommand2024::getCode, Function.identity()));

    private static final Set<BInterfaceCommand2024> HIGH_RISK_COMMANDS =
            EnumSet.allOf(BInterfaceCommand2024.class).stream()
                    .filter(BInterfaceCommand2024::isHighRisk)
                    .collect(Collectors.toUnmodifiableSet());

    BInterfaceCommand2024(String name, int code, Direction direction, Category category, boolean highRisk) {
        this.name = name;
        this.code = code;
        this.direction = direction;
        this.category = category;
        this.highRisk = highRisk;
    }

    // ==================== Getters ====================
    public String getName() { return name; }
    public int getCode() { return code; }
    public Direction getDirection() { return direction; }
    public Category getCategory() { return category; }
    public boolean isHighRisk() { return highRisk; }

    /** 是否为 ACK（响应）命令。 */
    public boolean isAck() { return name.endsWith("_ACK"); }

    // ==================== 静态查找 ====================

    /** 按 Name 精确查找。 */
    public static Optional<BInterfaceCommand2024> findByName(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(BY_NAME.get(name));
    }

    /** 按 Code 精确查找。 */
    public static Optional<BInterfaceCommand2024> findByCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }

    /** 获取所有高风险命令。 */
    public static Set<BInterfaceCommand2024> getHighRiskCommands() {
        return HIGH_RISK_COMMANDS;
    }

    /** 命令总数。 */
    public static int commandCount() {
        return values().length;
    }

    // ==================== 子类型 ====================

    public enum Direction {
        FSU_TO_SC,
        SC_TO_FSU
    }

    public enum Category {
        NETWORK,
        SCHEME_CONFIG,
        FACTORY_CONFIG,
        SP_CONFIG,
        REALTIME_DATA,
        HISTORY_DATA,
        ALARM,
        CONTROL,
        FTP,
        SYSTEM
    }
}
