package com.dcim.platform.module.resource.repository;

import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FsuDeviceRepository extends JpaRepository<FsuDeviceEntity, Long> {
    Optional<FsuDeviceEntity> findByFsuCode(String fsuCode);
    List<FsuDeviceEntity> findBySiteId(Long siteId);
    List<FsuDeviceEntity> findByStatus(String status);
}
