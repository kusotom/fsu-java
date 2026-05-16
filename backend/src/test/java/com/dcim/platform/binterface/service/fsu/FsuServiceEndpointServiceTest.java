package com.dcim.platform.binterface.service.fsu;

import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResult;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceEndpointService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FsuServiceEndpointService 单元测试。
 *
 * <p>验证 FSU endpoint 解析的各种场景：正常解析、设备未找到、地址缺失等。</p>
 */
class FsuServiceEndpointServiceTest {

    private FsuServiceEndpointService endpointService;
    private StubFsuDeviceRepository deviceRepo;

    @BeforeEach
    void setUp() {
        deviceRepo = new StubFsuDeviceRepository();
        endpointService = new FsuServiceEndpointService(deviceRepo);
    }

    // ==================== 成功场景 ====================

    @Test
    void shouldResolveFromIpAndPort() {
        deviceRepo.addDevice(device("FSU-001", "192.168.1.100", 8080));

        FsuEndpointResult result = endpointService.resolve("FSU-001");

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertEquals("http://192.168.1.100:8080/services/FSUService", result.getServiceUrl());
        assertFalse(result.isFromExplicitServiceUrl());
        assertTrue(result.isFromHostPort());
    }

    @Test
    void shouldResolveWithDifferentIpAndPort() {
        deviceRepo.addDevice(device("FSU-002", "10.0.0.50", 9090));

        FsuEndpointResult result = endpointService.resolve("FSU-002");

        assertTrue(result.isSuccess());
        assertEquals("http://10.0.0.50:9090/services/FSUService", result.getServiceUrl());
    }

    @Test
    void shouldResolveWithIpv6Address() {
        deviceRepo.addDevice(device("FSU-003", "2001:db8::1", 8080));

        FsuEndpointResult result = endpointService.resolve("FSU-003");

        assertTrue(result.isSuccess());
        assertEquals("http://2001:db8::1:8080/services/FSUService", result.getServiceUrl());
    }

    // ==================== 失败场景 ====================

    @Test
    void shouldFailWhenDeviceNotFound() {
        FsuEndpointResult result = endpointService.resolve("FSU-NOT-FOUND");

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
        assertTrue(result.getResultDesc().contains("FSU-NOT-FOUND"));
        assertNull(result.getServiceUrl());
    }

    @Test
    void shouldFailWhenNullFsuCode() {
        FsuEndpointResult result = endpointService.resolve(null);

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
        assertNull(result.getServiceUrl());
    }

    @Test
    void shouldFailWhenEmptyFsuCode() {
        FsuEndpointResult result = endpointService.resolve("");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailWhenWhitespaceFsuCode() {
        FsuEndpointResult result = endpointService.resolve("   ");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailWhenIpAddrIsNull() {
        deviceRepo.addDevice(device("FSU-001", null, 8080));

        FsuEndpointResult result = endpointService.resolve("FSU-001");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("IP"));
    }

    @Test
    void shouldFailWhenIpAddrIsEmpty() {
        deviceRepo.addDevice(device("FSU-001", "", 8080));

        FsuEndpointResult result = endpointService.resolve("FSU-001");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("IP"));
    }

    @Test
    void shouldFailWhenPortIsNull() {
        deviceRepo.addDevice(device("FSU-001", "192.168.1.100", null));

        FsuEndpointResult result = endpointService.resolve("FSU-001");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("端口"));
    }

    @Test
    void shouldFailWhenPortIsZero() {
        deviceRepo.addDevice(device("FSU-001", "192.168.1.100", 0));

        FsuEndpointResult result = endpointService.resolve("FSU-001");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("端口"));
    }

    @Test
    void shouldFailWhenPortIsNegative() {
        deviceRepo.addDevice(device("FSU-001", "192.168.1.100", -1));

        FsuEndpointResult result = endpointService.resolve("FSU-001");

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
        assertTrue(result.getResultDesc().contains("端口"));
    }

    // ==================== buildServiceUrl ====================

    @Test
    void buildServiceUrlShouldConstructCorrectUrl() {
        String url = FsuServiceEndpointService.buildServiceUrl("192.168.1.100", 8080);
        assertEquals("http://192.168.1.100:8080/services/FSUService", url);
    }

    @Test
    void buildServiceUrlShouldWorkWithStandardPort() {
        String url = FsuServiceEndpointService.buildServiceUrl("10.0.0.1", 80);
        assertEquals("http://10.0.0.1:80/services/FSUService", url);
    }

    // ==================== isValidUrl ====================

    @Test
    void isValidUrlShouldAcceptHttp() {
        assertTrue(FsuServiceEndpointService.isValidUrl("http://192.168.1.100:8080/services/FSUService"));
    }

    @Test
    void isValidUrlShouldAcceptHttps() {
        assertTrue(FsuServiceEndpointService.isValidUrl("https://192.168.1.100:8443/services/FSUService"));
    }

    @Test
    void isValidUrlShouldRejectNull() {
        assertFalse(FsuServiceEndpointService.isValidUrl(null));
    }

    @Test
    void isValidUrlShouldRejectEmpty() {
        assertFalse(FsuServiceEndpointService.isValidUrl(""));
    }

    @Test
    void isValidUrlShouldRejectNonHttp() {
        assertFalse(FsuServiceEndpointService.isValidUrl("ftp://192.168.1.100"));
    }

    @Test
    void isValidUrlShouldRejectTooShort() {
        assertFalse(FsuServiceEndpointService.isValidUrl("http://a"));
    }

    @Test
    void isValidUrlShouldAcceptCaseInsensitive() {
        assertTrue(FsuServiceEndpointService.isValidUrl("HTTP://192.168.1.100:8080/path"));
        assertTrue(FsuServiceEndpointService.isValidUrl("HTTPS://192.168.1.100:8443/path"));
    }

    // ==================== FsuEndpointResult factories ====================

    @Test
    void fsuEndpointResultFromHostPortShouldSetFlags() {
        FsuEndpointResult r = FsuEndpointResult.fromHostPort("FSU-001", "http://host:8080/path");
        assertTrue(r.isSuccess());
        assertFalse(r.isFromExplicitServiceUrl());
        assertTrue(r.isFromHostPort());
        assertEquals("0", r.getResultCode());
    }

    @Test
    void fsuEndpointResultFromExplicitServiceUrlShouldSetFlags() {
        FsuEndpointResult r = FsuEndpointResult.fromExplicitServiceUrl("FSU-001", "http://host:8080/path");
        assertTrue(r.isSuccess());
        assertTrue(r.isFromExplicitServiceUrl());
        assertFalse(r.isFromHostPort());
        assertEquals("0", r.getResultCode());
    }

    @Test
    void fsuEndpointResultFailShouldReturnError() {
        FsuEndpointResult r = FsuEndpointResult.fail("FSU-001", "2001", "error");
        assertFalse(r.isSuccess());
        assertNull(r.getServiceUrl());
        assertEquals("2001", r.getResultCode());
        assertEquals("error", r.getResultDesc());
        assertFalse(r.isFromExplicitServiceUrl());
        assertFalse(r.isFromHostPort());
    }

    // ==================== Stub Repository ====================

    static class StubFsuDeviceRepository implements FsuDeviceRepository {
        final Map<String, FsuDeviceEntity> store = new HashMap<>();

        void addDevice(FsuDeviceEntity entity) {
            store.put(entity.getFsuCode(), entity);
        }

        @Override
        public Optional<FsuDeviceEntity> findByFsuCode(String fsuCode) {
            return Optional.ofNullable(store.get(fsuCode));
        }

        @Override
        public List<FsuDeviceEntity> findBySiteId(Long siteId) { return List.of(); }

        @Override
        public List<FsuDeviceEntity> findByStatus(String status) {
            return store.values().stream().filter(e -> status.equals(e.getStatus())).toList();
        }

        // JpaRepository methods — only used ones are implemented
        final Map<Long, FsuDeviceEntity> idStore = new HashMap<>();

        @Override
        public FsuDeviceEntity save(FsuDeviceEntity entity) {
            if (entity.getId() == null) entity.setId((long) (idStore.size() + 1));
            idStore.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public Optional<FsuDeviceEntity> findById(Long id) { return Optional.ofNullable(idStore.get(id)); }

        @Override
        public boolean existsById(Long id) { return idStore.containsKey(id); }

        @Override
        public List<FsuDeviceEntity> findAll() { return List.copyOf(idStore.values()); }

        @Override
        public List<FsuDeviceEntity> findAllById(Iterable<Long> ids) { return List.of(); }

        @Override
        public long count() { return idStore.size(); }

        @Override
        public void deleteById(Long id) { idStore.remove(id); }

        @Override
        public void delete(FsuDeviceEntity entity) { if (entity.getId() != null) idStore.remove(entity.getId()); }

        @Override
        public void deleteAll(Iterable<? extends FsuDeviceEntity> entities) { entities.forEach(e -> idStore.remove(e.getId())); }

        @Override
        public void deleteAll() { idStore.clear(); }

        @Override
        public void flush() {}

        @Override
        public <S extends FsuDeviceEntity> S saveAndFlush(S entity) { save(entity); return entity; }

        @Override
        public <S extends FsuDeviceEntity> List<S> saveAll(Iterable<S> entities) {
            List<S> r = new ArrayList<>(); entities.forEach(e -> { save(e); r.add(e); }); return r;
        }

        @Override
        public <S extends FsuDeviceEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
            return saveAll(entities);
        }

        @Override
        public FsuDeviceEntity getReferenceById(Long id) { return idStore.get(id); }

        @Override
        public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(idStore::remove); }

        @Override
        public void deleteAllInBatch() { idStore.clear(); }

        @Override
        public void deleteAllInBatch(Iterable<FsuDeviceEntity> entities) { entities.forEach(e -> idStore.remove(e.getId())); }

        @Override
        public void deleteAllByIdInBatch(Iterable<Long> ids) { ids.forEach(idStore::remove); }

        @Override
        public FsuDeviceEntity getOne(Long id) { return idStore.get(id); }

        @Override
        public FsuDeviceEntity getById(Long id) { return idStore.get(id); }

        @Override
        public <S extends FsuDeviceEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }

        @Override
        public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> example) { return List.of(); }

        @Override
        public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return List.of(); }

        @Override
        public <S extends FsuDeviceEntity> long count(org.springframework.data.domain.Example<S> example) { return 0; }

        @Override
        public <S extends FsuDeviceEntity> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }

        @Override
        public <S extends FsuDeviceEntity, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }

        @Override
        public org.springframework.data.domain.Page<FsuDeviceEntity> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }

        @Override
        public <S extends FsuDeviceEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }

        @Override
        public List<FsuDeviceEntity> findAll(org.springframework.data.domain.Sort sort) { return List.copyOf(idStore.values()); }
    }

    private FsuDeviceEntity device(String fsuCode, String ipAddr, Integer port) {
        FsuDeviceEntity e = new FsuDeviceEntity();
        e.setFsuCode(fsuCode);
        e.setIpAddr(ipAddr);
        e.setPort(port);
        e.setFsuName("test");
        e.setStatus("ONLINE");
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }
}
