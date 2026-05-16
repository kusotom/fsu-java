package com.dcim.platform.module.alarm.repository;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AlarmRecordRepository extends JpaRepository<AlarmRecordEntity, Long> {
    List<AlarmRecordEntity> findByFsuId(Long fsuId);
    List<AlarmRecordEntity> findByAlarmStatus(String alarmStatus);
    List<AlarmRecordEntity> findByAlarmLevelAndAlarmStatus(String alarmLevel, String alarmStatus);
    Optional<AlarmRecordEntity> findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus(
            Long fsuId, String pointCode, String alarmCode, String alarmStatus);
}
