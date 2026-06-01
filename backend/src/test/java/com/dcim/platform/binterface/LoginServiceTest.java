package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.binterface.service.LoginResult;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 LoginService 单元测试。
 *
 * 使用内存 HashMap 替代 JPA Repository，不依赖数据库。
 */
class LoginServiceTest {

    private LoginService loginService;

    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubFsuStatusRepository fsuStatusRepo;
    private StubSessionRepository sessionRepo;

    @BeforeEach
    void setUp() {
        fsuDeviceRepo = new StubFsuDeviceRepository();
        fsuStatusRepo = new StubFsuStatusRepository();
        sessionRepo = new StubSessionRepository();

        // 预注册一个 FSU 设备
        FsuDeviceEntity existing = new FsuDeviceEntity();
        existing.setId(1L);
        existing.setFsuCode("FSU-001");
        existing.setFsuName("测试FSU-001");
        existing.setStatus("ONLINE");
        existing.setCreatedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(existing);

        loginService = new LoginService(fsuDeviceRepo, fsuStatusRepo, sessionRepo);
    }

    // ==================== 登录成功 ====================

    @Test
    void shouldLoginSuccessfully() {
        LoginResult result = loginService.login("FSU-001", "192.168.1.100");

        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");
        assertEquals("FSU-001", result.getFsuCode());
        assertNotNull(result.getSessionId());
        assertTrue(result.getSessionId().startsWith("SESSION-FSU-001-"));
        assertNotNull(result.getLoginTime());
    }

    @Test
    void shouldCreateSessionOnLogin() {
        loginService.login("FSU-001", null);

        Optional<BInterfaceSessionEntity> session = loginService.getActiveSession("FSU-001");
        assertTrue(session.isPresent());
        assertEquals("ACTIVE", session.get().getStatus());
        assertEquals("FSU-001", session.get().getFsuCode());
        assertNotNull(session.get().getSessionId());
        assertNotNull(session.get().getLoginTime());
    }

    @Test
    void shouldUpdateFsuStatusOnLogin() {
        loginService.login("FSU-001", null);

        Optional<BInterfaceFsuStatusEntity> status = loginService.getStatus("FSU-001");
        assertTrue(status.isPresent());
        assertEquals("LOGIN", status.get().getLoginStatus());
        assertEquals("ONLINE", status.get().getOnlineStatus());
        assertNotNull(status.get().getLastLoginTime());
    }

    // ==================== 重复登录 ====================

    @Test
    void shouldMarkDuplicateWithin120s() {
        LoginResult first = loginService.login("FSU-001", null);
        LoginResult second = loginService.login("FSU-001", null);

        assertTrue(second.isSuccess());
        assertTrue(second.isDuplicateWithin120s(),
                "Repeat login within 120s must be marked duplicate");
        assertEquals("ACCEPTED_DUPLICATE", second.getRegisterDecision());
        assertTrue(second.getRegisterIntervalSeconds() < 120);
        // Same session reused for duplicate
        assertEquals(first.getSessionId(), second.getSessionId());
    }

    // ==================== 缺少 FSUCode ====================

    @Test
    void shouldFailOnNullFsuCode() {
        LoginResult result = loginService.login(null, null);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnEmptyFsuCode() {
        LoginResult result = loginService.login("", null);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnBlankFsuCode() {
        LoginResult result = loginService.login("   ", null);
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    // ==================== 未知 FSU ====================

    @Test
    void shouldFailOnUnknownFsu() {
        LoginResult result = loginService.login("FSU-UNKNOWN", null);
        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
        assertTrue(result.getResultDesc().contains("未注册"));
    }

    // ==================== Session 管理 ====================

    @Test
    void isLoggedInShouldBeFalseBeforeLogin() {
        assertFalse(loginService.isLoggedIn("FSU-001"));
    }

    @Test
    void isLoggedInShouldBeTrueAfterLogin() {
        loginService.login("FSU-001", null);
        assertTrue(loginService.isLoggedIn("FSU-001"));
    }

    @Test
    void updateLastSeenShouldUpdateTimestamp() {
        loginService.login("FSU-001", null);
        loginService.updateLastSeen("FSU-001");

        Optional<BInterfaceFsuStatusEntity> status = loginService.getStatus("FSU-001");
        assertTrue(status.isPresent());
        assertEquals("ONLINE", status.get().getOnlineStatus());
        assertNotNull(status.get().getLastHeartbeat());
    }

    @Test
    void clearSessionShouldLogout() {
        loginService.login("FSU-001", null);
        loginService.clearSession("FSU-001");

        assertFalse(loginService.isLoggedIn("FSU-001"));

        Optional<BInterfaceFsuStatusEntity> status = loginService.getStatus("FSU-001");
        assertTrue(status.isPresent());
        assertEquals("LOGOUT", status.get().getLoginStatus());
        assertEquals("OFFLINE", status.get().getOnlineStatus());
    }

    @Test
    void clearSessionShouldUpdateSessionStatus() {
        loginService.login("FSU-001", null);
        loginService.clearSession("FSU-001");

        Optional<BInterfaceSessionEntity> session = loginService.getActiveSession("FSU-001");
        assertTrue(session.isEmpty()); // 不应再有 ACTIVE session
    }

    @Test
    void getActiveSessionShouldReturnEmptyBeforeLogin() {
        assertTrue(loginService.getActiveSession("FSU-001").isEmpty());
    }

    // ==================== 边界情况 ====================

    @Test
    void updateLastSeenShouldNotThrowForUnknownFsu() {
        // 对不存在的 FSU 调用应安静跳过
        loginService.updateLastSeen("FSU-NONEXISTENT");
        // 不应抛异常
    }

    @Test
    void clearSessionShouldNotThrowForUnknownFsu() {
        loginService.clearSession("FSU-NONEXISTENT");
    }

    @Test
    void getActiveSessionShouldReturnEmptyForUnknownFsu() {
        assertTrue(loginService.getActiveSession("FSU-UNKNOWN").isEmpty());
    }

    @Test
    void loginShouldUpdateFsuDeviceRegisterTime() {
        loginService.login("FSU-001", null);
        Optional<FsuDeviceEntity> fsu = fsuDeviceRepo.findByFsuCode("FSU-001");
        assertTrue(fsu.isPresent());
        assertNotNull(fsu.get().getRegisterTime());
        assertNotNull(fsu.get().getLastOnlineTime());
    }

    // ==================== Stub Repository 实现 ====================

    /**
     * 所有 Stub 仓库的公用的 JPA 方法基类。
     * 使用 @SuppressWarnings 处理泛型类型擦除。
     */
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
                    .collect(java.util.stream.Collectors.toList());
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
