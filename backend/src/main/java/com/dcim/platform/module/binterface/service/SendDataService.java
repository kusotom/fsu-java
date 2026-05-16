package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SEND_DATA 实时数据上报服务。
 *
 * 职责：
 * <ul>
 *   <li>解析 XmlDataModel 中的 Signal 项列表</li>
 *   <li>校验每项 SignalID / Value 必填</li>
 *   <li>通过 {@link MonitoringPointRepository} 查找对应测点</li>
 *   <li>通过 {@link RealtimeDataRepository} 更新实时数据（按 pointId upsert）</li>
 *   <li>统计 acceptedCount / rejectedCount</li>
 * </ul>
 *
 * 线程安全：依赖 Spring @Transactional，由容器保证事务安全。
 */
@Service
public class SendDataService {

    private static final Logger log = LoggerFactory.getLogger(SendDataService.class);

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final FsuDeviceRepository fsuDeviceRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final RealtimeDataRepository realtimeDataRepository;

    public SendDataService(FsuDeviceRepository fsuDeviceRepository,
                           MonitoringPointRepository monitoringPointRepository,
                           RealtimeDataRepository realtimeDataRepository) {
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.monitoringPointRepository = monitoringPointRepository;
        this.realtimeDataRepository = realtimeDataRepository;
    }

    /**
     * 处理 SEND_DATA 实时数据上报。
     *
     * @param fsuCode        FSU 编码
     * @param collectTimeStr 采集时间字符串（Info.CollectTime，可选，ISO8601）
     * @param xmlData        已解析的 xmlData 模型
     * @return 处理结果
     */
    @Transactional
    public SendDataResult processData(String fsuCode, String collectTimeStr, XmlDataModel xmlData) {
        // 1. 查找 FSU 设备
        Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepository.findByFsuCode(fsuCode);
        if (fsuOpt.isEmpty()) {
            log.warn("SEND_DATA 失败: FSU 未注册 fsuCode={}", fsuCode);
            return SendDataResult.fail("1002", "FSU 未注册: " + fsuCode);
        }
        Long fsuId = fsuOpt.get().getId();

        // 2. 解析采集时间
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime collectTime = parseCollectTime(collectTimeStr, now);

        // 3. 获取 Signal 项列表
        if (xmlData == null) {
            log.warn("SEND_DATA 失败: xmlData 为空 fsuCode={}", fsuCode);
            return SendDataResult.fail("2003", "无有效测点数据");
        }
        List<Map<String, String>> items = xmlData.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("SEND_DATA 失败: 无 Signal 项 fsuCode={}", fsuCode);
            return SendDataResult.fail("2003", "无有效测点数据");
        }

        // 4. 逐项处理
        int accepted = 0;
        int rejected = 0;
        List<String> errors = new ArrayList<>();

        for (Map<String, String> item : items) {
            try {
                SignalParseResult parsed = parseSignal(item);
                if (!parsed.valid) {
                    rejected++;
                    errors.add(parsed.error);
                    continue;
                }

                // 查找测点映射
                Optional<MonitoringPointEntity> pointOpt = monitoringPointRepository
                        .findByFsuIdAndPointCode(fsuId, parsed.signalId);
                if (pointOpt.isEmpty()) {
                    rejected++;
                    errors.add("SignalID=" + parsed.signalId + " 未找到对应测点");
                    continue;
                }

                MonitoringPointEntity point = pointOpt.get();

                // upsert 实时数据
                upsertRealtimeData(fsuId, point, parsed, collectTime, now);
                accepted++;

            } catch (Exception e) {
                rejected++;
                errors.add("Signal处理异常: " + e.getMessage());
            }
        }

        if (accepted == 0 && rejected > 0) {
            return SendDataResult.fail("2003", "无有效测点数据", fsuCode);
        }

        if (rejected > 0) {
            return SendDataResult.partial(fsuCode, accepted, rejected, errors);
        }

        return SendDataResult.success(fsuCode, accepted, rejected);
    }

    // ==================== 内部方法 ====================

    private SignalParseResult parseSignal(Map<String, String> item) {
        String signalId = null;
        String value = null;
        String quality = null;
        String status = null;

        for (Map.Entry<String, String> entry : item.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase("SignalID")) {
                signalId = entry.getValue();
            } else if (key.equalsIgnoreCase("Value")) {
                value = entry.getValue();
            } else if (key.equalsIgnoreCase("Quality")) {
                quality = entry.getValue();
            } else if (key.equalsIgnoreCase("Status")) {
                status = entry.getValue();
            }
        }

        if (signalId == null || signalId.trim().isEmpty()) {
            return new SignalParseResult(false, null, null, null, null, "缺少 SignalID");
        }
        if (value == null || value.trim().isEmpty()) {
            return new SignalParseResult(false, signalId, null, null, null, "SignalID=" + signalId + " 缺少 Value");
        }

        return new SignalParseResult(true, signalId.trim(), value.trim(), quality, status, null);
    }

    private void upsertRealtimeData(Long fsuId, MonitoringPointEntity point,
                                     SignalParseResult parsed,
                                     LocalDateTime collectTime, LocalDateTime now) {
        RealtimeDataEntity data = realtimeDataRepository.findByPointId(point.getId())
                .orElseGet(RealtimeDataEntity::new);

        data.setFsuId(fsuId);
        data.setPointId(point.getId());
        data.setPointCode(parsed.signalId);
        data.setValueText(parsed.value);
        data.setValueNumber(parseNumeric(parsed.value));
        data.setValueStatus(parsed.status);
        data.setQuality(parsed.quality);
        data.setCollectTime(collectTime);
        data.setReceiveTime(now);
        if (data.getCreatedAt() == null) {
            data.setCreatedAt(now);
        }
        data.setUpdatedAt(now);

        realtimeDataRepository.save(data);
    }

    public static BigDecimal parseNumeric(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static LocalDateTime parseCollectTime(String collectTimeStr, LocalDateTime defaultTime) {
        if (collectTimeStr == null || collectTimeStr.trim().isEmpty()) {
            return defaultTime;
        }
        try {
            return LocalDateTime.parse(collectTimeStr.trim(), ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("CollectTime 解析失败: {}, 使用当前时间", collectTimeStr);
            return defaultTime;
        }
    }

    // ==================== 内部模型 ====================

    private static class SignalParseResult {
        final boolean valid;
        final String signalId;
        final String value;
        final String quality;
        final String status;
        final String error;

        SignalParseResult(boolean valid, String signalId, String value,
                          String quality, String status, String error) {
            this.valid = valid;
            this.signalId = signalId;
            this.value = value;
            this.quality = quality;
            this.status = status;
            this.error = error;
        }
    }
}
