package com.dcim.platform.module.binterface.repository;

import com.dcim.platform.module.binterface.entity.BInterfaceCallRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BInterfaceCallRecordRepository extends JpaRepository<BInterfaceCallRecordEntity, Long> {
    List<BInterfaceCallRecordEntity> findByFsuIdOrderByCallTimeDesc(Long fsuId);
    List<BInterfaceCallRecordEntity> findByCommandCodeOrderByCallTimeDesc(String commandCode);
}
