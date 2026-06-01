package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BInterfaceSessionQueryService {

    private final BInterfaceSessionRepository repository;
    private final DataScopeService dataScopeService;

    public BInterfaceSessionQueryService(BInterfaceSessionRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<BInterfaceSessionEntity> list() {
        List<BInterfaceSessionEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, BInterfaceSessionEntity::getFsuCode);
    }

    public BInterfaceSessionEntity getById(Long id) {
        BInterfaceSessionEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("BInterfaceSessionEntity not found: " + id));
        checkScopeForEntity(entity, "fsuCode", "BInterfaceSessionEntity");
        return entity;
    }

    private void checkScopeForEntity(BInterfaceSessionEntity entity, String scopeType, String entityName) {
        com.dcim.platform.common.security.RequestContext ctx = com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null || ctx.isAdminLike()) return;

        String code = entity.getFsuCode();
        if (code != null && !ctx.getFsuScope().isEmpty() && !ctx.getFsuScope().contains(code)) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无权访问此" + entityName + ": fsuCode=" + code, "fsu:view");
        }
    }

    // TODO: 真实会话管理在 BIF-P1 系列任务中实现
}
