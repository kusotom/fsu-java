package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BInterfaceMessageLogRepository extends JpaRepository<BInterfaceMessageLogEntity, Long> {

    // ===== 旧接口（LANDING-008，保持兼容） =====

    List<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode);

    List<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command);

    // ===== LANDING-010: 分页查询 =====

    Page<BInterfaceMessageLogEntity> findByDirectionOrderByCreatedAtDesc(String direction, Pageable pageable);

    Page<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode, Pageable pageable);

    Page<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command, Pageable pageable);

    Page<BInterfaceMessageLogEntity> findByMessageTypeOrderByCreatedAtDesc(String messageType, Pageable pageable);

    // ===== LANDING-010: 日志清理 =====

    @Modifying
    @Transactional
    long deleteByCreatedAtBefore(LocalDateTime cutoff);
}
