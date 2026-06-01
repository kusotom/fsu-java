package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.service.BInterface2016GetFsuInfoResult;
import com.dcim.platform.module.binterface.service.BInterface2016GetFsuInfoService;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * LANDING-015: 真实 FSU GET_FSUINFO 心跳轮询 run-once 验证。
 *
 * <p>手动执行:
 * <pre>mvn test -Dtest='Landing015RealFsuIntegrationTest' -DrealFsuTest.enabled=true</pre>
 *
 * <p>不依赖 Spring 容器（手动 wiring），直接向真实 FSU 发送 SOAP 请求。
 * 默认 mvn test 不执行（@EnabledIfSystemProperty + assumeTrue 双保险）。
 */
@Tag("real-fsu")
@EnabledIfSystemProperty(named = "realFsuTest.enabled", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Landing015RealFsuIntegrationTest {

    private static final String FSU_CODE = "51051243812345";
    private static final String SERVICE_URL = "http://192.168.100.100:8080/services/FSUService";

    private static final SoapMessageHandler soapHandler = new SoapMessageHandler();
    private static final XmlDataParser xmlDataParser = new XmlDataParser();
    private static final FsuServiceRpcAdapter rpcAdapter = new FsuServiceRpcAdapter();
    private static final RealHttpFsuServiceClient client =
            new RealHttpFsuServiceClient(soapHandler, xmlDataParser, rpcAdapter, 5000, 10000);

    private BInterface2016GetFsuInfoService getFsuInfoService;

    @BeforeEach
    void setUp() {
        FsuEndpointResolver resolver = fsuCode ->
                FsuEndpointResult.fromExplicitServiceUrl(fsuCode, SERVICE_URL);

        // 使用简化的 stub repository（真实环境仍需 Spring Data）
        // 本次 run-once 重点验证 FSUService 调用与解析，状态持久化验证需连接数据库
        BInterfaceFsuStatusRepository fsuStatusRepo = new StubFsuStatusRepository();
        FsuDeviceRepository fsuDeviceRepo = new StubFsuDeviceRepository();

        getFsuInfoService = new BInterface2016GetFsuInfoService(
                client, resolver, fsuStatusRepo, fsuDeviceRepo, xmlDataParser);
    }

    // ==================== GET_FSUINFO (Code=1701) ====================

    @Test
    @Order(1)
    void shouldExecuteGetFsuinfoWithRealFsu() {
        assumeTrue(Boolean.getBoolean("realFsuTest.enabled"),
                "真实 FSU 测试需显式启用: -DrealFsuTest.enabled=true");

        System.out.println("\n========== GET_FSUINFO (2016 Code=1701) ==========");

        BInterface2016GetFsuInfoResult result = getFsuInfoService.execute(FSU_CODE, SERVICE_URL);

        System.out.println("success: " + result.isSuccess());
        System.out.println("resultCode: " + result.getResultCode());
        System.out.println("resultDesc: " + result.getResultDesc());
        System.out.println("fsuCode: " + result.getFsuCode());
        System.out.println("cpuUsage: " + result.getCpuUsage());
        System.out.println("memUsage: " + result.getMemUsage());
        System.out.println("realDeviceAccessed: " + result.isRealDeviceAccessed());
        System.out.println("statusUpdated: " + result.isStatusUpdated());

        // 核心断言
        assertTrue(result.isSuccess(),
                "GET_FSUINFO 应成功: " + result.getResultDesc());
        assertTrue(result.isRealDeviceAccessed(),
                "应标记为真实设备访问");

        // CPU/MEM 至少一项非空
        assertTrue(result.getCpuUsage() != null || result.getMemUsage() != null,
                "CPUUsage 或 MEMUsage 至少一项应非空");

        System.out.println("========== GET_FSUINFO PASSED ==========");
    }

    // ==================== Stubs（不连数据库时使用） ====================

    static class StubFsuStatusRepository implements BInterfaceFsuStatusRepository {
        private BInterfaceFsuStatusEntity lastSaved;

        @Override
        public Optional<BInterfaceFsuStatusEntity> findByFsuCode(String fsuCode) {
            // 模拟未找到已有记录（首次心跳）
            return Optional.empty();
        }

        @Override
        public BInterfaceFsuStatusEntity save(BInterfaceFsuStatusEntity entity) {
            if (entity.getId() == null) entity.setId(1L);
            this.lastSaved = entity;
            return entity;
        }

        @Override public Optional<BInterfaceFsuStatusEntity> findByFsuId(Long id) { return Optional.empty(); }
        @Override public List<BInterfaceFsuStatusEntity> findByOnlineStatus(String s) { return List.of(); }
        @Override public Optional<BInterfaceFsuStatusEntity> findById(Long id) { return Optional.ofNullable(lastSaved); }
        @Override public List<BInterfaceFsuStatusEntity> findAll() { return lastSaved != null ? List.of(lastSaved) : List.of(); }
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override @SuppressWarnings("unchecked")
        public <S extends BInterfaceFsuStatusEntity> S saveAndFlush(S e) { return (S) save(e); }
        @Override public <S extends BInterfaceFsuStatusEntity> List<S> saveAll(Iterable<S> es) { return List.of(); }
        @Override public <S extends BInterfaceFsuStatusEntity> List<S> saveAllAndFlush(Iterable<S> es) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<BInterfaceFsuStatusEntity> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public BInterfaceFsuStatusEntity getOne(Long id) { return null; }
        @Override public BInterfaceFsuStatusEntity getById(Long id) { return null; }
        @Override public BInterfaceFsuStatusEntity getReferenceById(Long id) { return null; }
        @Override public <S extends BInterfaceFsuStatusEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends BInterfaceFsuStatusEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends BInterfaceFsuStatusEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends BInterfaceFsuStatusEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends BInterfaceFsuStatusEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends BInterfaceFsuStatusEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends BInterfaceFsuStatusEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<BInterfaceFsuStatusEntity> findAll(org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public List<BInterfaceFsuStatusEntity> findAll(org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public List<BInterfaceFsuStatusEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(BInterfaceFsuStatusEntity e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends BInterfaceFsuStatusEntity> es) {}
        @Override public boolean existsById(Long id) { return false; }
    }

    static class StubFsuDeviceRepository implements FsuDeviceRepository {
        @Override
        public Optional<FsuDeviceEntity> findByFsuCode(String fsuCode) {
            // 返回模拟设备（允许 GET_FSUINFO 查找 fsuId）
            FsuDeviceEntity device = new FsuDeviceEntity();
            device.setId(1L);
            device.setFsuCode(fsuCode);
            device.setFsuName("真实FSU");
            device.setStatus("ONLINE");
            device.setCreatedAt(LocalDateTime.now());
            device.setUpdatedAt(LocalDateTime.now());
            return Optional.of(device);
        }
        @Override public Optional<FsuDeviceEntity> findById(Long id) { return Optional.empty(); }
        @Override public List<FsuDeviceEntity> findBySiteId(Long id) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String s) { return List.of(); }
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends FsuDeviceEntity> S save(S e) { return e; }
        @Override public <S extends FsuDeviceEntity> S saveAndFlush(S e) { return e; }
        @Override public <S extends FsuDeviceEntity> List<S> saveAll(Iterable<S> es) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> List<S> saveAllAndFlush(Iterable<S> es) { return List.of(); }
        @Override public void deleteAllInBatch(Iterable<FsuDeviceEntity> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public FsuDeviceEntity getOne(Long id) { return null; }
        @Override public FsuDeviceEntity getById(Long id) { return null; }
        @Override public FsuDeviceEntity getReferenceById(Long id) { return null; }
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
        @Override public void deleteById(Long id) {}
        @Override public void delete(FsuDeviceEntity e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends FsuDeviceEntity> es) {}
        @Override public boolean existsById(Long id) { return false; }
    }
}
