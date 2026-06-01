package com.dcim.platform.module.mapping.repository;

import com.dcim.platform.module.mapping.entity.UnmappedSignalObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnmappedSignalObservationRepository extends JpaRepository<UnmappedSignalObservationEntity, Long> {
    Optional<UnmappedSignalObservationEntity> findFirstByFsuIdAndDeviceIdAndSignalIdAndRawIdAndSourceCommandAndReason(
            String fsuId, String deviceId, String signalId, String rawId, String sourceCommand, String reason);
    List<UnmappedSignalObservationEntity> findByFsuIdOrderByLastSeenAtDesc(String fsuId);
    List<UnmappedSignalObservationEntity> findAllByOrderByLastSeenAtDesc();
}
