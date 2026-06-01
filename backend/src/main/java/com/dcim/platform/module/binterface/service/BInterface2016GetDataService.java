package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.mapping.service.UnmappedSignalObservationService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * B接口2016 GET_DATA DeviceList/TSemaphore 查询服务。
 *
 * 使用 B接口2016 Code=401 + pkTypeFormat="legacy-2016"。
 * 支持 Info.DeviceList (Device Id/Code) 和可选 TSemaphore。
 */
@Service
public class BInterface2016GetDataService {

    private static final Logger log = LoggerFactory.getLogger(BInterface2016GetDataService.class);

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;
    private final FsuDeviceRepository fsuDeviceRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final RealtimeDataRepository realtimeDataRepository;
    private final EStoneIIMappingService mappingService;
    private final UnmappedSignalObservationService unmappedService;

    @Autowired
    public BInterface2016GetDataService(FsuServiceClient fsuServiceClient,
                                         FsuEndpointResolver fsuEndpointResolver,
                                         FsuDeviceRepository fsuDeviceRepository,
                                         MonitoringPointRepository monitoringPointRepository,
                                         RealtimeDataRepository realtimeDataRepository,
                                         EStoneIIMappingService mappingService,
                                         UnmappedSignalObservationService unmappedService) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.monitoringPointRepository = monitoringPointRepository;
        this.realtimeDataRepository = realtimeDataRepository;
        this.mappingService = mappingService;
        this.unmappedService = unmappedService;
    }

    public BInterface2016GetDataService(FsuServiceClient fsuServiceClient,
                                         FsuEndpointResolver fsuEndpointResolver,
                                         FsuDeviceRepository fsuDeviceRepository,
                                         MonitoringPointRepository monitoringPointRepository,
                                         RealtimeDataRepository realtimeDataRepository) {
        this(fsuServiceClient, fsuEndpointResolver, fsuDeviceRepository, monitoringPointRepository,
                realtimeDataRepository, null, null);
    }

    /**
     * 执行 2016 GET_DATA 查询。
     *
     * @param fsuCode    FSU 编码
     * @param deviceIds  要查询的 DeviceID 列表（必填，至少一个）
     * @param semaphores 可选的 TSemaphore 映射 (DeviceID → SignalID 列表)，null 表示查全设备
     */
    public BInterface2016GetDataResult execute(String fsuCode, List<String> deviceIds,
                                                Map<String, List<String>> semaphores) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return BInterface2016GetDataResult.fail("2001", "缺少 FSUCode", null);
        }
        String suid = fsuCode.trim();

        Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepository.findByFsuCode(suid);
        if (fsuOpt.isEmpty()) {
            return BInterface2016GetDataResult.fail("1002", "FSU 未注册", suid);
        }
        Long fsuId = fsuOpt.get().getId();

        if (deviceIds == null || deviceIds.isEmpty()) {
            return BInterface2016GetDataResult.fail("2003", "缺少 DeviceID", suid);
        }

        FsuEndpointResult endpoint = fsuEndpointResolver.resolve(suid);
        if (!endpoint.isSuccess()) {
            return BInterface2016GetDataResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), suid);
        }

        try {
            // 1. 构造 Info: FsuId + FsuCode + DeviceList
            String infoXml = buildInfoXml(suid, deviceIds, semaphores);

            // 2. 构造 FsuServiceRequest (legacy-2016)
            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(suid)
                    .serviceUrl(endpoint.getServiceUrl())
                    .pkType(BInterfacePkType.GET_DATA)
                    .pkTypeFormat("legacy-2016")
                    .infoXml(infoXml)
                    .build();

            // 3. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);
            boolean realCall = response.isRealCall();
            String rawSoap = response.getRawSoap();

            if (!response.isSuccess()) {
                log.warn("GET_DATA 2016 调用失败: fsuCode={}, code={}, desc={}",
                        suid, response.getResultCode(), response.getResultDesc());
                return BInterface2016GetDataResult.fail(
                        response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 查询失败", suid);
            }

            // 4. 校验 ACK Code=402
            if (rawSoap != null && !rawSoap.contains("<Code>402</Code>") && !rawSoap.contains("<Code>402<")) {
                log.warn("GET_DATA ACK Code 不是 402: fsuCode={}", suid);
                return BInterface2016GetDataResult.fail("2010", "ACK Code 不匹配", suid);
            }

            // 5. 解析响应
            XmlDataModel xmlData = response.getXmlData();
            boolean emptyData = (xmlData == null || xmlData.isEmpty());

            if (emptyData) {
                return BInterface2016GetDataResult.successEmpty(
                        suid, suid, realCall,
                        request.getInfoXml(), rawSoap);
            }

            // 6. 解析 TSemaphore 值
            List<BInterface2016GetDataResult.DeviceData> devices = new ArrayList<>();
            List<BInterface2016GetDataResult.UnmappedEntry> unmapped = new ArrayList<>();

            parseSemaphores(xmlData, fsuId, suid, devices, unmapped);

            var builder = BInterface2016GetDataResult.builder()
                    .success(true).emptyData(false)
                    .fsuCode(suid).fsuId(suid)
                    .realDeviceAccessed(realCall)
                    .rawRequest(request.getInfoXml())
                    .rawResponse(rawSoap);
            for (var d : devices) builder.addDevice(d);
            for (var u : unmapped) builder.addUnmapped(u);
            return builder.build();

        } catch (Exception e) {
            log.error("GET_DATA 2016 处理异常: fsuCode={}", suid, e);
            return BInterface2016GetDataResult.fail("5001", "查询异常: " + e.getMessage(), suid);
        }
    }

    // ==================== 内部方法 ====================

    private String buildInfoXml(String fsuCode, List<String> deviceIds,
                                 Map<String, List<String>> semaphores) {
        StringBuilder sb = new StringBuilder();
        sb.append("<FsuId>").append(escape(fsuCode)).append("</FsuId>");
        sb.append("<FsuCode>").append(escape(fsuCode)).append("</FsuCode>");
        sb.append("<DeviceList>");
        for (String devId : deviceIds) {
            sb.append("<Device Id=\"").append(escape(devId))
              .append("\" Code=\"").append(escape(devId)).append("\"");
            List<String> semList = semaphores != null ? semaphores.get(devId) : null;
            if (semList != null && !semList.isEmpty()) {
                sb.append(">\n");
                boolean hasValid = false;
                for (String semId : semList) {
                    if (semId == null || semId.isBlank()) continue;
                    sb.append("<Id>").append(escape(semId)).append("</Id>\n");
                    hasValid = true;
                }
                if (!hasValid) {
                    sb.setLength(sb.length() - 2); // remove ">\n"
                    sb.append("/>\n");
                } else {
                    sb.append("</Device>\n");
                }
            } else {
                sb.append("/>\n");
            }
        }
        sb.append("</DeviceList>");
        return sb.toString();
    }

    private void parseSemaphores(XmlDataModel xmlData, Long fsuId, String fsuCode,
                                  List<BInterface2016GetDataResult.DeviceData> devices,
                                  List<BInterface2016GetDataResult.UnmappedEntry> unmapped) {
        List<Map<String, String>> items = xmlData.getItems();
        if (items == null || items.isEmpty()) {
            unmapped.add(new BInterface2016GetDataResult.UnmappedEntry(
                    "unknown", "unknown", null, null));
            return;
        }

        for (Map<String, String> item : items) {
            String devId = getField(item, "DeviceID");
            String spid = getField(item, "Id");
            String signalId = getField(item, "Code");
            String measuredVal = getField(item, "MeasuredVal");
            String status = getField(item, "Status");
            String time = getField(item, "Time");

            if (devId == null) devId = "unknown";
            if (spid == null && signalId == null) continue;

            // 尝试匹配 monitoring_point
            boolean matched = false;
            String pointCode = spid != null ? spid : signalId;
            if (pointCode != null) {
                Optional<MonitoringPointEntity> pointOpt =
                        monitoringPointRepository.findByFsuIdAndPointCode(fsuId, pointCode);
                if (pointOpt.isPresent()) {
                    MonitoringPointEntity point = pointOpt.get();
                    upsertRealtimeData(fsuId, point, pointCode, measuredVal, status, time);
                    matched = true;
                }
            }

            if (!matched) {
                recordUnmapped(fsuCode, devId, spid, signalId, measuredVal);
                unmapped.add(new BInterface2016GetDataResult.UnmappedEntry(
                        devId, spid, signalId, measuredVal));
            }
        }
    }

    private void recordUnmapped(String fsuCode, String deviceId, String spid, String signalId, String value) {
        if (unmappedService == null) return;
        String reason = "UNKNOWN_DEVICE_SIGNAL_PAIR";
        if (mappingService != null) {
            EStoneIIMappingResult mapped = mappingService.resolveRealtime(fsuCode, deviceId, null, spid, signalId, value);
            if (mapped != null) {
                if (mapped.mapped() && mapped.reason() == null) return;
                reason = mapped.reason() != null ? mapped.reason()
                        : ("TEMPLATE_ONLY".equals(mapped.mappingStatus()) ? "TEMPLATE_ONLY_NOT_RETURNED" : "UNKNOWN_DEVICE_SIGNAL_PAIR");
            }
        }
        unmappedService.record(fsuCode, deviceId, null, spid, signalId, spid, null, value,
                null, "GET_DATA", null, null, reason);
    }

    private void upsertRealtimeData(Long fsuId, MonitoringPointEntity point,
                                     String pointCode, String measuredVal,
                                     String status, String time) {
        try {
            RealtimeDataEntity data = realtimeDataRepository.findByPointId(point.getId())
                    .orElseGet(RealtimeDataEntity::new);
            data.setFsuId(fsuId);
            data.setPointId(point.getId());
            data.setPointCode(pointCode);
            data.setValueText(measuredVal);
            data.setValueNumber(parseNumeric(measuredVal));
            data.setValueStatus(status);
            data.setReceiveTime(LocalDateTime.now());
            if (data.getCreatedAt() == null) data.setCreatedAt(LocalDateTime.now());
            data.setUpdatedAt(LocalDateTime.now());
            realtimeDataRepository.save(data);
        } catch (Exception e) {
            log.warn("realtime_data 写入失败: fsuId={} pointCode={}", fsuId, pointCode, e);
        }
    }

    private String getField(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (var e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key))
                return e.getValue() != null ? e.getValue().trim() : null;
        }
        return null;
    }

    private BigDecimal parseNumeric(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        try { return new BigDecimal(v.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
