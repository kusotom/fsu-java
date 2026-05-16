package com.dcim.platform.module.telemetry.repository;

import com.dcim.platform.module.telemetry.entity.DeviceHeartbeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DeviceHeartbeatRepository extends JpaRepository<DeviceHeartbeatEntity, Long> {
    List<DeviceHeartbeatEntity> findByFsuIdOrderByHeartbeatTimeDesc(Long fsuId);
}
