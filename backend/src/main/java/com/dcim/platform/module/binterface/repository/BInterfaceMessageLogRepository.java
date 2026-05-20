package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BInterfaceMessageLogRepository extends JpaRepository<BInterfaceMessageLogEntity, Long> {
    List<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode);
    List<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command);
}
