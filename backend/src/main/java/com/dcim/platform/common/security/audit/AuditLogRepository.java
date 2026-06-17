package com.dcim.platform.common.security.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * BE-AUTH-P0-001: 审计日志仓库.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    List<AuditLogEntity> findByUserIdOrderByRequestTimeDesc(Long userId);

    List<AuditLogEntity> findByActionOrderByRequestTimeDesc(String action);

    List<AuditLogEntity> findByAllowedFalseOrderByRequestTimeDesc();
}
