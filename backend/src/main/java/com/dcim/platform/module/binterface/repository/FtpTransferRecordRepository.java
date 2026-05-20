package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.ftp.FtpTransferRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FtpTransferRecordRepository extends JpaRepository<FtpTransferRecordEntity, Long> {
    List<FtpTransferRecordEntity> findByFsuCodeOrderByTransferTimeDesc(String fsuCode);
}
