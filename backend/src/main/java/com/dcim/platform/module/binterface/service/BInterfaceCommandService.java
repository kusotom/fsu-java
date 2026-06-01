package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceCommandEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceCommandRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceCommandService {

    private final BInterfaceCommandRepository repository;
    
    public BInterfaceCommandService(BInterfaceCommandRepository repository) {
        this.repository = repository;
    }

    public List<BInterfaceCommandEntity> list() {
        return repository.findAll();
    }

    public BInterfaceCommandEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Command not found: " + id));
    }

    // 不提供 safeEnabled 更新接口，保护 SET_FSUREBOOT
}
