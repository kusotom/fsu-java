package com.dcim.platform.module.mapping.repository;

import com.dcim.platform.module.mapping.entity.EStoneIIControlReferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EStoneIIControlReferenceRepository extends JpaRepository<EStoneIIControlReferenceEntity, Long> {
    Optional<EStoneIIControlReferenceEntity> findByCommandId(String commandId);
}
