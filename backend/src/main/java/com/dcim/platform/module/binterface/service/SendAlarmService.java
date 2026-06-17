package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.mapping.service.UnmappedSignalObservationService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SEND_ALARM 告警上报服务。
 *
 * 职责：
 * <ul>
 *   <li>解析 XmlDataModel 中的 Alarm 项列表</li>
 *   <li>校验每项 SignalID / AlarmCode 必填</li>
 *   <li>区分告警产生（AlarmType=0）与告警恢复（AlarmType=1）</li>
 *   <li>通过 {@link AlarmRecordRepository} 保存告警记录或更新状态</li>
 *   <li>统计 acceptedCount / recoveredCount / rejectedCount</li>
 * </ul>
 */
@Service
public class SendAlarmService {

    private static final Logger log = LoggerFactory.getLogger(SendAlarmService.class);

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    public static final String ALARM_TYPE_GENERATE = "0";
    public static final String ALARM_TYPE_RECOVER = "1";
    public static final String ALARM_STATUS_ACTIVE = "ACTIVE";
    public static final String ALARM_STATUS_RECOVERED = "RECOVERED";

    private final FsuDeviceRepository fsuDeviceRepository;
    private final AlarmRecordRepository alarmRecordRepository;
    private final EStoneIIMappingService mappingService;
    private final UnmappedSignalObservationService unmappedService;

    @Autowired
    public SendAlarmService(FsuDeviceRepository fsuDeviceRepository,
                            AlarmRecordRepository alarmRecordRepository,
                            EStoneIIMappingService mappingService,
                            UnmappedSignalObservationService unmappedService) {
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.alarmRecordRepository = alarmRecordRepository;
        this.mappingService = mappingService;
        this.unmappedService = unmappedService;
    }

    public SendAlarmService(FsuDeviceRepository fsuDeviceRepository,
                            AlarmRecordRepository alarmRecordRepository) {
        this(fsuDeviceRepository, alarmRecordRepository, null, null);
    }

    /**
     * 处理 SEND_ALARM 告警上报。
     *
     * @param fsuCode      FSU 编码
     * @param alarmTimeStr 采集时间字符串（Info.AlarmTime，可选，ISO8601）
     * @param xmlData      已解析的 xmlData 模型
     * @return 处理结果
     */
    @Transactional
    public SendAlarmResult processAlarms(String fsuCode, String alarmTimeStr, XmlDataModel xmlData) {
        // 1. 查找 FSU 设备
        Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepository.findByFsuCode(fsuCode);
        if (fsuOpt.isEmpty()) {
            log.warn("SEND_ALARM 失败: FSU 未注册 fsuCode={}", fsuCode);
            return SendAlarmResult.fail("1002", "FSU 未注册: " + fsuCode);
        }
        Long fsuId = fsuOpt.get().getId();

        // 2. 解析告警时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime alarmTime = parseAlarmTime(alarmTimeStr, now);

        // 3. 获取 Alarm 项列表
        if (xmlData == null) {
            log.warn("SEND_ALARM 失败: xmlData 为空 fsuCode={}", fsuCode);
            return SendAlarmResult.fail("2003", "无有效告警数据");
        }
        List<Map<String, String>> items = xmlData.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("SEND_ALARM 失败: 无 Alarm 项 fsuCode={}", fsuCode);
            return SendAlarmResult.fail("2003", "无有效告警数据");
        }

        // 4. 逐项处理
        int accepted = 0;
        int recovered = 0;
        int rejected = 0;
        List<String> errors = new ArrayList<>();
        List<Long> alarmIds = new ArrayList<>();

        for (Map<String, String> item : items) {
            try {
                AlarmParseResult parsed = parseAlarmItem(item);
                if (!parsed.valid) {
                    rejected++;
                    errors.add(parsed.error);
                    continue;
                }

                // 处理告警（产生或恢复）
                AlarmRecordEntity saved = processOneAlarm(fsuCode, fsuId, parsed, alarmTime, now);
                if (parsed.isRecover) {
                    recovered++;
                } else {
                    accepted++;
                }
                alarmIds.add(saved.getId());

            } catch (Exception e) {
                rejected++;
                errors.add("Alarm处理异常: " + e.getMessage());
            }
        }

        if (accepted == 0 && recovered == 0 && rejected > 0) {
            return SendAlarmResult.fail("2003", "无有效告警数据", fsuCode);
        }

        if (rejected > 0) {
            return SendAlarmResult.partial(fsuCode, accepted, recovered, rejected, errors, alarmIds);
        }

        return SendAlarmResult.success(fsuCode, accepted, recovered, rejected, alarmIds);
    }

    // ==================== 内部方法 ====================

    private AlarmParseResult parseAlarmItem(Map<String, String> item) {
        String signalId = null;
        String alarmCode = null;
        String alarmName = null;
        String alarmLevel = null;
        String alarmValue = null;
        String alarmDesc = null;
        String alarmType = null;
        String serialNo = null;
        String deviceId = null;
        String spid = null;
        String deviceCode = null;
        String alarmFlag = null;

        for (Map.Entry<String, String> entry : item.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase("SignalID")) {
                signalId = entry.getValue();
            } else if (key.equalsIgnoreCase("SerialNo")) {
                serialNo = entry.getValue();
            } else if (key.equalsIgnoreCase("DeviceID")) {
                deviceId = entry.getValue();
            } else if (key.equalsIgnoreCase("SPID") || key.equalsIgnoreCase("ID")) {
                spid = entry.getValue();
            } else if (key.equalsIgnoreCase("DeviceCode")) {
                deviceCode = entry.getValue();
            } else if (key.equalsIgnoreCase("AlarmFlag")) {
                alarmFlag = entry.getValue();
            } else if (key.equalsIgnoreCase("AlarmCode")) {
                alarmCode = entry.getValue();
            } else if (key.equalsIgnoreCase("AlarmName")) {
                alarmName = entry.getValue();
            } else if (key.equalsIgnoreCase("AlarmLevel")) {
                alarmLevel = entry.getValue();
            } else if (key.equalsIgnoreCase("AlarmValue")) {
                alarmValue = entry.getValue();
            } else if (key.equalsIgnoreCase("AlarmDesc")) {
                alarmDesc = entry.getValue();
            } else if (key.equalsIgnoreCase("AlarmType")) {
                alarmType = entry.getValue();
            }
        }

        // 2016 兼容: ID 可作为 SignalID 备选
        if (signalId == null && spid != null) {
            signalId = spid;
        }

        if (signalId == null || signalId.trim().isEmpty()) {
            return new AlarmParseResult(false, null, null, null, null, null, null, null,
                    null, null, null, null, null, false, "缺少 SignalID");
        }
        if (alarmCode == null || alarmCode.trim().isEmpty()) {
            return new AlarmParseResult(false, signalId, null, null, null, null, null, null,
                    null, null, null, null, null, false,
                    "SignalID=" + signalId + " 缺少 AlarmCode");
        }
        if (alarmLevel == null || alarmLevel.trim().isEmpty()) {
            return new AlarmParseResult(false, signalId, alarmCode, null, null, null, null, null,
                    null, null, null, null, null, false,
                    "SignalID=" + signalId + " 缺少 AlarmLevel");
        }

        boolean isRecover = ALARM_TYPE_RECOVER.equals(alarmType)
                || (!ALARM_TYPE_GENERATE.equals(alarmType) && "1".equals(alarmFlag));
        return new AlarmParseResult(true, signalId.trim(), alarmCode.trim(),
                alarmName != null ? alarmName.trim() : null,
                alarmLevel.trim(),
                alarmValue != null ? alarmValue.trim() : null,
                alarmDesc != null ? alarmDesc.trim() : null,
                alarmType != null ? alarmType.trim() : ALARM_TYPE_GENERATE,
                serialNo != null ? serialNo.trim() : null,
                deviceId != null ? deviceId.trim() : null,
                spid != null ? spid.trim() : null,
                deviceCode != null ? deviceCode.trim() : null,
                alarmFlag != null ? alarmFlag.trim() : null,
                isRecover, null);
    }

    private AlarmRecordEntity processOneAlarm(String fsuCode, Long fsuId, AlarmParseResult parsed,
                                               LocalDateTime alarmTime, LocalDateTime now) {
        if (parsed.isRecover) {
            // 告警恢复：查找已有 ACTIVE 告警，更新状态
            Optional<AlarmRecordEntity> activeOpt = alarmRecordRepository
                    .findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus(
                            fsuId, parsed.signalId, parsed.alarmCode, ALARM_STATUS_ACTIVE);
            if (activeOpt.isPresent()) {
                AlarmRecordEntity existing = activeOpt.get();
                existing.setAlarmStatus(ALARM_STATUS_RECOVERED);
                existing.setAlarmValue(parsed.alarmValue);
                existing.setAlarmDesc(parsed.alarmDesc);
                existing.setClearTime(now);
                existing.setUpdatedAt(now);
                // LANDING-008: 补写 SerialNo/DeviceID/SPID（兼容旧数据）
                if (existing.getSerialNo() == null && parsed.serialNo != null) {
                    existing.setSerialNo(parsed.serialNo);
                }
                if (existing.getDeviceId() == null && parsed.deviceId != null) {
                    existing.setDeviceId(parsed.deviceId);
                }
                if (existing.getSpid() == null && parsed.spid != null) {
                    existing.setSpid(parsed.spid);
                }
                return alarmRecordRepository.save(existing);
            } else {
                // 未找到原始告警，以恢复状态创建记录
                log.warn("SEND_ALARM 恢复: 未找到 ACTIVE 告警 fsuId={} signalId={} alarmCode={}",
                        fsuId, parsed.signalId, parsed.alarmCode);
                AlarmRecordEntity entity = buildAlarmEntity(fsuCode, fsuId, parsed, alarmTime, now);
                entity.setAlarmStatus(ALARM_STATUS_RECOVERED);
                entity.setClearTime(now);
                return alarmRecordRepository.save(entity);
            }
        } else {
            // 告警产生：创建新记录
            AlarmRecordEntity entity = buildAlarmEntity(fsuCode, fsuId, parsed, alarmTime, now);
            entity.setAlarmStatus(ALARM_STATUS_ACTIVE);
            return alarmRecordRepository.save(entity);
        }
    }

    private AlarmRecordEntity buildAlarmEntity(String fsuCode, Long fsuId, AlarmParseResult parsed,
                                                LocalDateTime alarmTime, LocalDateTime now) {
        AlarmRecordEntity entity = new AlarmRecordEntity();
        entity.setFsuId(fsuId);
        entity.setPointCode(parsed.signalId);
        entity.setSerialNo(parsed.serialNo);
        entity.setDeviceId(parsed.deviceId);
        entity.setSpid(parsed.spid);
        entity.setAlarmCode(parsed.alarmCode);
        EStoneIIMappingResult mapped = resolveAlarmMapping(fsuCode, parsed);
        entity.setAlarmName(parsed.alarmName != null ? parsed.alarmName
                : (mapped != null && mapped.eventName() != null ? mapped.eventName() : mapped != null ? mapped.signalName() : null));
        entity.setAlarmLevel(parsed.alarmLevel);
        entity.setAlarmValue(parsed.alarmValue);
        // 2016 DeviceCode 写入 alarmDesc 补充（不覆盖原始描述）
        String descWithDeviceCode = parsed.alarmDesc;
        if (parsed.deviceCode != null && !parsed.deviceCode.isEmpty()) {
            descWithDeviceCode = (descWithDeviceCode != null ? descWithDeviceCode : "")
                    + " [DeviceCode=" + parsed.deviceCode + "]";
        }
        entity.setAlarmDesc(descWithDeviceCode);
        entity.setOccurTime(alarmTime);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        if (mapped != null && (!mapped.mapped() || mapped.reason() != null)) {
            recordUnmappedAlarm(fsuCode, parsed, mapped);
        }
        return entity;
    }

    private EStoneIIMappingResult resolveAlarmMapping(String fsuCode, AlarmParseResult parsed) {
        if (mappingService == null) return null;
        return mappingService.resolveAlarm(fsuCode, parsed.deviceId, parsed.deviceCode,
                parsed.spid, parsed.signalId, parsed.alarmCode, parsed.alarmValue);
    }

    private void recordUnmappedAlarm(String fsuCode, AlarmParseResult parsed, EStoneIIMappingResult mapped) {
        if (unmappedService == null) return;
        String rawId = "UNKNOWN_EVENT_ID".equals(mapped.reason())
                ? parsed.alarmCode : firstNonBlank(parsed.spid, parsed.signalId, parsed.alarmCode);
        unmappedService.record(fsuCode, parsed.deviceId, parsed.deviceCode, parsed.spid,
                parsed.signalId, rawId,
                parsed.alarmName, parsed.alarmValue, null, "SEND_ALARM", null, null,
                mapped.reason() != null ? mapped.reason() : "UNKNOWN_EVENT_ID");
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v.trim();
        return null;
    }

    public static LocalDateTime parseAlarmTime(String alarmTimeStr, LocalDateTime defaultTime) {
        if (alarmTimeStr == null || alarmTimeStr.trim().isEmpty()) {
            return defaultTime;
        }
        try {
            return LocalDateTime.parse(alarmTimeStr.trim(), ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("AlarmTime 解析失败: {}, 使用当前时间", alarmTimeStr);
            return defaultTime;
        }
    }

    // ==================== 内部模型 ====================

    static class AlarmParseResult {
        final boolean valid;
        final String signalId;
        final String alarmCode;
        final String alarmName;
        final String alarmLevel;
        final String alarmValue;
        final String alarmDesc;
        final String alarmType;
        final String serialNo;
        final String deviceId;
        final String spid;
        final String deviceCode;
        final String alarmFlag;
        final boolean isRecover;
        final String error;

        AlarmParseResult(boolean valid, String signalId, String alarmCode,
                         String alarmName, String alarmLevel, String alarmValue,
                         String alarmDesc, String alarmType,
                         String serialNo, String deviceId, String spid,
                         String deviceCode, String alarmFlag,
                         boolean isRecover, String error) {
            this.valid = valid;
            this.signalId = signalId;
            this.alarmCode = alarmCode;
            this.alarmName = alarmName;
            this.alarmLevel = alarmLevel;
            this.alarmValue = alarmValue;
            this.alarmDesc = alarmDesc;
            this.alarmType = alarmType;
            this.serialNo = serialNo;
            this.deviceId = deviceId;
            this.spid = spid;
            this.deviceCode = deviceCode;
            this.alarmFlag = alarmFlag;
            this.isRecover = isRecover;
            this.error = error;
        }
    }
}
