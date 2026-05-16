package com.dcim.platform.module.binterface.log;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * B接口报文日志 Repository
 */
@Repository
public interface BInterfaceMessageLogRepository extends JpaRepository<BInterfaceMessageLogEntity, Long> {
}
