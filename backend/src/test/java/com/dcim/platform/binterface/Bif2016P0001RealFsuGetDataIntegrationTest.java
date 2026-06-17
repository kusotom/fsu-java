package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.BInterface2016GetDataResult;
import com.dcim.platform.module.binterface.service.BInterface2016GetDataService;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * BIF2016-P0-001: 真实 FSU GET_DATA DeviceList/TSemaphore run-once 验证。
 *
 * <pre>mvn test -Dtest='Bif2016P0001RealFsuGetDataIntegrationTest' -DrealFsuTest.enabled=true</pre>
 */
@Tag("real-fsu")
@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Bif2016P0001RealFsuGetDataIntegrationTest {

    private static final String FSU_CODE = "51051243812345";
    private static final String SERVICE_URL = "http://192.168.100.100:8080/services/FSUService";
    private static final Path RAW_DIR = Path.of("docs/landing/raw-samples");

    private static final List<String> ALL_DEVICES = List.of(
            "51051241820004", "51051241830004", "51051241840004",
            "51051240700002", "51051243812345");

    private static final SoapMessageHandler soapHandler = new SoapMessageHandler();
    private static final XmlDataParser xmlParser = new XmlDataParser();
    private static final FsuServiceRpcAdapter rpc = new FsuServiceRpcAdapter();
    private static final RealHttpFsuServiceClient client =
            new RealHttpFsuServiceClient(soapHandler, xmlParser, rpc, 5000, 10000);

    private BInterface2016GetDataService service;

    @BeforeEach
    void setUp() {
        assumeTrue(Boolean.getBoolean("realFsuTest.enabled"),
                "真实 FSU 测试需显式启用: -DrealFsuTest.enabled=true");

        FsuEndpointResolver resolver = fsuCode ->
                FsuEndpointResult.fromExplicitServiceUrl(fsuCode, SERVICE_URL);
        service = new BInterface2016GetDataService(client, resolver,
                new StubFsuDeviceRepo(), new StubPointRepo(), new StubRtRepo());

        try { Files.createDirectories(RAW_DIR); } catch (IOException ignored) {}
    }

    // ==================== 策略 1: 全 DeviceID 查询 ====================

    @Test @Order(1)
    void strategy1_allDevices() throws Exception {
        System.out.println("\n========== GET_DATA 策略1: 全DeviceID ==========");
        BInterface2016GetDataResult r = service.execute(FSU_CODE, ALL_DEVICES, null);
        printResult(r);
        saveRaw("all-devices", r);
        assertTrue(r.isSuccess() || r.isEmptyData(), "应成功或明确空数据");
    }

    // ==================== 策略 2: 单 DeviceID ====================

    @Test @Order(2)
    void strategy2_singleDevice() throws Exception {
        System.out.println("\n========== GET_DATA 策略2: 单DeviceID ==========");
        BInterface2016GetDataResult r = service.execute(FSU_CODE,
                List.of("51051241820004"), null);
        printResult(r);
        saveRaw("single-device", r);
    }

    // ==================== 策略 3: DeviceID 通配 ====================

    @Test @Order(3)
    void strategy3_wildcard() throws Exception {
        System.out.println("\n========== GET_DATA 策略3: 通配 ==========");
        BInterface2016GetDataResult r = service.execute(FSU_CODE,
                List.of("999999999999"), null);
        printResult(r);
        saveRaw("wildcard", r);
    }

    // ==================== helpers ====================

    private void printResult(BInterface2016GetDataResult r) {
        System.out.println("success: " + r.isSuccess());
        System.out.println("emptyData: " + r.isEmptyData());
        System.out.println("resultCode: " + r.getResultCode());
        System.out.println("resultDesc: " + r.getResultDesc());
        System.out.println("devices: " + r.getDevices().size());
        System.out.println("unmapped: " + r.getUnmapped().size());
        System.out.println("realDeviceAccessed: " + r.isRealDeviceAccessed());
        for (var u : r.getUnmapped()) {
            System.out.println("  UNMAPPED: deviceId=" + u.getDeviceId()
                    + " spid=" + u.getSpid() + " signalId=" + u.getSignalId()
                    + " value=" + u.getValue());
        }
    }

    private void saveRaw(String strategy, BInterface2016GetDataResult r) throws Exception {
        String prefix = "bif2016-p0-001-get-data-" + strategy;
        if (r.getRawRequest() != null) {
            Files.writeString(RAW_DIR.resolve(prefix + "-request-info.xml"),
                    r.getRawRequest());
            System.out.println("  saved: " + prefix + "-request-info.xml");
        }
        if (r.getRawResponse() != null) {
            Files.writeString(RAW_DIR.resolve(prefix + "-response.xml"),
                    r.getRawResponse());
            System.out.println("  saved: " + prefix + "-response.xml");
        }
    }

    // stubs — real run-once doesn't need DB
    static class StubFsuDeviceRepo implements FsuDeviceRepository {
        @Override public Optional<FsuDeviceEntity> findByFsuCode(String c) {
            FsuDeviceEntity d = new FsuDeviceEntity(); d.setId(1L); d.setFsuCode(c); d.setFsuName("FSU"); d.setStatus("ONLINE");
            d.setCreatedAt(LocalDateTime.now()); d.setUpdatedAt(LocalDateTime.now()); return Optional.of(d);
        }
        @Override public Optional<FsuDeviceEntity> findById(Long id) { return Optional.empty(); }
        @Override public List<FsuDeviceEntity> findBySiteId(Long id) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String s) { return List.of(); }
        @Override public void deleteAll() {} @Override public void flush() {}
        @Override public <S extends FsuDeviceEntity> S save(S e) { return e; }
        @Override public <S extends FsuDeviceEntity> S saveAndFlush(S e) { return e; }
        @Override public <S extends FsuDeviceEntity> List<S> saveAll(Iterable<S> es) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> List<S> saveAllAndFlush(Iterable<S> es) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<FsuDeviceEntity> es) {} @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {} @Override public void deleteAllInBatch() {}
        @Override public FsuDeviceEntity getOne(Long id) { return null; } @Override public FsuDeviceEntity getById(Long id) { return null; } @Override public FsuDeviceEntity getReferenceById(Long id) { return null; }
        @Override public <S extends FsuDeviceEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends FsuDeviceEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends FsuDeviceEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends FsuDeviceEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<FsuDeviceEntity> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<FsuDeviceEntity> findAll(org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public List<FsuDeviceEntity> findAll() { return List.of(); }
        @Override public List<FsuDeviceEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {} @Override public void delete(FsuDeviceEntity e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {} @Override public void deleteAll(Iterable<? extends FsuDeviceEntity> es) {}
        @Override public boolean existsById(Long id) { return false; }
    }
    static class StubPointRepo implements MonitoringPointRepository {
        @Override public Optional<MonitoringPointEntity> findByFsuIdAndPointCode(Long fsuId, String pc) { return Optional.empty(); }
        @Override public List<MonitoringPointEntity> findByFsuId(Long fsuId) { return List.of(); }
        @Override public List<MonitoringPointEntity> findByPointType(String t) { return List.of(); }
        @Override public void deleteAll() {} @Override public void flush() {}
        @Override public <S extends MonitoringPointEntity> S save(S e) { return e; } @Override public <S extends MonitoringPointEntity> S saveAndFlush(S e) { return e; }
        @Override public <S extends MonitoringPointEntity> List<S> saveAll(Iterable<S> es) { return List.of(); } @Override public <S extends MonitoringPointEntity> List<S> saveAllAndFlush(Iterable<S> es) { return List.of(); }
        @Override public Optional<MonitoringPointEntity> findById(Long id) { return Optional.empty(); } @Override public List<MonitoringPointEntity> findAll() { return List.of(); } @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {} @Override public void delete(MonitoringPointEntity e) {} @Override public void deleteAllById(Iterable<? extends Long> ids) {} @Override public void deleteAll(Iterable<? extends MonitoringPointEntity> es) {} @Override public void deleteAllInBatch(Iterable<MonitoringPointEntity> es) {} @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {} @Override public void deleteAllInBatch() {}
        @Override public MonitoringPointEntity getOne(Long id) { return null; } @Override public MonitoringPointEntity getById(Long id) { return null; } @Override public MonitoringPointEntity getReferenceById(Long id) { return null; }
        @Override public <S extends MonitoringPointEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends MonitoringPointEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends MonitoringPointEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends MonitoringPointEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends MonitoringPointEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends MonitoringPointEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends MonitoringPointEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<MonitoringPointEntity> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<MonitoringPointEntity> findAll(org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public List<MonitoringPointEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public boolean existsById(Long id) { return false; }
    }
    static class StubRtRepo implements RealtimeDataRepository {
        @Override public Optional<RealtimeDataEntity> findByPointId(Long id) { return Optional.empty(); } @Override public List<RealtimeDataEntity> findByFsuId(Long fsuId) { return List.of(); }
        @Override public <S extends RealtimeDataEntity> S save(S e) { return e; } @Override public <S extends RealtimeDataEntity> S saveAndFlush(S e) { return e; }
        @Override public <S extends RealtimeDataEntity> List<S> saveAll(Iterable<S> es) { return List.of(); } @Override public <S extends RealtimeDataEntity> List<S> saveAllAndFlush(Iterable<S> es) { return List.of(); }
        @Override public void deleteAll() {} @Override public void flush() {} @Override public void deleteAllInBatch(Iterable<RealtimeDataEntity> es) {} @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {} @Override public void deleteAllInBatch() {}
        @Override public RealtimeDataEntity getOne(Long id) { return null; } @Override public RealtimeDataEntity getById(Long id) { return null; } @Override public RealtimeDataEntity getReferenceById(Long id) { return null; }
        @Override public Optional<RealtimeDataEntity> findById(Long id) { return Optional.empty(); } @Override public List<RealtimeDataEntity> findAll() { return List.of(); } @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {} @Override public void delete(RealtimeDataEntity e) {} @Override public void deleteAllById(Iterable<? extends Long> ids) {} @Override public void deleteAll(Iterable<? extends RealtimeDataEntity> es) {}
        @Override public <S extends RealtimeDataEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends RealtimeDataEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends RealtimeDataEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends RealtimeDataEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends RealtimeDataEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends RealtimeDataEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends RealtimeDataEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<RealtimeDataEntity> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<RealtimeDataEntity> findAll(org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public List<RealtimeDataEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public boolean existsById(Long id) { return false; }
    }
}
