package com.dcim.platform.module.telemetry.service;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.module.binterface.dto.BInterfaceFrontendDtos.RealtimePointDto;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.entity.SiteEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.resource.repository.SiteRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RealtimeDataService {

    private final RealtimeDataRepository repository;
    private final DataScopeService dataScopeService;
    private final FsuDeviceRepository fsuDeviceRepository;
    private final MonitoringPointRepository monitoringPointRepository;
    private final SiteRepository siteRepository;
    private final EStoneIIDictionaryImportService dictionaryImportService;
    private final EStoneIIMappingService mappingService;

    public RealtimeDataService(RealtimeDataRepository repository, DataScopeService dataScopeService,
                               FsuDeviceRepository fsuDeviceRepository,
                               MonitoringPointRepository monitoringPointRepository,
                               SiteRepository siteRepository,
                               EStoneIIDictionaryImportService dictionaryImportService,
                               EStoneIIMappingService mappingService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.monitoringPointRepository = monitoringPointRepository;
        this.siteRepository = siteRepository;
        this.dictionaryImportService = dictionaryImportService;
        this.mappingService = mappingService;
    }

    public List<RealtimePointDto> list() {
        dictionaryImportService.importIfNeeded();
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        Map<Long, MonitoringPointEntity> pointsById = monitoringPointsById();
        Map<Long, String> siteNameByFsuId = siteNameByFsuId(fsuCodeById);
        List<RealtimeDataEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all,
                        e -> fsuCodeById.getOrDefault(e.getFsuId(), String.valueOf(e.getFsuId())))
                .stream()
                .map(e -> toDto(e, fsuCodeById.get(e.getFsuId()), pointsById.get(e.getPointId()), siteNameByFsuId))
                .toList();
    }

    public RealtimePointDto getById(Long id) {
        RealtimeDataEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("RealtimeData not found: " + id));
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        Map<Long, MonitoringPointEntity> pointsById = monitoringPointsById();
        Map<Long, String> siteNameByFsuId = siteNameByFsuId(fsuCodeById);
        List<RealtimeDataEntity> visible = dataScopeService.filterByFsuScope(List.of(entity),
                e -> fsuCodeById.getOrDefault(e.getFsuId(), String.valueOf(e.getFsuId())));
        if (visible.isEmpty()) throw new RuntimeException("RealtimeData not found: " + id);
        return toDto(entity, fsuCodeById.get(entity.getFsuId()), pointsById.get(entity.getPointId()), siteNameByFsuId);
    }

    // TODO: upsert by point_id - will be implemented in INIT-005

    private RealtimePointDto toDto(RealtimeDataEntity e, String fsuCode, MonitoringPointEntity point,
                                    Map<Long, String> siteNameByFsuId) {
        RealtimePointDto dto = new RealtimePointDto();
        dto.fsuCode = fsuCode;
        dto.stationName = siteNameByFsuId.getOrDefault(e.getFsuId(), fsuCode != null ? "站点 " + fsuCode : null);
        dto.siteName = dto.stationName;
        dto.signalId = e.getPointCode();
        dto.spid = e.getPointCode();
        dto.pointName = point != null ? point.getPointName() : null;
        dto.valueText = e.getValueText();
        dto.valueNumber = e.getValueNumber();
        dto.value = e.getValueText() != null ? e.getValueText()
                : (e.getValueNumber() != null ? e.getValueNumber().toPlainString() : null);
        dto.quality = e.getQuality();
        dto.collectTime = e.getCollectTime() != null ? e.getCollectTime().toString() : null;
        dto.updatedAt = e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null;
        EStoneIIMappingResult mapped = mappingService.resolveRealtime(fsuCode, null, null, e.getPointCode(), e.getPointCode(), dto.value);
        dto.deviceId = mapped.deviceId();
        dto.deviceCode = mapped.deviceCode();
        dto.deviceName = mapped.deviceName();
        dto.pointName = firstNonBlank(mapped.signalName(), dto.pointName);
        dto.signalName = mapped.signalName();
        dto.signalType = firstNonBlank(mapped.signalType(), point != null ? point.getPointType() : null);
        dto.signalCategory = mapped.signalCategory();
        dto.unit = mapped.unit() != null ? mapped.unit() : (point != null ? point.getUnit() : null);
        dto.valueMeaning = mapped.valueMeaning();
        dto.mappingStatus = mapped.mappingStatus();
        dto.mappingConfidence = mapped.mappingConfidence();
        dto.templateVariant = mapped.templateVariant();
        dto.needRealDataConfirm = mapped.needRealDataConfirm();
        dto.verifiedByRealData = mapped.verifiedByRealData();
        dto.derived = mapped.derived();
        dto.source = mapped.source();
        dto.hasSignalIdentity = hasSignalIdentity(e.getPointCode());
        dto.isDeviceOnly = false;
        dto.legacyData = false;
        dto.observationType = "REALTIME_SIGNAL";
        if (!mapped.mapped() && isLegacyRealtimePointCode(e.getPointCode())) {
            dto.mappingStatus = "HISTORICAL_PENDING_BACKFILL";
            dto.mappingConfidence = "UNKNOWN";
            dto.source = "legacy_realtime_data";
            dto.needRealDataConfirm = true;
            dto.legacyData = true;
            dto.observationType = "HISTORICAL_REALTIME";
        }
        return dto;
    }

    private static boolean hasSignalIdentity(String pointCode) {
        return pointCode != null && !pointCode.trim().isEmpty();
    }

    private static boolean isLegacyRealtimePointCode(String pointCode) {
        return pointCode == null || !pointCode.trim().matches("\\d+");
    }

    private Map<Long, String> siteNameByFsuId(Map<Long, String> fsuCodeById) {
        Map<Long, String> siteById = new HashMap<>();
        for (SiteEntity site : siteRepository.findAll()) {
            if (site.getId() != null) siteById.put(site.getId(),
                firstNonBlank(site.getSiteName(), "站点 " + site.getId()));
        }
        Map<Long, String> result = new HashMap<>();
        for (FsuDeviceEntity fsu : fsuDeviceRepository.findAll()) {
            if (fsu.getId() != null) {
                String siteName = siteById.get(fsu.getSiteId());
                result.put(fsu.getId(), siteName != null ? siteName
                    : fsuCodeById.getOrDefault(fsu.getId(), "站点"));
            }
        }
        return result;
    }

    private Map<Long, String> fsuCodeByDbId() {
        Map<Long, String> map = new HashMap<>();
        for (FsuDeviceEntity fsu : fsuDeviceRepository.findAll()) {
            if (fsu.getId() != null && fsu.getFsuCode() != null) map.put(fsu.getId(), fsu.getFsuCode());
        }
        return map;
    }

    private Map<Long, MonitoringPointEntity> monitoringPointsById() {
        Map<Long, MonitoringPointEntity> map = new HashMap<>();
        for (MonitoringPointEntity point : monitoringPointRepository.findAll()) {
            if (point.getId() != null) map.put(point.getId(), point);
        }
        return map;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }
}
