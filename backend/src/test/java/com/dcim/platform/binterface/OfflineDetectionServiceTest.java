package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.binterface.service.OfflineDetectionResult;
import com.dcim.platform.module.binterface.service.OfflineDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 OfflineDetectionService 单元测试。
 *
 * <p>使用内存 HashMap 替代 JPA Repository，不依赖数据库。</p>
 */
class OfflineDetectionServiceTest {

    private OfflineDetectionService offlineDetectionService;

    private StubFsuStatusRepository fsuStatusRepo;
    private StubSessionRepository sessionRepo;

    private static final long HEARTBEAT_TIMEOUT_SECS = 300;

    @BeforeEach
    void setUp() {
        fsuStatusRepo = new StubFsuStatusRepository();
        sessionRepo = new StubSessionRepository();

        offlineDetectionService = new OfflineDetectionService(fsuStatusRepo, sessionRepo);
    }

    // ==================== 基础场景 ====================

    @Test
    void shouldReturnEmptyWhenNoOnlineFsu() {
        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(LocalDateTime.now(), HEARTBEAT_TIMEOUT_SECS);

        assertEquals(0, result.getScannedCount());
        assertEquals(0, result.getOfflineCount());
        assertEquals(0, result.getOnlineCount());
        assertFalse(result.hasError());
    }

    @Test
    void shouldKeepOnlineFsuWithinTimeout() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(60)); // 1 分钟前心跳，未超时

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(1, result.getScannedCount());
        assertEquals(0, result.getOfflineCount());
        assertEquals(1, result.getOnlineCount());
        assertTrue(result.getOnlineFsuCodes().contains("FSU-001"));
    }

    @Test
    void shouldSetOfflineWhenHeartbeatExceedsTimeout() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(600)); // 10 分钟前心跳，超时

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(1, result.getScannedCount());
        assertEquals(1, result.getOfflineCount());
        assertEquals(0, result.getOnlineCount());
        assertTrue(result.getOfflineFsuCodes().contains("FSU-001"));
    }

    // ==================== 状态变化 ====================

    @Test
    void shouldChangeOnlineStatusToOffline() {
        LocalDateTime now = LocalDateTime.now();
        Long fsuId = givenOnlineFsu("FSU-001", now.minusSeconds(600));

        offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        BInterfaceFsuStatusEntity status = fsuStatusRepo.findByFsuCode("FSU-001").orElseThrow();
        assertEquals("OFFLINE", status.getOnlineStatus());
        assertEquals("LOGOUT", status.getLoginStatus());
    }

    @Test
    void shouldClearSessionOnOffline() {
        LocalDateTime now = LocalDateTime.now();
        Long fsuId = givenOnlineFsu("FSU-001", now.minusSeconds(600));
        givenActiveSession(fsuId, "FSU-001");

        offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        Optional<BInterfaceSessionEntity> session = sessionRepo.findByFsuIdAndStatus(fsuId, "ACTIVE");
        assertTrue(session.isEmpty());
    }

    @Test
    void shouldUpdateSessionStatusToLogout() {
        LocalDateTime now = LocalDateTime.now();
        Long fsuId = givenOnlineFsu("FSU-001", now.minusSeconds(600));
        givenActiveSession(fsuId, "FSU-001");

        offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        Optional<BInterfaceSessionEntity> session = sessionRepo.findByFsuIdAndStatus(fsuId, "LOGOUT");
        assertTrue(session.isPresent());
        assertNotNull(session.get().getLogoutTime());
    }

    @Test
    void shouldIncrementHeartbeatMissCount() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(600), 0);

        offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        BInterfaceFsuStatusEntity status = fsuStatusRepo.findByFsuCode("FSU-001").orElseThrow();
        assertEquals(1, status.getHeartbeatMissCount());
    }

    @Test
    void shouldIncrementExistingHeartbeatMissCount() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(600), 3);

        offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        BInterfaceFsuStatusEntity status = fsuStatusRepo.findByFsuCode("FSU-001").orElseThrow();
        assertEquals(4, status.getHeartbeatMissCount());
    }

    // ==================== 混合场景 ====================

    @Test
    void shouldHandleMixedOnlineAndOffline() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(60));   // 在线
        givenOnlineFsu("FSU-002", now.minusSeconds(600));  // 离线
        givenOnlineFsu("FSU-003", now.minusSeconds(120));  // 在线

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(3, result.getScannedCount());
        assertEquals(1, result.getOfflineCount());
        assertEquals(2, result.getOnlineCount());
        assertTrue(result.getOfflineFsuCodes().contains("FSU-002"));
        assertTrue(result.getOnlineFsuCodes().containsAll(List.of("FSU-001", "FSU-003")));
    }

    @Test
    void shouldHandleAllOffline() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(600));
        givenOnlineFsu("FSU-002", now.minusSeconds(900));

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(2, result.getScannedCount());
        assertEquals(2, result.getOfflineCount());
        assertEquals(0, result.getOnlineCount());
    }

    @Test
    void shouldHandleAllOnlineWithinTimeout() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(30));
        givenOnlineFsu("FSU-002", now.minusSeconds(120));

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(2, result.getScannedCount());
        assertEquals(0, result.getOfflineCount());
        assertEquals(2, result.getOnlineCount());
    }

    // ==================== 边界情况 ====================

    @Test
    void shouldSetOfflineWhenLastHeartbeatIsNull() {
        LocalDateTime now = LocalDateTime.now();
        Long fsuId = 1L;
        BInterfaceFsuStatusEntity status = new BInterfaceFsuStatusEntity();
        status.setFsuId(fsuId);
        status.setFsuCode("FSU-NO-HEARTBEAT");
        status.setLoginStatus("LOGIN");
        status.setOnlineStatus("ONLINE");
        status.setLastHeartbeat(null);
        status.setHeartbeatMissCount(0);
        status.setCreatedAt(now);
        status.setUpdatedAt(now);
        fsuStatusRepo.save(status);

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(1, result.getOfflineCount());
        assertTrue(result.getOfflineFsuCodes().contains("FSU-NO-HEARTBEAT"));

        BInterfaceFsuStatusEntity updated = fsuStatusRepo.findByFsuCode("FSU-NO-HEARTBEAT").orElseThrow();
        assertEquals("OFFLINE", updated.getOnlineStatus());
    }

    @Test
    void shouldHandleExactTimeoutBoundary() {
        LocalDateTime now = LocalDateTime.now();
        // 刚好在超时边界上 — lastHeartbeat == now - timeout，不应视为超时
        givenOnlineFsu("FSU-001", now.minusSeconds(HEARTBEAT_TIMEOUT_SECS));

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(1, result.getOnlineCount());
        assertEquals(0, result.getOfflineCount());
    }

    @Test
    void shouldHandleOneSecondPastTimeout() {
        LocalDateTime now = LocalDateTime.now();
        // 超过超时边界 1 秒 — 应视为超时
        givenOnlineFsu("FSU-001", now.minusSeconds(HEARTBEAT_TIMEOUT_SECS + 1));

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(0, result.getOnlineCount());
        assertEquals(1, result.getOfflineCount());
    }

    @Test
    void shouldHandleCustomTimeout() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(30)); // 30 秒前心跳

        // 使用 15 秒超时，则 FSU-001 应离线
        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, 15);

        assertEquals(1, result.getOfflineCount());
        assertTrue(result.getOfflineFsuCodes().contains("FSU-001"));
    }

    @Test
    void shouldNotFailWhenSessionNotFound() {
        LocalDateTime now = LocalDateTime.now();
        // FSU 有 ONLINE 状态但没有 ACTIVE session（数据异常情况）
        givenOnlineFsu("FSU-001", now.minusSeconds(600));
        // 不创建 session

        // 不应抛异常
        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(1, result.getOfflineCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void shouldNotFailWhenNoFsuStatusesExist() {
        // 数据库没有任何 FSU 状态记录
        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(LocalDateTime.now(), HEARTBEAT_TIMEOUT_SECS);

        assertEquals(0, result.getScannedCount());
        assertFalse(result.hasError());
    }

    @Test
    void shouldNotFailWhenOfflineStatusExists() {
        // 已有 OFFLINE 状态的 FSU 不应被扫描到
        LocalDateTime now = LocalDateTime.now();
        BInterfaceFsuStatusEntity offline = new BInterfaceFsuStatusEntity();
        offline.setFsuId(1L);
        offline.setFsuCode("FSU-OFFLINE");
        offline.setLoginStatus("LOGOUT");
        offline.setOnlineStatus("OFFLINE");
        offline.setLastHeartbeat(now.minusSeconds(3600)); // 很久以前
        offline.setHeartbeatMissCount(0);
        offline.setCreatedAt(now);
        offline.setUpdatedAt(now);
        fsuStatusRepo.save(offline);

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(0, result.getScannedCount());
        assertEquals(0, result.getOfflineCount());
    }

    // ==================== 输出 ====================

    @Test
    void resultShouldContainCorrectOfflineAndOnlineCounts() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(60));
        givenOnlineFsu("FSU-002", now.minusSeconds(600));
        givenOnlineFsu("FSU-003", now.minusSeconds(120));
        givenOnlineFsu("FSU-004", now.minusSeconds(800));

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        assertEquals(4, result.getScannedCount());
        assertEquals(2, result.getOfflineCount());
        assertEquals(2, result.getOnlineCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    void toStringShouldContainKeyFields() {
        LocalDateTime now = LocalDateTime.now();
        givenOnlineFsu("FSU-001", now.minusSeconds(600));

        OfflineDetectionResult result = offlineDetectionService.scanOfflineFsu(now, HEARTBEAT_TIMEOUT_SECS);

        String str = result.toString();
        assertTrue(str.contains("scannedCount=1"));
        assertTrue(str.contains("offlineCount=1"));
    }

    // ==================== Helper 方法 ====================

    /**
     * 创建一个 ONLINE 状态的 FSU。
     * @return 创建的 fsuId
     */
    private Long givenOnlineFsu(String fsuCode, LocalDateTime lastHeartbeat) {
        return givenOnlineFsu(fsuCode, lastHeartbeat, 0);
    }

    private Long givenOnlineFsu(String fsuCode, LocalDateTime lastHeartbeat, Integer missCount) {
        long fsuId = Long.parseLong(fsuCode.replaceAll("\\D", ""));
        if (fsuId == 0) fsuId = new Random().nextLong(1000) + 100;

        BInterfaceFsuStatusEntity status = new BInterfaceFsuStatusEntity();
        status.setFsuId(fsuId);
        status.setFsuCode(fsuCode);
        status.setLoginStatus("LOGIN");
        status.setOnlineStatus("ONLINE");
        status.setLastHeartbeat(lastHeartbeat);
        status.setHeartbeatMissCount(missCount);
        status.setCreatedAt(LocalDateTime.now());
        status.setUpdatedAt(LocalDateTime.now());
        fsuStatusRepo.save(status);
        return fsuId;
    }

    private void givenActiveSession(Long fsuId, String fsuCode) {
        BInterfaceSessionEntity session = new BInterfaceSessionEntity();
        session.setFsuId(fsuId);
        session.setFsuCode(fsuCode);
        session.setSessionId("SESSION-" + fsuCode + "-TEST");
        session.setStatus("ACTIVE");
        session.setLoginTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepo.save(session);
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

    static class StubFsuStatusRepository extends BaseStub<BInterfaceFsuStatusEntity> implements BInterfaceFsuStatusRepository {
        @Override protected Long getId(BInterfaceFsuStatusEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceFsuStatusEntity e, Long id) { e.setId(id); }

        @Override
        public BInterfaceFsuStatusEntity save(BInterfaceFsuStatusEntity entity) {
            return saveEntity(entity, entity.getId());
        }

        @Override
        public Optional<BInterfaceFsuStatusEntity> findByFsuCode(String fsuCode) {
            return store.values().stream()
                    .filter(e -> fsuCode.equals(e.getFsuCode()))
                    .findFirst();
        }

        @Override
        public Optional<BInterfaceFsuStatusEntity> findByFsuId(Long fsuId) {
            return store.values().stream()
                    .filter(e -> fsuId.equals(e.getFsuId()))
                    .findFirst();
        }

        @Override
        public List<BInterfaceFsuStatusEntity> findByOnlineStatus(String onlineStatus) {
            return store.values().stream()
                    .filter(e -> onlineStatus.equals(e.getOnlineStatus()))
                    .collect(Collectors.toList());
        }
    }

    static class StubSessionRepository extends BaseStub<BInterfaceSessionEntity> implements BInterfaceSessionRepository {
        @Override protected Long getId(BInterfaceSessionEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceSessionEntity e, Long id) { e.setId(id); }

        @Override
        public BInterfaceSessionEntity save(BInterfaceSessionEntity entity) {
            return saveEntity(entity, entity.getId());
        }

        @Override
        public Optional<BInterfaceSessionEntity> findBySessionId(String sessionId) {
            return store.values().stream()
                    .filter(e -> sessionId.equals(e.getSessionId()))
                    .findFirst();
        }

        @Override
        public Optional<BInterfaceSessionEntity> findByFsuIdAndStatus(Long fsuId, String status) {
            return store.values().stream()
                    .filter(e -> fsuId.equals(e.getFsuId()) && status.equals(e.getStatus()))
                    .findFirst();
        }
    }
}
