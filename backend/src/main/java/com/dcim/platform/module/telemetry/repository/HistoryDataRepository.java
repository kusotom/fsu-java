package com.dcim.platform.module.telemetry.repository;

import com.dcim.platform.module.telemetry.entity.HistoryDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoryDataRepository extends JpaRepository<HistoryDataEntity, Long> {
    List<HistoryDataEntity> findByPointIdAndCollectTimeBetween(Long pointId, LocalDateTime start, LocalDateTime end);
    List<HistoryDataEntity> findByFsuIdAndCollectTimeBetween(Long fsuId, LocalDateTime start, LocalDateTime end);
}
