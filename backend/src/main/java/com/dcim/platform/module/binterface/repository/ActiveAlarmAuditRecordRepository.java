package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.entity.ActiveAlarmAuditRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveAlarmAuditRecordRepository extends JpaRepository<ActiveAlarmAuditRecordEntity, Long> {

    List<ActiveAlarmAuditRecordEntity> findByFsuCodeOrderByRunAtDesc(String fsuCode);

    List<ActiveAlarmAuditRecordEntity> findBySuccessOrderByRunAtDesc(boolean success);

    Optional<ActiveAlarmAuditRecordEntity> findTopByOrderByRunAtDesc();

    Optional<ActiveAlarmAuditRecordEntity> findTopByFsuCodeOrderByRunAtDesc(String fsuCode);

    Page<ActiveAlarmAuditRecordEntity> findByFsuCodeOrderByRunAtDesc(String fsuCode, Pageable pageable);

    Page<ActiveAlarmAuditRecordEntity> findAllByOrderByRunAtDesc(Pageable pageable);
}
