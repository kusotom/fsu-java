package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceSessionQueryService {

    private final BInterfaceSessionRepository repository;

    public BInterfaceSessionQueryService(BInterfaceSessionRepository repository) {
        this.repository = repository;
    }

    public List<BInterfaceSessionEntity> list() {
        return repository.findAll();
    }

    public BInterfaceSessionEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Session not found: " + id));
    }

    // TODO: 真实会话管理在 BIF-P1 系列任务中实现
}
