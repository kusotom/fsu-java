package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.compat.BInterfaceFieldAliasMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * FSU 活动告警 vs 本地 alarm_record 差异核对服务 (BIF-P4-018)。
 *
 * <p>只读审计，不修改本地告警状态。</p>
 */
@Service
public class ActiveAlarmDiffService {

    private static final Logger log = LoggerFactory.getLogger(ActiveAlarmDiffService.class);

    /**
     * 对比 FSU 活动告警快照与本地活动告警。
     *
     * @param suid        FSU 编码
     * @param fsuAlarms   GET_ACTIVEALARM 返回的 FSU 活动告警
     * @param localAlarms alarm_record 中该 FSU 的活动告警
     * @return 差异结果
     */
    public ActiveAlarmDiffResult diff(String suid,
                                       List<GetActiveAlarmResult.ActiveAlarmItem> fsuAlarms,
                                       List<LocalAlarmSnapshot> localAlarms) {
        if (suid == null || suid.trim().isEmpty())
            return ActiveAlarmDiffResult.fail(null, "缺少 SUID");
        suid = suid.trim();

        List<ActiveAlarmDiffResult.DiffItem> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        List<GetActiveAlarmResult.ActiveAlarmItem> fsuList = fsuAlarms != null ? fsuAlarms : List.of();
        List<LocalAlarmSnapshot> localList = localAlarms != null ? localAlarms : List.of();

        // 索引化本地告警
        Map<String, LocalAlarmSnapshot> localBySerialNo = new HashMap<>();
        Map<String, LocalAlarmSnapshot> localByKey = new HashMap<>();
        for (LocalAlarmSnapshot la : localList) {
            if (la.serialNo != null && !la.serialNo.isEmpty()) localBySerialNo.put(la.serialNo, la);
            localByKey.put(matchKey(la.deviceId, la.spid, la.alarmFlag), la);
        }
        Set<String> matchedLocalKeys = new HashSet<>();

        int matched = 0;

        for (GetActiveAlarmResult.ActiveAlarmItem fa : fsuList) {
            if (fa.getSerialNo() == null && fa.getSpid() == null) {
                items.add(new ActiveAlarmDiffResult.DiffItem(
                        ActiveAlarmDiffResult.DiffType.INVALID, null, null, null, null, null,
                        "FSU alarm fields missing", null, List.of(), "核查 FSU 告警格式"));
                continue;
            }

            // 1. SerialNo 匹配
            LocalAlarmSnapshot la = null;
            if (fa.getSerialNo() != null && !fa.getSerialNo().isEmpty()) {
                la = localBySerialNo.get(fa.getSerialNo());
            }
            // 2. SUID + DeviceID + SPID + AlarmFlag 降级
            if (la == null) {
                String key = matchKey(fa.getDeviceId(), fa.getSpid(),
                        fa.getAlarmFlag());
                la = localByKey.get(key);
            }

            if (la == null) {
                items.add(buildDiff(ActiveAlarmDiffResult.DiffType.FSU_ONLY, fa, null, warnings));
                continue;
            }
            matchedLocalKeys.add(matchKey(la.deviceId, la.spid, la.alarmFlag));

            // 字段比较
            List<String> mismatches = compareFields(fa, la);
            if (mismatches.isEmpty()) {
                items.add(buildDiff(ActiveAlarmDiffResult.DiffType.MATCHED, fa, la, warnings));
                matched++;
            } else {
                items.add(new ActiveAlarmDiffResult.DiffItem(
                        ActiveAlarmDiffResult.DiffType.FIELD_MISMATCH,
                        fa.getSerialNo(), fa.getDeviceId(), fa.getSpid(),
                        fa.getAlarmLevel(), fa.getAlarmFlag(),
                        summary(fa), summary(la), mismatches,
                        "差异字段: " + String.join(",", mismatches)));
            }
        }

        // LOCAL_ONLY
        int localOnly = 0;
        for (LocalAlarmSnapshot la : localList) {
            String key = matchKey(la.deviceId, la.spid, la.alarmFlag);
            if (!matchedLocalKeys.contains(key)) {
                items.add(new ActiveAlarmDiffResult.DiffItem(
                        ActiveAlarmDiffResult.DiffType.LOCAL_ONLY,
                        la.serialNo, la.deviceId, la.spid, la.alarmLevel, la.alarmFlag,
                        null, summary(la), List.of(),
                        "本地有活动告警但 FSU 快照中不存在，核查是否恢复报文丢失"));
                localOnly++;
            }
        }

        int fsuOnly = count(items, ActiveAlarmDiffResult.DiffType.FSU_ONLY);
        int mismatch = count(items, ActiveAlarmDiffResult.DiffType.FIELD_MISMATCH);

        return ActiveAlarmDiffResult.of(suid, fsuList.size(), localList.size(),
                matched, fsuOnly, localOnly, mismatch, items, warnings);
    }

    // ==================== 内部 ====================

    private String matchKey(String deviceId, String spid, String alarmFlag) {
        return (deviceId != null ? deviceId : "") + "|"
                + (spid != null ? spid : "") + "|"
                + (alarmFlag != null ? alarmFlag : "");
    }

    private List<String> compareFields(GetActiveAlarmResult.ActiveAlarmItem fa, LocalAlarmSnapshot la) {
        List<String> m = new ArrayList<>();
        if (!eq(fa.getAlarmLevel(), la.alarmLevel)) m.add("AlarmLevel");
        if (!eq(fa.getAlarmFlag(), la.alarmFlag)) m.add("AlarmFlag");
        if (!eq(fa.getTriggerVal(), la.triggerVal)) m.add("TriggerVal");
        if (!eq(fa.getDeviceId(), la.deviceId)) m.add("DeviceID");
        if (!eq(fa.getSpid(), la.spid)) m.add("SPID");
        if (!eq(fa.getAlarmDesc(), la.alarmDesc)) m.add("AlarmDesc");
        return m;
    }

    private boolean eq(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private ActiveAlarmDiffResult.DiffItem buildDiff(ActiveAlarmDiffResult.DiffType type,
                                                      GetActiveAlarmResult.ActiveAlarmItem fa,
                                                      LocalAlarmSnapshot la,
                                                      List<String> warnings) {
        String sug;
        if (type == ActiveAlarmDiffResult.DiffType.FSU_ONLY) sug = "FSU 有活动告警但本地不存在，核查是否漏报 SEND_ALARM";
        else if (type == ActiveAlarmDiffResult.DiffType.MATCHED) sug = null;
        else sug = "";
        return new ActiveAlarmDiffResult.DiffItem(type,
                fa.getSerialNo(), fa.getDeviceId(), fa.getSpid(),
                fa.getAlarmLevel(), fa.getAlarmFlag(),
                summary(fa), la != null ? summary(la) : null, List.of(), sug);
    }

    private String summary(GetActiveAlarmResult.ActiveAlarmItem a) {
        if (a == null) return null;
        return "SerialNo=" + a.getSerialNo() + " SPID=" + a.getSpid()
                + " Level=" + a.getAlarmLevel() + " Flag=" + a.getAlarmFlag()
                + " Val=" + a.getTriggerVal();
    }

    private String summary(LocalAlarmSnapshot a) {
        if (a == null) return null;
        return "SerialNo=" + a.serialNo + " SPID=" + a.spid
                + " Level=" + a.alarmLevel + " Flag=" + a.alarmFlag + " Val=" + a.triggerVal;
    }

    private int count(List<ActiveAlarmDiffResult.DiffItem> items, ActiveAlarmDiffResult.DiffType type) {
        return (int) items.stream().filter(i -> i.getType() == type).count();
    }

    /** 本地告警快照（只读投影）。 */
    public static class LocalAlarmSnapshot {
        public final String serialNo, deviceId, spid, alarmLevel, alarmFlag, triggerVal, alarmDesc;
        public LocalAlarmSnapshot(String serialNo, String deviceId, String spid,
                                   String alarmLevel, String alarmFlag, String triggerVal, String alarmDesc) {
            this.serialNo = serialNo; this.deviceId = deviceId; this.spid = spid;
            this.alarmLevel = alarmLevel; this.alarmFlag = alarmFlag;
            this.triggerVal = triggerVal; this.alarmDesc = alarmDesc;
        }

        public static LocalAlarmSnapshot of(String serialNo, String deviceId, String spid,
                                              String alarmLevel, String alarmFlag, String triggerVal, String alarmDesc) {
            return new LocalAlarmSnapshot(serialNo, deviceId, spid, alarmLevel, alarmFlag, triggerVal, alarmDesc);
        }
    }
}
