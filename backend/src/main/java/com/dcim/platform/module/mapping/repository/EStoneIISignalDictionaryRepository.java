package com.dcim.platform.module.mapping.repository;

import com.dcim.platform.module.mapping.entity.EStoneIISignalDictionaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EStoneIISignalDictionaryRepository extends JpaRepository<EStoneIISignalDictionaryEntity, Long> {
    Optional<EStoneIISignalDictionaryEntity> findBySignalId(String signalId);
}
