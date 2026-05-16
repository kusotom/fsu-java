package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceCallRecordEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceCallRecordRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceCallRecordQueryService {

    private final BInterfaceCallRecordRepository repository;

    public BInterfaceCallRecordQueryService(BInterfaceCallRecordRepository repository) {
        this.repository = repository;
    }

    public List<BInterfaceCallRecordEntity> list() {
        return repository.findAll();
    }

    public BInterfaceCallRecordEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("CallRecord not found: " + id));
    }

    // TODO: 真实调用记录写入在 BIF-P1 系列任务中实现
}
