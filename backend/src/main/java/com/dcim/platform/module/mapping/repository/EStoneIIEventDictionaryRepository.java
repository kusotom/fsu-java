package com.dcim.platform.module.mapping.repository;

import com.dcim.platform.module.mapping.entity.EStoneIIEventDictionaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EStoneIIEventDictionaryRepository extends JpaRepository<EStoneIIEventDictionaryEntity, Long> {
    Optional<EStoneIIEventDictionaryEntity> findByEventId(String eventId);
    Optional<EStoneIIEventDictionaryEntity> findFirstBySignalId(String signalId);
}
