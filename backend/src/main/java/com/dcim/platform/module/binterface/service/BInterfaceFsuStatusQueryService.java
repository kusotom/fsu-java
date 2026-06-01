package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceFsuStatusQueryService {

    private final BInterfaceFsuStatusRepository repository;
    private final DataScopeService dataScopeService;

    public BInterfaceFsuStatusQueryService(BInterfaceFsuStatusRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<BInterfaceFsuStatusEntity> list() {
        List<BInterfaceFsuStatusEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, BInterfaceFsuStatusEntity::getFsuCode);
    }

    public BInterfaceFsuStatusEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("FsuStatus not found: " + id));
    }
}
