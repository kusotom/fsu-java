package com.dcim.platform.module.telemetry.service;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.module.binterface.dto.BInterfaceFrontendDtos.RealtimePointDto;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
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
    private final EStoneIIDictionaryImportService dictionaryImportService;
    private final EStoneIIMappingService mappingService;

    public RealtimeDataService(RealtimeDataRepository repository, DataScopeService dataScopeService,
                               FsuDeviceRepository fsuDeviceRepository,
                               EStoneIIDictionaryImportService dictionaryImportService,
                               EStoneIIMappingService mappingService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.dictionaryImportService = dictionaryImportService;
        this.mappingService = mappingService;
    }

    public List<RealtimePointDto> list() {
        dictionaryImportService.importIfNeeded();
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        List<RealtimeDataEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all,
                        e -> fsuCodeById.getOrDefault(e.getFsuId(), String.valueOf(e.getFsuId())))
                .stream()
                .map(e -> toDto(e, fsuCodeById.get(e.getFsuId())))
                .toList();
    }

    public RealtimePointDto getById(Long id) {
        RealtimeDataEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("RealtimeData not found: " + id));
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        List<RealtimeDataEntity> visible = dataScopeService.filterByFsuScope(List.of(entity),
                e -> fsuCodeById.getOrDefault(e.getFsuId(), String.valueOf(e.getFsuId())));
        if (visible.isEmpty()) throw new RuntimeException("RealtimeData not found: " + id);
        return toDto(entity, fsuCodeById.get(entity.getFsuId()));
    }

    // TODO: upsert by point_id - will be implemented in INIT-005

    private RealtimePointDto toDto(RealtimeDataEntity e, String fsuCode) {
        RealtimePointDto dto = new RealtimePointDto();
        dto.fsuCode = fsuCode;
        dto.signalId = e.getPointCode();
        dto.spid = e.getPointCode();
        dto.valueText = e.getValueText();
        dto.valueNumber = e.getValueNumber();
        dto.value = e.getValueText() != null ? e.getValueText()
                : (e.getValueNumber() != null ? e.getValueNumber().toPlainString() : null);
        dto.quality = e.getQuality();
        dto.collectTime = e.getCollectTime() != null ? e.getCollectTime().toString() : null;
        dto.updatedAt = e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null;
        EStoneIIMappingResult mapped = mappingService.resolveRealtime(fsuCode, null, null, e.getPointCode(), e.getPointCode(), dto.value);
        dto.signalName = mapped.signalName();
        dto.signalType = mapped.signalType();
        dto.signalCategory = mapped.signalCategory();
        dto.unit = mapped.unit();
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

    private Map<Long, String> fsuCodeByDbId() {
        Map<Long, String> map = new HashMap<>();
        for (FsuDeviceEntity fsu : fsuDeviceRepository.findAll()) {
            if (fsu.getId() != null && fsu.getFsuCode() != null) map.put(fsu.getId(), fsu.getFsuCode());
        }
        return map;
    }
}
