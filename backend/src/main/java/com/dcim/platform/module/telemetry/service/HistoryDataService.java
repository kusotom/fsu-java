package com.dcim.platform.module.telemetry.service;

import com.dcim.platform.module.telemetry.entity.HistoryDataEntity;
import com.dcim.platform.module.telemetry.repository.HistoryDataRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class HistoryDataService {

    private final HistoryDataRepository repository;
    private final DataScopeService dataScopeService;

    public HistoryDataService(HistoryDataRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<HistoryDataEntity> list() {
        List<HistoryDataEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, e -> String.valueOf(e.getFsuId()));
    }

    public HistoryDataEntity getById(Long id) {
        HistoryDataEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("HistoryDataEntity not found: " + id));
        checkScopeForEntity(entity, "fsuCode", "HistoryDataEntity");
        return entity;
    }

    /** BE-AUTH-P0-FIX-002: scope 校验 — empty scope = default-deny */
    private void checkScopeForEntity(HistoryDataEntity entity, String scopeType, String entityName) {
        com.dcim.platform.common.security.RequestContext ctx = com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null || ctx.isAdminLike()) return;

        java.util.Set<String> allowed = dataScopeService.getAllowedFsuCodes();
        if (allowed != null && allowed.isEmpty()) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无授权 FSU 范围", "fsu:view");
        }

        String code = String.valueOf(entity.getFsuId());
        if (allowed != null && !allowed.contains(code)) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无权访问此" + entityName + ": fsuCode=" + code, "fsu:view");
        }
    }

    public List<HistoryDataEntity> queryByPoint(Long pointId, LocalDateTime start, LocalDateTime end) {
        return repository.findByPointIdAndCollectTimeBetween(pointId, start, end);
    }
}
