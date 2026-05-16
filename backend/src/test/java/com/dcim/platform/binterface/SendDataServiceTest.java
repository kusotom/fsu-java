package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.SendDataResult;
import com.dcim.platform.module.binterface.service.SendDataService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SendDataService 单元测试。
 *
 * 使用内存 HashMap 替代 JPA Repository，不依赖数据库。
 */
class SendDataServiceTest {

    private SendDataService sendDataService;

    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubMonitoringPointRepository monitoringPointRepo;
    private StubRealtimeDataRepository realtimeDataRepo;

    private static final String FSU_CODE = "FSU-001";
    private static final Long FSU_ID = 1L;

    @BeforeEach
    void setUp() {
        fsuDeviceRepo = new StubFsuDeviceRepository();
        monitoringPointRepo = new StubMonitoringPointRepository();
        realtimeDataRepo = new StubRealtimeDataRepository();

        // 预注册 FSU 设备
        FsuDeviceEntity fsu = new FsuDeviceEntity();
        fsu.setId(FSU_ID);
        fsu.setFsuCode(FSU_CODE);
        fsu.setFsuName("测试FSU-001");
        fsu.setStatus("ONLINE");
        fsu.setCreatedAt(LocalDateTime.now());
        fsu.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(fsu);

        // 预注册测点
        createMonitoringPoint(10L, "TEMP-001", "温度1", "analog", "float");
        createMonitoringPoint(20L, "HUMI-001", "湿度1", "analog", "float");
        createMonitoringPoint(30L, "VOLT-001", "电压1", "analog", "float");
        createMonitoringPoint(40L, "DOOR-001", "门磁1", "digital", "int");
        createMonitoringPoint(50L, "WATER-001", "水浸1", "digital", "int");

        sendDataService = new SendDataService(fsuDeviceRepo, monitoringPointRepo, realtimeDataRepo);
    }

    private void createMonitoringPoint(Long id, String pointCode, String pointName,
                                        String pointType, String dataType) {
        MonitoringPointEntity point = new MonitoringPointEntity();
        point.setId(id);
        point.setFsuId(FSU_ID);
        point.setPointCode(pointCode);
        point.setPointName(pointName);
        point.setPointType(pointType);
        point.setDataType(dataType);
        point.setStatus("ACTIVE");
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        monitoringPointRepo.save(point);
    }

    // ==================== 成功处理 ====================

    @Test
    void shouldProcessAllSignalsSuccessfully() {
        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5"),
                signal("HUMI-001", "60.2"),
                signal("VOLT-001", "220.0"),
                signal("DOOR-001", "1"),
                signal("WATER-001", "0")
        );

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals(5, result.getAcceptedCount());
        assertEquals(0, result.getRejectedCount());
        assertEquals(FSU_CODE, result.getFsuCode());
    }

    @Test
    void shouldPersistRealtimeDataOnSuccess() {
        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5")
        );

        sendDataService.processData(FSU_CODE, null, xmlData);

        Optional<RealtimeDataEntity> saved = realtimeDataRepo.findByPointId(10L);
        assertTrue(saved.isPresent());
        assertEquals("25.5", saved.get().getValueText());
        assertEquals(0, new BigDecimal("25.5").compareTo(saved.get().getValueNumber()));
        assertEquals("TEMP-001", saved.get().getPointCode());
        assertEquals(FSU_ID, saved.get().getFsuId());
        assertNotNull(saved.get().getCollectTime());
        assertNotNull(saved.get().getReceiveTime());
    }

    @Test
    void shouldUpsertExistingRealtimeData() {
        // 先存一条
        RealtimeDataEntity existing = new RealtimeDataEntity();
        existing.setId(100L);
        existing.setFsuId(FSU_ID);
        existing.setPointId(10L);
        existing.setPointCode("TEMP-001");
        existing.setValueText("old");
        existing.setCollectTime(LocalDateTime.now());
        existing.setReceiveTime(LocalDateTime.now());
        existing.setCreatedAt(LocalDateTime.now().minusHours(1));
        existing.setUpdatedAt(LocalDateTime.now().minusHours(1));
        realtimeDataRepo.save(existing);

        // 再次上报
        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "26.8")
        );

        sendDataService.processData(FSU_CODE, null, xmlData);

        Optional<RealtimeDataEntity> saved = realtimeDataRepo.findByPointId(10L);
        assertTrue(saved.isPresent());
        assertEquals("26.8", saved.get().getValueText());
        // createdAt 应该保持原值（第一次创建的时间）
        assertNotNull(saved.get().getCreatedAt());
    }

    @Test
    void shouldParseNumericValuesCorrectly() {
        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5"),
                signal("DOOR-001", "1")
        );

        sendDataService.processData(FSU_CODE, null, xmlData);

        RealtimeDataEntity temp = realtimeDataRepo.findByPointId(10L).orElseThrow();
        RealtimeDataEntity door = realtimeDataRepo.findByPointId(40L).orElseThrow();

        assertEquals(0, new BigDecimal("25.5").compareTo(temp.getValueNumber()));
        assertEquals(0, new BigDecimal("1").compareTo(door.getValueNumber()));
    }

    // ==================== 部分成功 ====================

    @Test
    void shouldReportPartialSuccessWhenSomeSignalsFail() {
        // 只注册 3 个测点，上报 5 个信号
        monitoringPointRepo.deleteAll(); // 清空
        createMonitoringPoint(10L, "TEMP-001", "温度1", "analog", "float");
        createMonitoringPoint(20L, "HUMI-001", "湿度1", "analog", "float");
        createMonitoringPoint(30L, "VOLT-001", "电压1", "analog", "float");

        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5"),
                signal("HUMI-001", "60.2"),
                signal("VOLT-001", "220.0"),
                signal("DOOR-001", "1"),     // 无对应测点
                signal("WATER-001", "0")      // 无对应测点
        );

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getAcceptedCount());
        assertEquals(2, result.getRejectedCount());
    }

    @Test
    void shouldIncludeErrorMessagesInPartialResult() {
        monitoringPointRepo.deleteAll();
        createMonitoringPoint(10L, "TEMP-001", "温度1", "analog", "float");

        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5"),
                signal("UNKNOWN-001", "99.9")
        );

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("UNKNOWN-001")));
    }

    // ==================== 全量失败 ====================

    @Test
    void shouldFailWhenAllSignalsRejected() {
        // 没有注册任何测点
        monitoringPointRepo.deleteAll();

        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5")
        );

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
        assertEquals(0, result.getAcceptedCount());
        assertEquals(0, result.getRejectedCount());
        assertTrue(FSU_CODE.equals(result.getFsuCode()));
    }

    // ==================== 缺少 SignalID / Value ====================

    @Test
    void shouldRejectSignalWithMissingSignalId() {
        XmlDataModel xmlData = createXmlDataWithSignals(
                Map.of("Value", "25.5")  // 缺少 SignalID
        );

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldRejectSignalWithMissingValue() {
        XmlDataModel xmlData = createXmlDataWithSignals(
                Map.of("SignalID", "TEMP-001")  // 缺少 Value
        );

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== FSU 未注册 ====================

    @Test
    void shouldFailForUnknownFsu() {
        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5")
        );

        SendDataResult result = sendDataService.processData("FSU-UNKNOWN", null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== 空数据 ====================

    @Test
    void shouldFailForNullXmlData() {
        SendDataResult result = sendDataService.processData(FSU_CODE, null, null);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    @Test
    void shouldFailForEmptyItems() {
        XmlDataModel xmlData = createXmlDataWithSignals(); // 空列表

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== CollectTime 解析 ====================

    @Test
    void shouldUseProvidedCollectTime() {
        String collectTime = "2026-05-14T10:30:00+08:00";

        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5")
        );

        sendDataService.processData(FSU_CODE, collectTime, xmlData);

        RealtimeDataEntity saved = realtimeDataRepo.findByPointId(10L).orElseThrow();
        assertEquals(LocalDateTime.of(2026, 5, 14, 10, 30, 0), saved.getCollectTime());
    }

    @Test
    void shouldUseDefaultTimeForInvalidCollectTime() {
        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("TEMP-001", "25.5")
        );

        // 传入非法格式时间字符串，应使用当前时间
        sendDataService.processData(FSU_CODE, "not-a-valid-time", xmlData);

        RealtimeDataEntity saved = realtimeDataRepo.findByPointId(10L).orElseThrow();
        assertNotNull(saved.getCollectTime());
    }

    // ==================== parseNumeric ====================

    @Test
    void parseNumericShouldReturnNullForNonNumeric() {
        assertNull(SendDataService.parseNumeric("abc"));
        assertNull(SendDataService.parseNumeric(""));
        assertNull(SendDataService.parseNumeric(null));
    }

    @Test
    void parseNumericShouldReturnBigDecimalForValidNumber() {
        assertEquals(0, new BigDecimal("25.5").compareTo(SendDataService.parseNumeric("25.5")));
        assertEquals(0, new BigDecimal("0").compareTo(SendDataService.parseNumeric("0")));
        assertEquals(0, new BigDecimal("-1.5").compareTo(SendDataService.parseNumeric("-1.5")));
    }

    // ==================== parseCollectTime ====================

    @Test
    void parseCollectTimeShouldReturnDefaultForNull() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(now, SendDataService.parseCollectTime(null, now));
    }

    @Test
    void parseCollectTimeShouldReturnDefaultForEmpty() {
        LocalDateTime now = LocalDateTime.now();
        assertEquals(now, SendDataService.parseCollectTime("", now));
    }

    // ==================== Quality 和 Status 传播 ====================

    @Test
    void shouldPreserveQualityAndStatus() {
        Map<String, String> signal = new HashMap<>();
        signal.put("SignalID", "TEMP-001");
        signal.put("Value", "25.5");
        signal.put("Quality", "1");
        signal.put("Status", "normal");

        XmlDataModel xmlData = createXmlDataWithSignals(signal);

        sendDataService.processData(FSU_CODE, null, xmlData);

        RealtimeDataEntity saved = realtimeDataRepo.findByPointId(10L).orElseThrow();
        assertEquals("1", saved.getQuality());
        assertEquals("normal", saved.getValueStatus());
    }

    @Test
    void shouldHandleMultipleSignalsWithPartialErrors() {
        createMonitoringPoint(60L, "SIG-001", "信号1", "analog", "float");

        XmlDataModel xmlData = createXmlDataWithSignals(
                signal("SIG-001", "10.0"),       // 成功
                signal("SIG-001", "20.0"),       // 成功（同测点不同上报）
                signal("MISSING-001", "30.0"),   // 失败（无测点）
                Map.of("SignalID", ""),          // 失败（空 SignalID）
                Map.of("Value", "50.0")          // 失败（无 SignalID）
        );

        SendDataResult result = sendDataService.processData(FSU_CODE, null, xmlData);

        // SIG-001 被上报两次，每次都 upsert，所以两次都 accepted
        assertEquals(2, result.getAcceptedCount());
        assertEquals(3, result.getRejectedCount());
        assertTrue(result.hasErrors());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建只有 Signal 项列表的简单 XmlDataModel。
     */
    private XmlDataModel createXmlDataWithSignals(Map<String, String>... signals) {
        XmlDataModel model = new XmlDataModel();
        if (signals != null) {
            for (Map<String, String> signal : signals) {
                model.addItem(signal);
            }
        }
        return model;
    }

    private Map<String, String> signal(String signalId, String value) {
        Map<String, String> map = new HashMap<>();
        map.put("SignalID", signalId);
        map.put("Value", value);
        map.put("Quality", "0");
        map.put("Status", "normal");
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

        // === JPA 通用方法 ===
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

    static class StubMonitoringPointRepository extends BaseStub<MonitoringPointEntity> implements MonitoringPointRepository {
        @Override protected Long getId(MonitoringPointEntity e) { return e.getId(); }
        @Override protected void setId(MonitoringPointEntity e, Long id) { e.setId(id); }

        @Override
        public MonitoringPointEntity save(MonitoringPointEntity entity) {
            return saveEntity(entity, entity.getId());
        }

        @Override
        public Optional<MonitoringPointEntity> findByFsuIdAndPointCode(Long fsuId, String pointCode) {
            return store.values().stream()
                    .filter(e -> fsuId.equals(e.getFsuId()) && pointCode.equals(e.getPointCode()))
                    .findFirst();
        }

        @Override public List<MonitoringPointEntity> findByFsuId(Long fsuId) { return List.of(); }
        @Override public List<MonitoringPointEntity> findByPointType(String pointType) { return List.of(); }
    }

    static class StubRealtimeDataRepository extends BaseStub<RealtimeDataEntity> implements RealtimeDataRepository {
        @Override protected Long getId(RealtimeDataEntity e) { return e.getId(); }
        @Override protected void setId(RealtimeDataEntity e, Long id) { e.setId(id); }

        @Override
        public RealtimeDataEntity save(RealtimeDataEntity entity) {
            return saveEntity(entity, entity.getId());
        }

        @Override
        public Optional<RealtimeDataEntity> findByPointId(Long pointId) {
            return store.values().stream()
                    .filter(e -> pointId.equals(e.getPointId()))
                    .findFirst();
        }

        @Override public List<RealtimeDataEntity> findByFsuId(Long fsuId) { return List.of(); }
    }
}
