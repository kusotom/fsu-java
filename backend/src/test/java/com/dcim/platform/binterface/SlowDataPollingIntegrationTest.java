package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.binterface.service.slow.SignalPollingTarget;
import com.dcim.platform.module.binterface.service.slow.SignalPollingTargetService;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingProperties;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingResult;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 慢数据轮询集成测试（含 Signal 目标配置）。
 *
 * <p>使用真实 SlowDataPollingService + 真实 LoginService + 真实 SignalPollingTargetService
 * + Stub Repository + Stub Service。验证轮询编排 + 目标配置全链路的正确性。</p>
 */
class SlowDataPollingIntegrationTest {

    private SlowDataPollingProperties properties;
    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubFsuStatusRepository fsuStatusRepo;
    private StubSessionRepository sessionRepo;
    private StubMonitoringPointRepository monitoringPointRepo;
    private LoginService loginService;
    private SignalPollingTargetService targetService;
    private StubGetDataService getDataService;
    private StubGetThresholdService getThresholdService;
    private SlowDataPollingService pollingService;

    @BeforeEach
    void setUp() {
        properties = new SlowDataPollingProperties();
        properties.setEnabled(true);
        properties.setPollGetData(true);
        properties.setPollGetThreshold(false);
        properties.setMaxFsuPerRun(50);

        fsuDeviceRepo = new StubFsuDeviceRepository();
        fsuStatusRepo = new StubFsuStatusRepository();
        sessionRepo = new StubSessionRepository();
        monitoringPointRepo = new StubMonitoringPointRepository();
        loginService = new LoginService(fsuDeviceRepo, fsuStatusRepo, sessionRepo);
        targetService = new SignalPollingTargetService(fsuDeviceRepo, monitoringPointRepo);
        getDataService = new StubGetDataService();
        getThresholdService = new StubGetThresholdService();
        pollingService = new SlowDataPollingService(properties, fsuDeviceRepo, loginService,
                getDataService, getThresholdService, targetService);
    }

    private FsuDeviceEntity addFsu(String fsuCode) {
        FsuDeviceEntity fsu = new FsuDeviceEntity();
        fsu.setId((long) fsuDeviceRepo.store.size() + 1);
        fsu.setFsuCode(fsuCode);
        fsu.setFsuName("测试FSU-" + fsuCode);
        fsu.setStatus("ONLINE");
        fsu.setSiteId(1L);
        fsu.setCreatedAt(LocalDateTime.now());
        fsu.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(fsu);
        return fsu;
    }

    private void loginFsu(String fsuCode) {
        loginService.login(fsuCode, null);
    }

    private void addMonitoringPoint(String fsuCode, String pointCode) {
        FsuDeviceEntity fsu = fsuDeviceRepo.findByFsuCode(fsuCode).orElse(null);
        if (fsu == null) return;
        MonitoringPointEntity point = new MonitoringPointEntity();
        point.setFsuId(fsu.getId());
        point.setPointCode(pointCode);
        point.setPointName(pointCode);
        point.setPointType("analog");
        point.setDataType("float");
        point.setStatus("ACTIVE");
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        monitoringPointRepo.save(point);
    }

    // ==================== 无 FSU ====================

    @Test
    void shouldReturnEmptyWhenNoFsu() {
        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(0, result.getScannedFsuCount());
        assertTrue(result.isSuccess());
    }

    // ==================== ONLINE FSU with targets ====================

    @Test
    void shouldPollOnlineFsuWithTargets() {
        addFsu("FSU-001");
        loginFsu("FSU-001");
        addMonitoringPoint("FSU-001", "TEMP-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(0, result.getSkippedOfflineCount());
        assertEquals(0, result.getSkippedNoSignalCount());
        assertEquals(1, result.getGetDataSuccessCount());
        assertTrue(result.getProcessedFsuCodes().contains("FSU-001"));
    }

    // ==================== OFFLINE FSU ====================

    @Test
    void shouldSkipOfflineFsu() {
        addFsu("FSU-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedOfflineCount());
        assertEquals(0, result.getGetDataSuccessCount());
    }

    // ==================== 在线但无 target ====================

    @Test
    void shouldSkipFsuWithNoTargets() {
        addFsu("FSU-001");
        loginFsu("FSU-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedNoSignalCount());
        assertEquals(0, result.getGetDataSuccessCount());
    }

    // ==================== GET_THRESHOLD ====================

    @Test
    void shouldExecuteGetThresholdWhenEnabled() {
        properties.setPollGetThreshold(true);
        addFsu("FSU-001");
        loginFsu("FSU-001");
        addMonitoringPoint("FSU-001", "TEMP-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(1, result.getGetDataSuccessCount());
        assertEquals(1, result.getGetThresholdSuccessCount());
    }

    @Test
    void shouldNotExecuteGetThresholdWhenDisabled() {
        properties.setPollGetThreshold(false);
        addFsu("FSU-001");
        loginFsu("FSU-001");
        addMonitoringPoint("FSU-001", "TEMP-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(1, result.getGetDataSuccessCount());
        assertEquals(0, result.getGetThresholdSuccessCount());
        assertEquals(0, result.getGetThresholdFailureCount());
    }

    // ==================== 混合场景 ====================

    @Test
    void shouldHandleMixedOnlineAndOffline() {
        addFsu("FSU-001");
        addFsu("FSU-002");
        addFsu("FSU-003");
        loginFsu("FSU-001");
        loginFsu("FSU-003");
        addMonitoringPoint("FSU-001", "TEMP-001");
        addMonitoringPoint("FSU-003", "TEMP-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(3, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedOfflineCount());
        assertEquals(2, result.getGetDataSuccessCount());
    }

    // ==================== 不访问真实网络 ====================

    @Test
    void shouldNotAccessRealNetwork() {
        addFsu("FSU-001");
        loginFsu("FSU-001");
        addMonitoringPoint("FSU-001", "TEMP-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertFalse(result.isRealCallEnabled());
        assertTrue(result.isSuccess());
    }

    // ==================== maxFsuPerRun ====================

    @Test
    void shouldRespectMaxFsuPerRun() {
        properties.setMaxFsuPerRun(1);
        addFsu("FSU-001");
        addFsu("FSU-002");
        loginFsu("FSU-001");
        loginFsu("FSU-002");
        addMonitoringPoint("FSU-001", "TEMP-001");
        addMonitoringPoint("FSU-002", "TEMP-001");

        SlowDataPollingResult result = pollingService.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getGetDataSuccessCount());
    }

    @Test
    void resultShouldNotBeNull() {
        assertNotNull(pollingService.runOnce());
    }

    // ==================== GET_DATA 传了正确的 signalId ====================

    @Test
    void shouldPassSignalIdsToGetDataService() {
        addFsu("FSU-001");
        loginFsu("FSU-001");
        addMonitoringPoint("FSU-001", "TEMP-001");
        addMonitoringPoint("FSU-001", "HUMI-001");

        SlowDataPollingResult result = pollingService.runOnce();

        // StubGetDataService verifies signalIds is not empty
        assertEquals(1, result.getGetDataSuccessCount());
        assertTrue(result.isSuccess());
    }

    // ==================== Stubs ====================

    @SuppressWarnings("unchecked")
    abstract static class BaseStub<T> {
        final Map<Long, T> store = new HashMap<>();
        long nextId = 1;

        T saveEntity(T entity, Long id) {
            if (id == null) id = nextId++;
            setId(entity, id);
            store.put(id, entity);
            return entity;
        }

        protected abstract Long getId(T entity);
        protected abstract void setId(T entity, Long id);

        public Optional<T> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        public List<T> findAll() { return List.copyOf(store.values()); }
        public List<T> findAll(Sort sort) { return List.copyOf(store.values()); }
        public long count() { return store.size(); }
        public void deleteById(Long id) { store.remove(id); }
        public void deleteAll() { store.clear(); }
        public void flush() {}
        public <S extends T> S saveAndFlush(S entity) { return (S) saveEntity(entity, getId((T) entity)); }
        public <S extends T> List<S> saveAll(Iterable<S> entities) {
            List<S> r = new ArrayList<>(); for (S e : entities) r.add((S) saveEntity((T) e, getId((T) e))); return r;
        }
        public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
        public <S extends T> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        public <S extends T> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        public <S extends T> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example, Sort sort) { return List.of(); }
        public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example,
                                          java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> qf) { return null; }
        public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pp) { return org.springframework.data.domain.Page.empty(); }
        public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable pp) {
            return new PageImpl<>(List.copyOf(store.values()));
        }
        public void deleteAllInBatch(Iterable<T> entities) {}
        public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(store::remove); }
        public void deleteAllInBatch() {}
        public T getOne(Long id) { return store.get(id); }
        public T getById(Long id) { return store.get(id); }
        public T getReferenceById(Long id) { return store.get(id); }
        public boolean existsById(Long id) { return store.containsKey(id); }
        public List<T> findAllById(Iterable<Long> ids) { return List.of(); }
        public void delete(T entity) { store.remove(getId(entity)); }
        public void deleteAll(Iterable<? extends T> entities) { entities.forEach(e -> store.remove(getId(e))); }
    }

    static class StubFsuDeviceRepository extends BaseStub<FsuDeviceEntity> implements FsuDeviceRepository {
        @Override protected Long getId(FsuDeviceEntity e) { return e.getId(); }
        @Override protected void setId(FsuDeviceEntity e, Long id) { e.setId(id); }
        @Override public FsuDeviceEntity save(FsuDeviceEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override public Optional<FsuDeviceEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> e.getFsuCode().equals(fsuCode)).findFirst();
        }
        @Override public List<FsuDeviceEntity> findBySiteId(Long siteId) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String status) { return List.of(); }
    }

    static class StubFsuStatusRepository extends BaseStub<BInterfaceFsuStatusEntity> implements BInterfaceFsuStatusRepository {
        @Override protected Long getId(BInterfaceFsuStatusEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceFsuStatusEntity e, Long id) { e.setId(id); }
        @Override public BInterfaceFsuStatusEntity save(BInterfaceFsuStatusEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override public Optional<BInterfaceFsuStatusEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> fsuCode.equals(e.getFsuCode())).findFirst();
        }
        @Override public Optional<BInterfaceFsuStatusEntity> findByFsuId(Long fsuId) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId())).findFirst();
        }
        @Override public List<BInterfaceFsuStatusEntity> findByOnlineStatus(String onlineStatus) {
            return store.values().stream().filter(e -> onlineStatus.equals(e.getOnlineStatus())).collect(Collectors.toList());
        }
    }

    static class StubSessionRepository extends BaseStub<BInterfaceSessionEntity> implements BInterfaceSessionRepository {
        @Override protected Long getId(BInterfaceSessionEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceSessionEntity e, Long id) { e.setId(id); }
        @Override public BInterfaceSessionEntity save(BInterfaceSessionEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override public Optional<BInterfaceSessionEntity> findBySessionId(String sessionId) {
            return store.values().stream().filter(e -> sessionId.equals(e.getSessionId())).findFirst();
        }
        @Override public Optional<BInterfaceSessionEntity> findByFsuIdAndStatus(Long fsuId, String status) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId()) && status.equals(e.getStatus())).findFirst();
        }
    }

    static class StubMonitoringPointRepository extends BaseStub<MonitoringPointEntity> implements MonitoringPointRepository {
        @Override protected Long getId(MonitoringPointEntity e) { return e.getId(); }
        @Override protected void setId(MonitoringPointEntity e, Long id) { e.setId(id); }
        @Override public MonitoringPointEntity save(MonitoringPointEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override public Optional<MonitoringPointEntity> findByFsuIdAndPointCode(Long fsuId, String pointCode) {
            return store.values().stream()
                    .filter(e -> fsuId.equals(e.getFsuId()) && pointCode.equals(e.getPointCode()))
                    .findFirst();
        }
        @Override public List<MonitoringPointEntity> findByFsuId(Long fsuId) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId())).collect(Collectors.toList());
        }
        @Override public List<MonitoringPointEntity> findByPointType(String pointType) {
            return store.values().stream().filter(e -> pointType.equals(e.getPointType())).collect(Collectors.toList());
        }
    }

    static class StubGetDataService extends GetDataService {
        StubGetDataService() { super(null, null); }

        @Override
        public GetDataResult execute(String fsuCode, String serviceUrl, List<String> signalIds) {
            if (signalIds == null || signalIds.isEmpty()) {
                return GetDataResult.fail("2003", "缺少 SignalID", fsuCode);
            }
            return GetDataResult.success(fsuCode, List.of());
        }
    }

    static class StubGetThresholdService extends GetThresholdService {
        StubGetThresholdService() { super(null, null); }

        @Override
        public GetThresholdResult execute(String fsuCode, String serviceUrl, List<String> signalIds) {
            if (signalIds == null || signalIds.isEmpty()) {
                return GetThresholdResult.fail("2003", "缺少 SignalID", fsuCode);
            }
            return GetThresholdResult.success(fsuCode, List.of());
        }
    }
}
