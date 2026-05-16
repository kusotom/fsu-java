package com.dcim.platform.binterface.service.slow;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.*;
import com.dcim.platform.module.binterface.service.slow.SignalPollingTarget;
import com.dcim.platform.module.binterface.service.slow.SignalPollingTargetService;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingProperties;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingResult;
import com.dcim.platform.module.binterface.service.slow.SlowDataPollingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SlowDataPollingServiceTest {

    private SlowDataPollingProperties properties;
    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubLoginService loginService;
    private StubGetDataService getDataService;
    private StubGetThresholdService getThresholdService;
    private StubSignalPollingTargetService targetService;
    private SlowDataPollingService service;

    @BeforeEach
    void setUp() {
        properties = new SlowDataPollingProperties();
        properties.setEnabled(true);
        properties.setPollGetData(true);
        properties.setPollGetThreshold(false);
        properties.setMaxFsuPerRun(50);

        fsuDeviceRepo = new StubFsuDeviceRepository();
        loginService = new StubLoginService();
        getDataService = new StubGetDataService();
        getThresholdService = new StubGetThresholdService();
        targetService = new StubSignalPollingTargetService();
        service = new SlowDataPollingService(properties, fsuDeviceRepo, loginService,
                getDataService, getThresholdService, targetService);
    }

    private FsuDeviceEntity addFsu(String fsuCode) {
        FsuDeviceEntity fsu = new FsuDeviceEntity();
        fsu.setId((long) fsuDeviceRepo.store.size() + 1);
        fsu.setFsuCode(fsuCode);
        fsu.setFsuName(fsuCode);
        fsu.setStatus("ONLINE");
        fsu.setSiteId(1L);
        fsu.setCreatedAt(LocalDateTime.now());
        fsu.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.store.put(fsu.getId(), fsu);
        return fsu;
    }

    private void setOnlineAndLoggedIn(String fsuCode) {
        BInterfaceFsuStatusEntity status = new BInterfaceFsuStatusEntity();
        status.setFsuCode(fsuCode);
        status.setOnlineStatus("ONLINE");
        status.setLoginStatus("LOGIN");
        loginService.statusStore.put(fsuCode, status);
        loginService.loggedInSet.add(fsuCode);
    }

    private void setOffline(String fsuCode) {
        BInterfaceFsuStatusEntity status = new BInterfaceFsuStatusEntity();
        status.setFsuCode(fsuCode);
        status.setOnlineStatus("OFFLINE");
        status.setLoginStatus("LOGOUT");
        loginService.statusStore.put(fsuCode, status);
    }

    private void addTarget(String fsuCode, BInterfacePkType commandType, String signalId) {
        targetService.addTarget(fsuCode, commandType, signalId);
    }

    // ==================== 基础 ====================

    @Test
    void shouldReturnResultWhenNoFsu() {
        SlowDataPollingResult result = service.runOnce();
        assertNotNull(result);
        assertEquals(0, result.getScannedFsuCount());
        assertTrue(result.isSuccess());
    }

    @Test
    void shouldNotReturnNull() {
        SlowDataPollingResult result = service.runOnce();
        assertNotNull(result);
    }

    // ==================== ONLINE FSU with targets ====================

    @Test
    void shouldExecuteGetDataForOnlineFsuWithTargets() {
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getGetDataSuccessCount());
        assertEquals(0, result.getGetDataFailureCount());
        assertEquals(0, result.getSkippedOfflineCount());
        assertEquals(0, result.getSkippedNotLoggedInCount());
        assertEquals(0, result.getSkippedNoSignalCount());
        assertTrue(result.getProcessedFsuCodes().contains("FSU-001"));
        assertTrue(result.isSuccess());
    }

    // ==================== OFFLINE FSU ====================

    @Test
    void shouldSkipOfflineFsu() {
        addFsu("FSU-001");
        setOffline("FSU-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedOfflineCount());
        assertEquals(0, result.getGetDataSuccessCount());
    }

    // ==================== 未登录 ====================

    @Test
    void shouldSkipNotLoggedInFsu() {
        addFsu("FSU-001");
        BInterfaceFsuStatusEntity status = new BInterfaceFsuStatusEntity();
        status.setFsuCode("FSU-001");
        status.setOnlineStatus("ONLINE");
        status.setLoginStatus("LOGOUT");
        loginService.statusStore.put("FSU-001", status);

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedNotLoggedInCount());
        assertEquals(0, result.getGetDataSuccessCount());
    }

    // ==================== 无 target 跳过 ====================

    @Test
    void shouldSkipFsuWithNoTargets() {
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedNoSignalCount());
        assertEquals(0, result.getGetDataSuccessCount());
    }

    // ==================== GET_DATA 失败 ====================

    @Test
    void shouldHandleGetDataFailure() {
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");
        getDataService.shouldFail = true;

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(0, result.getGetDataSuccessCount());
        assertEquals(1, result.getGetDataFailureCount());
        assertEquals(0, result.getSkippedNoSignalCount());
    }

    // ==================== 单个 FSU 失败不影响其他 ====================

    @Test
    void shouldContinueWhenOneFsuFails() {
        addFsu("FSU-001");
        addFsu("FSU-002");
        setOnlineAndLoggedIn("FSU-001");
        setOnlineAndLoggedIn("FSU-002");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");
        addTarget("FSU-002", BInterfacePkType.GET_DATA, "TEMP-001");
        getDataService.failFsuCodes.add("FSU-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(2, result.getScannedFsuCount());
        assertEquals(1, result.getGetDataSuccessCount());
        assertEquals(1, result.getGetDataFailureCount());
    }

    // ==================== GET_THRESHOLD 开关 ====================

    @Test
    void shouldNotExecuteGetThresholdWhenDisabled() {
        properties.setPollGetThreshold(false);
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(0, result.getGetThresholdSuccessCount());
        assertEquals(0, result.getGetThresholdFailureCount());
    }

    @Test
    void shouldExecuteGetThresholdWhenEnabled() {
        properties.setPollGetThreshold(true);
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");
        addTarget("FSU-001", BInterfacePkType.GET_THRESHOLD, "TEMP-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getGetDataSuccessCount());
        assertEquals(1, result.getGetThresholdSuccessCount());
        assertEquals(0, result.getGetThresholdFailureCount());
    }

    @Test
    void shouldHandleGetThresholdFailure() {
        properties.setPollGetThreshold(true);
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");
        addTarget("FSU-001", BInterfacePkType.GET_THRESHOLD, "TEMP-001");
        getThresholdService.shouldFail = true;

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getGetDataSuccessCount());
        assertEquals(0, result.getGetThresholdSuccessCount());
        assertEquals(1, result.getGetThresholdFailureCount());
    }

    // ==================== maxFsuPerRun ====================

    @Test
    void shouldRespectMaxFsuPerRun() {
        properties.setMaxFsuPerRun(1);
        addFsu("FSU-001");
        addFsu("FSU-002");
        setOnlineAndLoggedIn("FSU-001");
        setOnlineAndLoggedIn("FSU-002");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");
        addTarget("FSU-002", BInterfacePkType.GET_DATA, "TEMP-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getGetDataSuccessCount());
    }

    // ==================== realCallEnabled ====================

    @Test
    void shouldReportRealCallDisabledByDefault() {
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");

        SlowDataPollingResult result = service.runOnce();

        assertFalse(result.isRealCallEnabled());
    }

    @Test
    void shouldReportRealCallEnabledWhenConfigured() {
        properties.setAllowRealCall(true);
        addFsu("FSU-001");
        setOnlineAndLoggedIn("FSU-001");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");

        SlowDataPollingResult result = service.runOnce();

        assertTrue(result.isRealCallEnabled());
    }

    // ==================== errors ====================

    @Test
    void shouldCollectErrors() {
        addFsu("FSU-001");
        addFsu("FSU-002");
        setOnlineAndLoggedIn("FSU-001");
        setOnlineAndLoggedIn("FSU-002");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");
        addTarget("FSU-002", BInterfacePkType.GET_DATA, "TEMP-001");
        getDataService.shouldFail = true;

        SlowDataPollingResult result = service.runOnce();

        assertFalse(result.getErrors().isEmpty());
    }

    // ==================== 无 FSU 状态 ====================

    @Test
    void shouldSkipFsuWithNoStatus() {
        addFsu("FSU-001");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(1, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedOfflineCount());
    }

    // ==================== 混合场景 ====================

    @Test
    void shouldHandleMixedFsuStates() {
        addFsu("FSU-001"); // ONLINE + logged in + has targets
        addFsu("FSU-002"); // OFFLINE → skip
        addFsu("FSU-003"); // ONLINE + not logged in → skip
        addFsu("FSU-004"); // ONLINE + logged in + no targets → skippedNoSignal
        addFsu("FSU-005"); // ONLINE + logged in + has targets → GET_DATA fail
        setOnlineAndLoggedIn("FSU-001");
        setOffline("FSU-002");
        BInterfaceFsuStatusEntity status3 = new BInterfaceFsuStatusEntity();
        status3.setFsuCode("FSU-003");
        status3.setOnlineStatus("ONLINE");
        status3.setLoginStatus("LOGOUT");
        loginService.statusStore.put("FSU-003", status3);
        setOnlineAndLoggedIn("FSU-004");
        setOnlineAndLoggedIn("FSU-005");
        addTarget("FSU-001", BInterfacePkType.GET_DATA, "TEMP-001");
        addTarget("FSU-005", BInterfacePkType.GET_DATA, "TEMP-001");
        getDataService.failFsuCodes.add("FSU-005");

        SlowDataPollingResult result = service.runOnce();

        assertEquals(5, result.getScannedFsuCount());
        assertEquals(1, result.getSkippedOfflineCount());
        assertEquals(1, result.getSkippedNotLoggedInCount());
        assertEquals(1, result.getSkippedNoSignalCount());
        assertEquals(1, result.getGetDataSuccessCount());
        assertEquals(1, result.getGetDataFailureCount());
        assertTrue(result.getProcessedFsuCodes().contains("FSU-001"));
        assertFalse(result.getProcessedFsuCodes().contains("FSU-002"));
    }

    // ==================== Stubs ====================

    static class StubFsuDeviceRepository implements FsuDeviceRepository {
        final Map<Long, FsuDeviceEntity> store = new HashMap<>();
        long nextId = 1;

        @Override public FsuDeviceEntity save(FsuDeviceEntity entity) {
            if (entity.getId() == null) entity.setId(nextId++);
            store.put(entity.getId(), entity);
            return entity;
        }
        @Override public List<FsuDeviceEntity> findAll() { return List.copyOf(store.values()); }
        @Override public Optional<FsuDeviceEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> fsuCode.equals(e.getFsuCode())).findFirst();
        }
        @Override public List<FsuDeviceEntity> findBySiteId(Long siteId) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String status) { return List.of(); }
        @Override public Optional<FsuDeviceEntity> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<FsuDeviceEntity> findAll(Sort sort) { return List.copyOf(store.values()); }
        @Override public List<FsuDeviceEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(FsuDeviceEntity entity) { store.remove(entity.getId()); }
        @Override public void deleteAll(Iterable<? extends FsuDeviceEntity> entities) { entities.forEach(e -> store.remove(e.getId())); }
        @Override public void deleteAll() { store.clear(); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public void flush() {}
        @Override public <S extends FsuDeviceEntity> S saveAndFlush(S entity) { return (S) save(entity); }
        @Override public <S extends FsuDeviceEntity> List<S> saveAll(Iterable<S> entities) { List<S> r = new ArrayList<>(); for (S e : entities) r.add((S) save(e)); return r; }
        @Override public <S extends FsuDeviceEntity> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
        @Override public void deleteAllInBatch(Iterable<FsuDeviceEntity> entities) { entities.forEach(e -> store.remove(e.getId())); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { ids.forEach(store::remove); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(store::remove); }
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public FsuDeviceEntity getOne(Long id) { return store.get(id); }
        @Override public FsuDeviceEntity getById(Long id) { return store.get(id); }
        @Override public FsuDeviceEntity getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends FsuDeviceEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends FsuDeviceEntity> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends FsuDeviceEntity> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> example, Sort sort) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public org.springframework.data.domain.Page<FsuDeviceEntity> findAll(org.springframework.data.domain.Pageable pageable) { return new PageImpl<>(List.copyOf(store.values())); }
        @Override public <S extends FsuDeviceEntity, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }

    static class StubLoginService extends LoginService {
        final Map<String, BInterfaceFsuStatusEntity> statusStore = new HashMap<>();
        final Set<String> loggedInSet = new HashSet<>();

        StubLoginService() { super(null, null, null); }

        @Override public boolean isLoggedIn(String fsuCode) { return loggedInSet.contains(fsuCode); }
        @Override public Optional<BInterfaceFsuStatusEntity> getStatus(String fsuCode) {
            return Optional.ofNullable(statusStore.get(fsuCode));
        }
        @Override public Optional<BInterfaceSessionEntity> getActiveSession(String fsuCode) { return Optional.empty(); }
    }

    static class StubGetDataService extends GetDataService {
        boolean shouldFail = false;
        final Set<String> failFsuCodes = new HashSet<>();

        StubGetDataService() { super(null, null); }

        @Override
        public GetDataResult execute(String fsuCode, String serviceUrl, List<String> signalIds) {
            assertNotNull(signalIds);
            assertFalse(signalIds.isEmpty(), "GET_DATA 不应传空 signalId 列表");
            if (shouldFail || failFsuCodes.contains(fsuCode)) {
                return GetDataResult.fail("5001", "模拟 GET_DATA 失败", fsuCode);
            }
            return GetDataResult.success(fsuCode, List.of());
        }
    }

    static class StubGetThresholdService extends GetThresholdService {
        boolean shouldFail = false;

        StubGetThresholdService() { super(null, null); }

        @Override
        public GetThresholdResult execute(String fsuCode, String serviceUrl, List<String> signalIds) {
            assertNotNull(signalIds);
            assertFalse(signalIds.isEmpty(), "GET_THRESHOLD 不应传空 signalId 列表");
            if (shouldFail) {
                return GetThresholdResult.fail("5001", "模拟 GET_THRESHOLD 失败", fsuCode);
            }
            return GetThresholdResult.success(fsuCode, List.of());
        }
    }

    static class StubSignalPollingTargetService extends SignalPollingTargetService {
        final Map<String, List<SignalPollingTarget>> targetsByFsu = new HashMap<>();

        StubSignalPollingTargetService() { super(null, null); }

        void addTarget(String fsuCode, BInterfacePkType commandType, String signalId) {
            String key = fsuCode + ":" + commandType.name();
            targetsByFsu.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new SignalPollingTarget(fsuCode, "device-1", signalId, commandType, true));
        }

        @Override
        public List<SignalPollingTarget> findTargets(String fsuCode, BInterfacePkType commandType) {
            String key = fsuCode + ":" + commandType.name();
            return targetsByFsu.getOrDefault(key, List.of());
        }

        @Override
        public boolean hasTargets(String fsuCode, BInterfacePkType commandType) {
            return !findTargets(fsuCode, commandType).isEmpty();
        }
    }
}
