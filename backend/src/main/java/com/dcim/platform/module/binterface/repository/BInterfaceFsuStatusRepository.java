package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BInterfaceFsuStatusRepository extends JpaRepository<BInterfaceFsuStatusEntity, Long> {
    Optional<BInterfaceFsuStatusEntity> findByFsuId(Long fsuId);
    Optional<BInterfaceFsuStatusEntity> findByFsuCode(String fsuCode);
    List<BInterfaceFsuStatusEntity> findByOnlineStatus(String onlineStatus);
}
