package com.dcim.platform.module.binterface.service;

import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.module.binterface.entity.BInterfaceCallRecordEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceCallRecordRepository;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * BE-AUTH-P0-FIX-001: DataScopeService 接入，过滤 fsuScope.
 */
@Service
public class BInterfaceCallRecordQueryService {

    private final BInterfaceCallRecordRepository repository;
    private final DataScopeService dataScopeService;

    public BInterfaceCallRecordQueryService(BInterfaceCallRecordRepository repository,
                                             DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<BInterfaceCallRecordEntity> list() {
        List<BInterfaceCallRecordEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, BInterfaceCallRecordEntity::getFsuCode);
    }

    public BInterfaceCallRecordEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("CallRecord not found: " + id));
    }
}
