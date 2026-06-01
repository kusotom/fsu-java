package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.ftp.FtpTransferRecordEntity;
import com.dcim.platform.module.binterface.repository.FtpTransferRecordRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FtpTransferRecordQueryService {

    private final FtpTransferRecordRepository repository;
    private final DataScopeService dataScopeService;

    public FtpTransferRecordQueryService(FtpTransferRecordRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<FtpTransferRecordEntity> list() {
        List<FtpTransferRecordEntity> all = repository.findAll();
        return dataScopeService.filterByFsuScope(all, FtpTransferRecordEntity::getFsuCode);
    }

    public FtpTransferRecordEntity getById(Long id) {
        FtpTransferRecordEntity entity = repository.findById(id).orElseThrow(() -> new RuntimeException("FtpTransferRecordEntity not found: " + id));
        checkScopeForEntity(entity, "fsuCode", "FtpTransferRecordEntity");
        return entity;
    }

    private void checkScopeForEntity(FtpTransferRecordEntity entity, String scopeType, String entityName) {
        com.dcim.platform.common.security.RequestContext ctx = com.dcim.platform.common.security.RequestContext.getCurrent();
        if (ctx == null || ctx.isAdminLike()) return;

        String code = entity.getFsuCode();
        if (code != null && !ctx.getFsuScope().isEmpty() && !ctx.getFsuScope().contains(code)) {
            throw new com.dcim.platform.common.exception.ForbiddenException(
                "无权访问此" + entityName + ": fsuCode=" + code, "fsu:view");
        }
    }

    // 不实现真实 FTP 连接
}
