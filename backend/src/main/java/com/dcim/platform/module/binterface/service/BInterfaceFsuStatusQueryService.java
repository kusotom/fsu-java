package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceFsuStatusQueryService {

    private final BInterfaceFsuStatusRepository repository;

    public BInterfaceFsuStatusQueryService(BInterfaceFsuStatusRepository repository) {
        this.repository = repository;
    }

    public List<BInterfaceFsuStatusEntity> list() {
        return repository.findAll();
    }

    public BInterfaceFsuStatusEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("FsuStatus not found: " + id));
    }
}
