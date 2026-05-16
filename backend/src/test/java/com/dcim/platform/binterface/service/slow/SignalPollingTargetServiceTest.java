package com.dcim.platform.binterface.service.slow;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.slow.SignalPollingTarget;
import com.dcim.platform.module.binterface.service.slow.SignalPollingTargetService;
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

class SignalPollingTargetServiceTest {

    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubMonitoringPointRepository monitoringPointRepo;
    private SignalPollingTargetService service;

    @BeforeEach
    void setUp() {
        fsuDeviceRepo = new StubFsuDeviceRepository();
        monitoringPointRepo = new StubMonitoringPointRepository();
        service = new SignalPollingTargetService(fsuDeviceRepo, monitoringPointRepo);
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
        fsuDeviceRepo.save(fsu);
        return fsu;
    }

    private void addPoint(Long fsuId, String pointCode, String status) {
        MonitoringPointEntity point = new MonitoringPointEntity();
        point.setFsuId(fsuId);
        point.setPointCode(pointCode);
        point.setPointName(pointCode);
        point.setPointType("analog");
        point.setDataType("float");
        point.setStatus(status);
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        monitoringPointRepo.save(point);
    }

    // ==================== GET_DATA targets ====================

    @Test
    void shouldReturnGetDataTargets() {
        FsuDeviceEntity fsu = addFsu("FSU-001");
        addPoint(fsu.getId(), "TEMP-001", "ACTIVE");
        addPoint(fsu.getId(), "HUMI-001", "ACTIVE");

        List<SignalPollingTarget> targets = service.findTargets("FSU-001", BInterfacePkType.GET_DATA);

        assertEquals(2, targets.size());
        assertTrue(targets.stream().anyMatch(t -> "TEMP-001".equals(t.getSignalId())));
        assertTrue(targets.stream().anyMatch(t -> "HUMI-001".equals(t.getSignalId())));
        assertEquals("FSU-001", targets.get(0).getFsuCode());
        assertEquals(BInterfacePkType.GET_DATA, targets.get(0).getCommandType());
        assertTrue(targets.get(0).isEnabled());
    }

    // ==================== GET_THRESHOLD targets ====================

    @Test
    void shouldReturnGetThresholdTargets() {
        FsuDeviceEntity fsu = addFsu("FSU-001");
        addPoint(fsu.getId(), "TEMP-001", "ACTIVE");

        List<SignalPollingTarget> targets = service.findTargets("FSU-001", BInterfacePkType.GET_THRESHOLD);

        assertEquals(1, targets.size());
        assertEquals("TEMP-001", targets.get(0).getSignalId());
        assertEquals(BInterfacePkType.GET_THRESHOLD, targets.get(0).getCommandType());
    }

    // ==================== 空列表 ====================

    @Test
    void shouldReturnEmptyForFsuWithNoPoints() {
        addFsu("FSU-001");

        List<SignalPollingTarget> targets = service.findTargets("FSU-001", BInterfacePkType.GET_DATA);

        assertTrue(targets.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNonExistentFsu() {
        List<SignalPollingTarget> targets = service.findTargets("FSU-NONEXIST", BInterfacePkType.GET_DATA);

        assertTrue(targets.isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullFsuCode() {
        List<SignalPollingTarget> targets = service.findTargets(null, BInterfacePkType.GET_DATA);

        assertTrue(targets.isEmpty());
    }

    @Test
    void shouldReturnEmptyForEmptyFsuCode() {
        List<SignalPollingTarget> targets = service.findTargets("", BInterfacePkType.GET_DATA);

        assertTrue(targets.isEmpty());
    }

    // ==================== disabled 过滤 ====================

    @Test
    void shouldFilterDisabledPoints() {
        FsuDeviceEntity fsu = addFsu("FSU-001");
        addPoint(fsu.getId(), "TEMP-001", "ACTIVE");
        addPoint(fsu.getId(), "HUMI-001", "INACTIVE");
        addPoint(fsu.getId(), "VOLT-001", "DISABLED");

        List<SignalPollingTarget> targets = service.findTargets("FSU-001", BInterfacePkType.GET_DATA);

        assertEquals(1, targets.size());
        assertEquals("TEMP-001", targets.get(0).getSignalId());
    }

    // ==================== not GET_DATA / GET_THRESHOLD ====================

    @Test
    void shouldReturnEmptyForUnsupportedCommandType() {
        FsuDeviceEntity fsu = addFsu("FSU-001");
        addPoint(fsu.getId(), "TEMP-001", "ACTIVE");

        List<SignalPollingTarget> targets = service.findTargets("FSU-001", BInterfacePkType.SET_THRESHOLD);

        assertTrue(targets.isEmpty());
    }

    // ==================== hasTargets ====================

    @Test
    void hasTargetsShouldReturnTrueWhenTargetsExist() {
        FsuDeviceEntity fsu = addFsu("FSU-001");
        addPoint(fsu.getId(), "TEMP-001", "ACTIVE");

        assertTrue(service.hasTargets("FSU-001", BInterfacePkType.GET_DATA));
    }

    @Test
    void hasTargetsShouldReturnFalseWhenNoTargets() {
        addFsu("FSU-001");

        assertFalse(service.hasTargets("FSU-001", BInterfacePkType.GET_DATA));
    }

    // ==================== 多 FSU ====================

    @Test
    void shouldRespectFsuBoundary() {
        FsuDeviceEntity fsu1 = addFsu("FSU-001");
        FsuDeviceEntity fsu2 = addFsu("FSU-002");
        addPoint(fsu1.getId(), "TEMP-001", "ACTIVE");
        addPoint(fsu2.getId(), "HUMI-001", "ACTIVE");

        List<SignalPollingTarget> targets1 = service.findTargets("FSU-001", BInterfacePkType.GET_DATA);
        List<SignalPollingTarget> targets2 = service.findTargets("FSU-002", BInterfacePkType.GET_DATA);

        assertEquals(1, targets1.size());
        assertEquals("TEMP-001", targets1.get(0).getSignalId());
        assertEquals(1, targets2.size());
        assertEquals("HUMI-001", targets2.get(0).getSignalId());
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
        public <S extends T> List<S> saveAll(Iterable<S> entities) { List<S> r = new ArrayList<>(); for (S e : entities) r.add((S) saveEntity((T) e, getId((T) e))); return r; }
        public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
        public <S extends T> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        public <S extends T> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        public <S extends T> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> example, Sort sort) { return List.of(); }
        public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable pageable) { return new PageImpl<>(List.copyOf(store.values())); }
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
}
