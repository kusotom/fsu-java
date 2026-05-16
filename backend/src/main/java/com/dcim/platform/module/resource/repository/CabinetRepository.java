package com.dcim.platform.module.resource.repository;

import com.dcim.platform.module.resource.entity.CabinetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CabinetRepository extends JpaRepository<CabinetEntity, Long> {
    Optional<CabinetEntity> findByCabinetCode(String cabinetCode);
    List<CabinetEntity> findBySiteId(Long siteId);
}
