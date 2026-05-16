package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BInterfaceSessionRepository extends JpaRepository<BInterfaceSessionEntity, Long> {
    Optional<BInterfaceSessionEntity> findBySessionId(String sessionId);
    Optional<BInterfaceSessionEntity> findByFsuIdAndStatus(Long fsuId, String status);
}
