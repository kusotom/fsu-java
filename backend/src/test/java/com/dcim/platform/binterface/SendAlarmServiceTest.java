package com.dcim.platform.binterface;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.binterface.service.SendAlarmResult;
import com.dcim.platform.module.binterface.service.SendAlarmService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SendAlarmService 单元测试。
 *
 * 使用内存 HashMap 替代 JPA Repository，不依赖数据库。
 */
class SendAlarmServiceTest {

    private SendAlarmService sendAlarmService;

    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubAlarmRecordRepository alarmRecordRepo;

    private static final String FSU_CODE = "FSU-001";
    private static final Long FSU_ID = 1L;

    @BeforeEach
    void setUp() {
        fsuDeviceRepo = new StubFsuDeviceRepository();
        alarmRecordRepo = new StubAlarmRecordRepository();

        // 预注册 FSU 设备
        FsuDeviceEntity fsu = new FsuDeviceEntity();
        fsu.setId(FSU_ID);
        fsu.setFsuCode(FSU_CODE);
        fsu.setFsuName("测试FSU-001");
        fsu.setStatus("ONLINE");
        fsu.setCreatedAt(LocalDateTime.now());
        fsu.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(fsu);

        sendAlarmService = new SendAlarmService(fsuDeviceRepo, alarmRecordRepo);
    }

    // ==================== 成功处理：告警产生 ====================

    @Test
    void shouldProcessSingleAlarmSuccessfully() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", "温度过高", "WARN", "62.0", "超限", "0")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals(1, result.getAcceptedCount());
        assertEquals(0, result.getRecoveredCount());
        assertEquals(0, result.getRejectedCount());
        assertEquals(1, result.getAlarmIds().size());
    }

    @Test
    void shouldPersistAlarmRecordOnGenerate() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", "温度过高", "WARN", "62.0", "超限", "0")
        );

        sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        List<AlarmRecordEntity> all = alarmRecordRepo.findAll();
        assertEquals(1, all.size());
        AlarmRecordEntity saved = all.get(0);
        assertEquals("TEMP-001", saved.getPointCode());
        assertEquals("TEMP-HIGH", saved.getAlarmCode());
        assertEquals("温度过高", saved.getAlarmName());
        assertEquals("WARN", saved.getAlarmLevel());
        assertEquals("62.0", saved.getAlarmValue());
        assertEquals("超限", saved.getAlarmDesc());
        assertEquals(FSU_ID, saved.getFsuId());
        assertEquals("ACTIVE", saved.getAlarmStatus());
        assertNotNull(saved.getOccurTime());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void shouldProcessMultipleAlarms() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", "温度过高", "WARN", "62.0", "超限", "0"),
                alarm("HUMI-001", "HUMI-HIGH", "湿度过高", "WARN", "90.0", "超限", "0"),
                alarm("DOOR-001", "DOOR-OPEN", "门磁告警", "CRITICAL", "1", "门未关", "0")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertEquals(3, result.getAcceptedCount());
        assertEquals(0, result.getRecoveredCount());
        assertEquals(3, alarmRecordRepo.findAll().size());
    }

    // ==================== 告警恢复 ====================

    @Test
    void shouldRecoverActiveAlarm() {
        // 先保存一个 ACTIVE 告警
        AlarmRecordEntity active = new AlarmRecordEntity();
        active.setId(100L);
        active.setFsuId(FSU_ID);
        active.setPointCode("TEMP-001");
        active.setAlarmCode("TEMP-HIGH");
        active.setAlarmLevel("WARN");
        active.setAlarmStatus("ACTIVE");
        active.setOccurTime(LocalDateTime.now());
        active.setCreatedAt(LocalDateTime.now().minusHours(1));
        active.setUpdatedAt(LocalDateTime.now().minusHours(1));
        alarmRecordRepo.save(active);

        // 发送恢复告警
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", "25.0", "已恢复", "1")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertEquals(0, result.getAcceptedCount());
        assertEquals(1, result.getRecoveredCount());
        assertEquals(0, result.getRejectedCount());

        // 验证告警状态已更新
        AlarmRecordEntity updated = alarmRecordRepo.findById(100L).orElseThrow();
        assertEquals("RECOVERED", updated.getAlarmStatus());
        assertEquals("25.0", updated.getAlarmValue());
        assertNotNull(updated.getClearTime());
    }

    @Test
    void shouldCreateRecoveredRecordWhenNoActiveAlarmFound() {
        // 没有 ACTIVE 告警，直接发恢复
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", "25.0", "已恢复", "1")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertEquals(0, result.getAcceptedCount());
        assertEquals(1, result.getRecoveredCount());

        List<AlarmRecordEntity> all = alarmRecordRepo.findAll();
        assertEquals(1, all.size());
        assertEquals("RECOVERED", all.get(0).getAlarmStatus());
    }

    // ==================== 混合：产生 + 恢复 ====================

    @Test
    void shouldHandleMixedGenerateAndRecover() {
        // 已有 ACTIVE 告警
        AlarmRecordEntity active = new AlarmRecordEntity();
        active.setId(200L);
        active.setFsuId(FSU_ID);
        active.setPointCode("DOOR-001");
        active.setAlarmCode("DOOR-OPEN");
        active.setAlarmLevel("CRITICAL");
        active.setAlarmStatus("ACTIVE");
        active.setOccurTime(LocalDateTime.now());
        active.setCreatedAt(LocalDateTime.now());
        active.setUpdatedAt(LocalDateTime.now());
        alarmRecordRepo.save(active);

        // 发送混合：2 个新告警 + 1 个恢复
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", "温度过高", "WARN", "62.0", "", "0"),
                alarm("HUMI-001", "HUMI-HIGH", "湿度过高", "WARN", "90.0", "", "0"),
                alarm("DOOR-001", "DOOR-OPEN", null, "CRITICAL", "0", "门已关", "1")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertEquals(2, result.getAcceptedCount());
        assertEquals(1, result.getRecoveredCount());
        assertEquals(0, result.getRejectedCount());
        assertEquals(3, result.getAlarmIds().size());
    }

    // ==================== 字段校验：缺少必要字段 ====================

    @Test
    void shouldRejectAlarmWithMissingSignalId() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                Map.of("AlarmCode", "TEMP-HIGH", "AlarmLevel", "WARN")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldRejectAlarmWithMissingAlarmCode() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                Map.of("SignalID", "TEMP-001", "AlarmLevel", "WARN")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldRejectAlarmWithMissingAlarmLevel() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                Map.of("SignalID", "TEMP-001", "AlarmCode", "TEMP-HIGH")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== FSU 未注册 ====================

    @Test
    void shouldFailForUnknownFsu() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", null, null, "0")
        );

        SendAlarmResult result = sendAlarmService.processAlarms("FSU-UNKNOWN", null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== 空数据 ====================

    @Test
    void shouldFailForNullXmlData() {
        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, null);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldFailForEmptyItems() {
        XmlDataModel xmlData = createXmlDataWithAlarms();

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== AlarmTime 解析 ====================

    @Test
    void shouldUseProvidedAlarmTime() {
        String alarmTime = "2026-05-14T10:33:00+08:00";

        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", null, null, "0")
        );

        sendAlarmService.processAlarms(FSU_CODE, alarmTime, xmlData);

        List<AlarmRecordEntity> all = alarmRecordRepo.findAll();
        assertEquals(1, all.size());
        assertEquals(LocalDateTime.of(2026, 5, 14, 10, 33, 0), all.get(0).getOccurTime());
    }

    @Test
    void shouldUseDefaultTimeForInvalidAlarmTime() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", null, null, "0")
        );

        sendAlarmService.processAlarms(FSU_CODE, "not-a-valid-time", xmlData);

        List<AlarmRecordEntity> all = alarmRecordRepo.findAll();
        assertNotNull(all.get(0).getOccurTime());
    }

    // ==================== parseAlarmTime ====================

    @Test
    void parseAlarmTimeShouldReturnDefaultForNull() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(now, SendAlarmService.parseAlarmTime(null, now));
    }

    @Test
    void parseAlarmTimeShouldReturnDefaultForEmpty() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(now, SendAlarmService.parseAlarmTime("", now));
    }

    // ==================== 部分成功 ====================

    @Test
    void shouldReportPartialSuccess() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", null, null, "0"),
                Map.of("SignalID", "")  // 无效项
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getAcceptedCount());
        assertEquals(1, result.getRejectedCount());
        assertTrue(result.hasErrors());
    }

    // ==================== 重复告警 ====================

    @Test
    void shouldCreateSeparateRecordsForSameAlarmCode() {
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", "62.0", null, "0"),
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", "63.0", null, "0")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertEquals(2, result.getAcceptedCount());
        assertEquals(2, alarmRecordRepo.findAll().size());
        // 同一 SignalID+AlarmCode 可以多次上报产生，每次产生新告警记录
    }

    // ==================== 最小字段 ====================

    @Test
    void shouldHandleAlarmWithMinimalRequiredFields() {
        // 只包含最简必要字段
        XmlDataModel xmlData = createXmlDataWithAlarms(
                Map.of("SignalID", "TEMP-001", "AlarmCode", "TEMP-HIGH", "AlarmLevel", "WARN",
                        "AlarmType", "0")
        );

        SendAlarmResult result = sendAlarmService.processAlarms(FSU_CODE, null, xmlData);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getAcceptedCount());
        assertEquals(1, alarmRecordRepo.findAll().size());
        AlarmRecordEntity saved = alarmRecordRepo.findAll().get(0);
        assertEquals("TEMP-001", saved.getPointCode());
        assertEquals("TEMP-HIGH", saved.getAlarmCode());
        assertEquals("WARN", saved.getAlarmLevel());
        assertEquals("ACTIVE", saved.getAlarmStatus());
    }

    @Test
    void shouldNotRegisterUnknownFsu() {
        // 验证不存在的 FSU 不会被自动注册
        XmlDataModel xmlData = createXmlDataWithAlarms(
                alarm("TEMP-001", "TEMP-HIGH", null, "WARN", null, null, "0")
        );

        SendAlarmResult result = sendAlarmService.processAlarms("FSU-AUTO-REG", null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
        assertEquals(0, fsuDeviceRepo.findByFsuCode("FSU-AUTO-REG").isPresent() ? 1 : 0);
    }

    // ==================== 辅助方法 ====================

    private XmlDataModel createXmlDataWithAlarms(Map<String, String>... alarms) {
        XmlDataModel model = new XmlDataModel();
        if (alarms != null) {
            for (Map<String, String> alarm : alarms) {
                model.addItem(alarm);
            }
        }
        return model;
    }

    private Map<String, String> alarm(String signalId, String alarmCode, String alarmName,
                                       String alarmLevel, String alarmValue, String alarmDesc,
                                       String alarmType) {
        Map<String, String> map = new LinkedHashMap<>();
        if (signalId != null) map.put("SignalID", signalId);
        if (alarmCode != null) map.put("AlarmCode", alarmCode);
        if (alarmName != null) map.put("AlarmName", alarmName);
        if (alarmLevel != null) map.put("AlarmLevel", alarmLevel);
        if (alarmValue != null) map.put("AlarmValue", alarmValue);
        if (alarmDesc != null) map.put("AlarmDesc", alarmDesc);
        if (alarmType != null) map.put("AlarmType", alarmType);
        return map;
    }

    // ==================== Stub Repository 实现 ====================

    @SuppressWarnings("unchecked")
    abstract static class BaseStub<T> {
        final Map<Long, T> store = new HashMap<>();
        long nextId = 1;

        public T saveEntity(T entity, Long id) {
            if (id == null) {
                id = nextId++;
            }
            setId(entity, id);
            store.put(id, entity);
            return entity;
        }

        protected abstract Long getId(T entity);
        protected abstract void setId(T entity, Long id);

        public Optional<T> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        public boolean existsById(Long id) { return store.containsKey(id); }
        public List<T> findAll() { return List.copyOf(store.values()); }
        public List<T> findAll(org.springframework.data.domain.Sort sort) { return List.copyOf(store.values()); }
        public List<T> findAllById(Iterable<Long> ids) { return List.of(); }
        public long count() { return store.size(); }
        public void deleteById(Long id) { store.remove(id); }
        public void delete(T entity) { store.remove(getId(entity)); }
        public void deleteAll(Iterable<? extends T> entities) { entities.forEach(e -> store.remove(getId(e))); }
        public void deleteAll() { store.clear(); }
        public void deleteAllInBatch(Iterable<T> entities) {}
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(store::remove); }
        public void deleteAllInBatch() {}
        public void flush() {}
        public T getOne(Long id) { return store.get(id); }
        public T getById(Long id) { return store.get(id); }
        public T getReferenceById(Long id) { return store.get(id); }
        public <S extends T> S saveAndFlush(S entity) { return (S) saveEntity((T) entity, getId((T) entity)); }
        public <S extends T> List<S> saveAll(Iterable<S> entities) { List<S> r = new ArrayList<>(); for (S e : entities) r.add((S) saveEntity((T) e, getId((T) e))); return r; }
        public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) { List<S> r = new ArrayList<>(); for (S e : entities) r.add((S) saveEntity((T) e, getId((T) e))); return r; }
        public <S extends T> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        public <S extends T> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        public <S extends T> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }
        public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable pageable) { return new org.springframework.data.domain.PageImpl<>(List.copyOf(store.values())); }
    }

    static class StubFsuDeviceRepository extends BaseStub<FsuDeviceEntity> implements FsuDeviceRepository {
        @Override protected Long getId(FsuDeviceEntity e) { return e.getId(); }
        @Override protected void setId(FsuDeviceEntity e, Long id) { e.setId(id); }

        @Override
        public FsuDeviceEntity save(FsuDeviceEntity entity) {
            return saveEntity(entity, entity.getId());
        }

        @Override
        public Optional<FsuDeviceEntity> findByFsuCode(String fsuCode) {
            return store.values().stream()
                    .filter(e -> e.getFsuCode().equals(fsuCode))
                    .findFirst();
        }

        @Override public List<FsuDeviceEntity> findBySiteId(Long siteId) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String status) { return List.of(); }
    }

    static class StubAlarmRecordRepository extends BaseStub<AlarmRecordEntity> implements AlarmRecordRepository {
        @Override protected Long getId(AlarmRecordEntity e) { return e.getId(); }
        @Override protected void setId(AlarmRecordEntity e, Long id) { e.setId(id); }

        @Override
        public AlarmRecordEntity save(AlarmRecordEntity entity) {
            return saveEntity(entity, entity.getId());
        }

        @Override
        public List<AlarmRecordEntity> findByFsuId(Long fsuId) {
            return store.values().stream()
                    .filter(e -> fsuId.equals(e.getFsuId()))
                    .toList();
        }

        @Override
        public List<AlarmRecordEntity> findByAlarmStatus(String alarmStatus) {
            return store.values().stream()
                    .filter(e -> alarmStatus.equals(e.getAlarmStatus()))
                    .toList();
        }

        @Override
        public List<AlarmRecordEntity> findByAlarmLevelAndAlarmStatus(String alarmLevel, String alarmStatus) {
            return store.values().stream()
                    .filter(e -> alarmLevel.equals(e.getAlarmLevel()) && alarmStatus.equals(e.getAlarmStatus()))
                    .toList();
        }

        @Override
        public Optional<AlarmRecordEntity> findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus(
                Long fsuId, String pointCode, String alarmCode, String alarmStatus) {
            return store.values().stream()
                    .filter(e -> fsuId.equals(e.getFsuId())
                            && pointCode.equals(e.getPointCode())
                            && alarmCode.equals(e.getAlarmCode())
                            && alarmStatus.equals(e.getAlarmStatus()))
                    .findFirst();
        }
    }
}
