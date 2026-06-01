package com.dcim.platform.module.mapping.repository;

import com.dcim.platform.module.mapping.entity.DeviceSignalCandidateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceSignalCandidateRepository extends JpaRepository<DeviceSignalCandidateEntity, Long> {
    Optional<DeviceSignalCandidateEntity> findFirstByFsuIdAndDeviceIdAndSignalId(String fsuId, String deviceId, String signalId);
    Optional<DeviceSignalCandidateEntity> findFirstByFsuIdAndDeviceCodeAndSignalId(String fsuId, String deviceCode, String signalId);
    Optional<DeviceSignalCandidateEntity> findFirstByDeviceIdAndSignalId(String deviceId, String signalId);
    Optional<DeviceSignalCandidateEntity> findFirstByDeviceCodeAndSignalId(String deviceCode, String signalId);
    Optional<DeviceSignalCandidateEntity> findFirstByFsuIdAndDeviceIdAndSignalIdAndMappingSource(
            String fsuId, String deviceId, String signalId, String mappingSource);
    List<DeviceSignalCandidateEntity> findByFsuIdOrderByDeviceIdAscSignalIdAsc(String fsuId);
    List<DeviceSignalCandidateEntity> findAllByOrderByDeviceIdAscSignalIdAsc();
}
