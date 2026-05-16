package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.entity.BInterfaceCommandEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface BInterfaceCommandRepository extends JpaRepository<BInterfaceCommandEntity, Long> {
    Optional<BInterfaceCommandEntity> findByCommandCode(String commandCode);
}
