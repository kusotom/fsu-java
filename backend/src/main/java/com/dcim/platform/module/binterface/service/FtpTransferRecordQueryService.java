package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.ftp.FtpTransferRecordEntity;
import com.dcim.platform.module.binterface.repository.FtpTransferRecordRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FtpTransferRecordQueryService {

    private final FtpTransferRecordRepository repository;

    public FtpTransferRecordQueryService(FtpTransferRecordRepository repository) {
        this.repository = repository;
    }

    public List<FtpTransferRecordEntity> list() {
        return repository.findAll();
    }

    public FtpTransferRecordEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("FtpRecord not found: " + id));
    }

    // 不实现真实 FTP 连接
}
