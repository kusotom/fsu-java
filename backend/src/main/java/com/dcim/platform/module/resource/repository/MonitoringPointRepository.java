package com.dcim.platform.module.resource.repository;

import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringPointRepository extends JpaRepository<MonitoringPointEntity, Long> {
    Optional<MonitoringPointEntity> findByFsuIdAndPointCode(Long fsuId, String pointCode);
    List<MonitoringPointEntity> findByFsuId(Long fsuId);
    List<MonitoringPointEntity> findByPointType(String pointType);
}
