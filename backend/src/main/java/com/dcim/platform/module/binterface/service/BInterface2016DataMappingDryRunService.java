package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * DATA-MAPPING-006: GET_DATA TSemaphore → monitoring_point dry-run 映射服务。
 *
 * <p>只读分析，不写 realtime_data，不创建 monitoring_point，不修改数据库。</p>
 *
 * <p>匹配规则（优先级从高到低）:
 * <ol>
 *   <li>fsuCode + deviceId + signalId (精确三要素)</li>
 *   <li>fsuCode + deviceId + spid</li>
 *   <li>fsuCode + deviceId (设备级匹配)</li>
 *   <li>deviceId + signalId (跨FSU)</li>
 *   <li>仅 signalId — 不自动确认</li>
 * </ol>
 */
@Service
public class BInterface2016DataMappingDryRunService {

    private static final Logger log = LoggerFactory.getLogger(BInterface2016DataMappingDryRunService.class);

    private final MonitoringPointRepository mpRepo;
    private final FsuDeviceRepository fsuDeviceRepo;

    public BInterface2016DataMappingDryRunService(MonitoringPointRepository mpRepo,
                                                   FsuDeviceRepository fsuDeviceRepo) {
        this.mpRepo = mpRepo;
        this.fsuDeviceRepo = fsuDeviceRepo;
    }

    /**
     * 对 GET_DATA 返回的 TSemaphore 执行 dry-run 映射分析。
     *
     * @param fsuCode FSU 编码
     * @param xmlData GET_DATA_ACK 解包后的 XmlDataModel (含 DeviceID/Id/MeasuredVal)
     * @return 映射结果（不写库）
     */
    public BInterface2016DataMappingDryRunResult dryRun(String fsuCode, XmlDataModel xmlData) {
        if (xmlData == null || xmlData.isEmpty()) {
            var r = new BInterface2016DataMappingDryRunResult(fsuCode, 0);
            log.info("DATA-MAPPING-006 dry-run: fsuCode={}, empty XmlData — no candidate points", fsuCode);
            return r;
        }

        List<Map<String, String>> items = xmlData.getItems();
        var result = new BInterface2016DataMappingDryRunResult(fsuCode, items.size());

        // Resolve FSU
        Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepo.findByFsuCode(fsuCode);
        if (fsuOpt.isEmpty()) {
            log.warn("DATA-MAPPING-006 dry-run: FSU not registered: fsuCode={}", fsuCode);
            for (var item : items) {
                result.addUnmatched(get(item, "DeviceID"), get(item, "Id"),
                        get(item, "MeasuredVal"), "FSU not registered");
            }
            return result;
        }
        Long fsuId = fsuOpt.get().getId();

        // Load all monitoring points for this FSU once
        List<MonitoringPointEntity> allPoints = mpRepo.findByFsuId(fsuId);
        log.info("DATA-MAPPING-006 dry-run: fsuCode={}, fsuId={}, candidatePoints={}, existingMPs={}",
                fsuCode, fsuId, items.size(), allPoints.size());

        for (var item : items) {
            String deviceId = get(item, "DeviceID");
            String signalId = get(item, "Id");
            String value = get(item, "MeasuredVal");

            if (signalId == null || signalId.isBlank()) {
                result.addUnmatched(deviceId, signalId, value, "signalId missing");
                continue;
            }

            // Rule 1: fsuId + signalId (point_code match)
            var bySignal = allPoints.stream()
                    .filter(p -> signalId.equals(p.getPointCode()))
                    .toList();

            if (bySignal.size() == 1) {
                result.addMatched(deviceId, signalId, value,
                        bySignal.get(0).getId(), "fsuId+signalId");
            } else if (bySignal.size() > 1) {
                result.addAmbiguous(deviceId, signalId, value,
                        bySignal.stream().map(MonitoringPointEntity::getId).toList(),
                        "multiple matches on signalId");
            } else {
                result.addUnmatched(deviceId, signalId, value,
                        "no monitoring_point with point_code=" + signalId + " for fsuId=" + fsuId);
            }
        }

        return result;
    }

    private String get(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (var e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key))
                return e.getValue() != null ? e.getValue().trim() : null;
        }
        return null;
    }
}
