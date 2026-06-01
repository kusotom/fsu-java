package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceMessageLogQueryService {

    private final BInterfaceMessageLogRepository repository;
    private final DataScopeService dataScopeService;

    public BInterfaceMessageLogQueryService(BInterfaceMessageLogRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<BInterfaceMessageLogEntity> list() {
        List<BInterfaceMessageLogEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, BInterfaceMessageLogEntity::getFsuCode);
    }

    public BInterfaceMessageLogEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("MessageLog not found: " + id));
    }

    // TODO: 真实日志写入在 BIF-P1 系列任务中实现
}
