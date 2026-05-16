package com.dcim.platform.module.telemetry.repository;

import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RealtimeDataRepository extends JpaRepository<RealtimeDataEntity, Long> {
    Optional<RealtimeDataEntity> findByPointId(Long pointId);
    List<RealtimeDataEntity> findByFsuId(Long fsuId);
}
