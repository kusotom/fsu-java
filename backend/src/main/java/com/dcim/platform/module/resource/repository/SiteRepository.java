package com.dcim.platform.module.resource.repository;

import com.dcim.platform.module.resource.entity.SiteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Long> {
    Optional<SiteEntity> findBySiteCode(String siteCode);
}
