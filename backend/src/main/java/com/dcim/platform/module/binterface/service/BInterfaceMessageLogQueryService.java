package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceMessageLogQueryService {

    private final BInterfaceMessageLogRepository repository;

    public BInterfaceMessageLogQueryService(BInterfaceMessageLogRepository repository) {
        this.repository = repository;
    }

    public List<BInterfaceMessageLogEntity> list() {
        return repository.findAll();
    }

    public BInterfaceMessageLogEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("MessageLog not found: " + id));
    }

    // TODO: 真实日志写入在 BIF-P1 系列任务中实现
}
