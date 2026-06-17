package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.service.BInterface2016GetFsuInfoResult;
import com.dcim.platform.module.binterface.service.BInterface2016GetFsuInfoService;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BInterface2016GetFsuInfoService 单元测试。
 * 使用内存 stub 替代所有外部依赖。
 */
class BInterface2016GetFsuInfoServiceTest {

    private BInterface2016GetFsuInfoService service;
    private StubFsuServiceClient fsuServiceClient;
    private StubFsuStatusRepository fsuStatusRepo;
    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubFsuEndpointResolver endpointResolver;

    private static final String FSU_CODE = "51051243812345";
    private static final Long FSU_ID = 1L;

    @BeforeEach
    void setUp() {
        fsuServiceClient = new StubFsuServiceClient();
        fsuStatusRepo = new StubFsuStatusRepository();
        fsuDeviceRepo = new StubFsuDeviceRepository();
        endpointResolver = new StubFsuEndpointResolver();

        service = new BInterface2016GetFsuInfoService(
                fsuServiceClient, endpointResolver, fsuStatusRepo, fsuDeviceRepo, new com.dcim.platform.module.binterface.xml.XmlDataParser());

        // 预注册 FSU 设备
        FsuDeviceEntity device = new FsuDeviceEntity();
        device.setId(FSU_ID);
        device.setFsuCode(FSU_CODE);
        device.setFsuName("真实FSU");
        device.setStatus("ONLINE");
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(device);
    }

    // ==================== 成功场景 ====================

    @Test
    void shouldExecuteGetFsuinfoWith2016Code() {
        // 模拟 FSU 返回 GET_FSUINFO_ACK (Code=1702)
        fsuServiceClient.setNextResponse(createSuccessResponse(
                createXmlData("14.95", "62.84")));

        BInterface2016GetFsuInfoResult result = service.execute(FSU_CODE, null);

        assertTrue(result.isSuccess());
        assertEquals(FSU_CODE, result.getFsuCode());
        assertEquals(new BigDecimal("14.95"), result.getCpuUsage());
        assertEquals(new BigDecimal("62.84"), result.getMemUsage());
        assertTrue(result.isStatusUpdated());
    }

    @Test
    void shouldUpdateLastHeartbeatOnSuccess() {
        fsuServiceClient.setNextResponse(createSuccessResponse(
                createXmlData("14.95", "62.84")));

        service.execute(FSU_CODE, null);

        Optional<BInterfaceFsuStatusEntity> status = fsuStatusRepo.findByFsuCode(FSU_CODE);
        assertTrue(status.isPresent());
        assertEquals("ONLINE", status.get().getOnlineStatus());
        assertNotNull(status.get().getLastHeartbeat());
        // statusDetail 应包含 CPU/MEM
        assertNotNull(status.get().getStatusDetail());
        assertTrue(status.get().getStatusDetail().contains("CPU"));
    }

    @Test
    void shouldUse2016PkTypeAndLegacyFormat() {
        fsuServiceClient.setNextResponse(createSuccessResponse(
                createXmlData("10.0", "50.0")));

        service.execute(FSU_CODE, null);

        // 验证请求使用了正确的 PK_Type 和格式
        FsuServiceRequest req = fsuServiceClient.getLastRequest();
        assertEquals(BInterfacePkType.GET_FSUINFO, req.getPkType());
        assertEquals("legacy-2016", req.getPkTypeFormat());
        // 对齐 LANDING-006 成功格式: 仅 FSUCode（大写）
        assertTrue(req.getInfoXml().contains("<FSUCode>") && req.getInfoXml().contains(FSU_CODE),
                "Info XML 应使用 LANDING-006 验证格式: <FSUCode>" + FSU_CODE + "</FSUCode>");
    }

    // ==================== Code 不是 1702 不更新 ====================

    @Test
    void shouldNotUpdateWhenAckCodeIsNot1702() {
        // rawSoap 包含非 1702 的 ACK Code
        String rawSoap = "<soap:Envelope><soap:Body><Response>"
                + "<PK_Type><Name>GET_FSUINFO_ACK</Name><Code>1002</Code></PK_Type>"
                + "</Response></soap:Body></soap:Envelope>";
        fsuServiceClient.setNextResponse(FsuServiceResponse.success(
                rawSoap, null, null, createXmlData("14.95", "62.84")));

        BInterface2016GetFsuInfoResult result = service.execute(FSU_CODE, null);

        assertFalse(result.isSuccess());
        assertFalse(result.isStatusUpdated());
    }

    // ==================== Result 非成功不更新 ====================

    @Test
    void shouldNotUpdateWhenResultIsNotZero() {
        fsuServiceClient.setNextResponse(createFailResponse("1", "FSU 处理失败"));

        BInterface2016GetFsuInfoResult result = service.execute(FSU_CODE, null);

        assertFalse(result.isSuccess());
        assertFalse(result.isStatusUpdated());
    }

    // ==================== XML 解析失败不更新 ====================

    @Test
    void shouldNotUpdateWhenXmlDataIsEmpty() {
        fsuServiceClient.setNextResponse(createSuccessResponse(null));

        BInterface2016GetFsuInfoResult result = service.execute(FSU_CODE, null);

        assertFalse(result.isSuccess());
        assertFalse(result.isStatusUpdated());
    }

    @Test
    void shouldNotUpdateWhenCpuMemMissing() {
        // xmlData 不含 CPUUsage/MEMUsage
        fsuServiceClient.setNextResponse(createSuccessResponse(new XmlDataModel()));

        BInterface2016GetFsuInfoResult result = service.execute(FSU_CODE, null);

        assertFalse(result.isSuccess());
        assertFalse(result.isStatusUpdated());
    }

    // ==================== FSU 调用异常不更新 ====================

    @Test
    void shouldNotUpdateWhenFsuCallFails() {
        fsuServiceClient.setNextResponse(FsuServiceResponse.fail("500", "连接超时"));

        BInterface2016GetFsuInfoResult result = service.execute(FSU_CODE, null);

        assertFalse(result.isSuccess());
        assertFalse(result.isStatusUpdated());
    }

    // ==================== FSU 未注册 ====================

    @Test
    void shouldFailWhenFsuNotRegistered() {
        BInterface2016GetFsuInfoResult result = service.execute("UNKNOWN-FSU", null);

        assertFalse(result.isSuccess());
        assertFalse(result.isStatusUpdated());
    }

    // ==================== 已有状态更新 ====================

    @Test
    void shouldUpdateExistingStatus() {
        // 预创建已有 FSU 状态记录
        BInterfaceFsuStatusEntity existing = new BInterfaceFsuStatusEntity();
        existing.setId(1L);
        existing.setFsuId(FSU_ID);
        existing.setFsuCode(FSU_CODE);
        existing.setLoginStatus("LOGIN");
        existing.setOnlineStatus("OFFLINE");
        existing.setLastHeartbeat(LocalDateTime.now().minusHours(1));
        existing.setCreatedAt(LocalDateTime.now().minusDays(1));
        existing.setUpdatedAt(LocalDateTime.now().minusHours(1));
        fsuStatusRepo.save(existing);

        fsuServiceClient.setNextResponse(createSuccessResponse(
                createXmlData("20.0", "70.0")));

        service.execute(FSU_CODE, null);

        BInterfaceFsuStatusEntity updated = fsuStatusRepo.findByFsuCode(FSU_CODE).orElseThrow();
        assertEquals("ONLINE", updated.getOnlineStatus());
        assertNotNull(updated.getLastHeartbeat());
        // last_heartbeat 应更新（不小于原有值）
        assertFalse(updated.getLastHeartbeat().isBefore(existing.getLastHeartbeat()),
                "last_heartbeat 应更新为当前时间");
        assertTrue(updated.getStatusDetail().contains("20.0"));
    }

    // ==================== 2024 代码不受影响 ====================

    @Test
    void shouldUse2016CodeNot2024() {
        fsuServiceClient.setNextResponse(createSuccessResponse(
                createXmlData("5.0", "30.0")));

        service.execute(FSU_CODE, null);

        // 确认使用的是 2016 GET_FSUINFO 而非 2024 GET_SUINFO
        assertEquals(BInterfacePkType.GET_FSUINFO, fsuServiceClient.getLastRequest().getPkType());
        assertNotEquals(BInterfacePkType.GET_SUINFO, fsuServiceClient.getLastRequest().getPkType());
    }

    // ==================== Stubs ====================

    static class StubFsuServiceClient implements FsuServiceClient {
        private FsuServiceResponse nextResponse;
        private FsuServiceRequest lastRequest;

        void setNextResponse(FsuServiceResponse r) { this.nextResponse = r; }
        FsuServiceRequest getLastRequest() { return lastRequest; }

        @Override
        public FsuServiceResponse call(FsuServiceRequest request) {
            this.lastRequest = request;
            return nextResponse;
        }
    }

    static class StubFsuStatusRepository implements BInterfaceFsuStatusRepository {
        private final Map<Long, BInterfaceFsuStatusEntity> store = new HashMap<>();
        private long nextId = 1;

        public BInterfaceFsuStatusEntity save(BInterfaceFsuStatusEntity e) {
            if (e.getId() == null) e.setId(nextId++);
            store.put(e.getId(), e);
            return e;
        }

        @Override
        public Optional<BInterfaceFsuStatusEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> fsuCode.equals(e.getFsuCode())).findFirst();
        }

        @Override public Optional<BInterfaceFsuStatusEntity> findByFsuId(Long fsuId) { return Optional.empty(); }
        @Override public List<BInterfaceFsuStatusEntity> findByOnlineStatus(String s) { return List.of(); }
        @Override public Optional<BInterfaceFsuStatusEntity> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<BInterfaceFsuStatusEntity> findAll() { return new ArrayList<>(store.values()); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override @SuppressWarnings("unchecked")
        public <S extends BInterfaceFsuStatusEntity> S saveAndFlush(S e) { return (S) save(e); }
        @Override @SuppressWarnings("unchecked")
        public <S extends BInterfaceFsuStatusEntity> List<S> saveAll(Iterable<S> es) { List<S> r = new ArrayList<>(); for (S e : es) r.add((S) save(e)); return r; }
        @Override @SuppressWarnings("unchecked")
        public <S extends BInterfaceFsuStatusEntity> List<S> saveAllAndFlush(Iterable<S> es) { List<S> r = new ArrayList<>(); for (S e : es) r.add((S) save(e)); return r; }
        @Override public void deleteAllInBatch(Iterable<BInterfaceFsuStatusEntity> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public BInterfaceFsuStatusEntity getOne(Long id) { return store.get(id); }
        @Override public BInterfaceFsuStatusEntity getById(Long id) { return store.get(id); }
        @Override public BInterfaceFsuStatusEntity getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends BInterfaceFsuStatusEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends BInterfaceFsuStatusEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends BInterfaceFsuStatusEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends BInterfaceFsuStatusEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends BInterfaceFsuStatusEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends BInterfaceFsuStatusEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends BInterfaceFsuStatusEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<BInterfaceFsuStatusEntity> findAll(org.springframework.data.domain.Pageable p) { return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(store.values()), p, store.size()); }
        @Override public List<BInterfaceFsuStatusEntity> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
        @Override public List<BInterfaceFsuStatusEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(BInterfaceFsuStatusEntity e) { store.remove(e.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends BInterfaceFsuStatusEntity> es) { es.forEach(e -> store.remove(e.getId())); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
    }

    static class StubFsuDeviceRepository implements FsuDeviceRepository {
        private final Map<Long, FsuDeviceEntity> store = new HashMap<>();
        private long nextId = 1;

        public FsuDeviceEntity save(FsuDeviceEntity e) {
            if (e.getId() == null) e.setId(nextId++);
            store.put(e.getId(), e); return e;
        }
        @Override public Optional<FsuDeviceEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> fsuCode.equals(e.getFsuCode())).findFirst();
        }
        @Override public Optional<FsuDeviceEntity> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<FsuDeviceEntity> findBySiteId(Long id) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String s) { return List.of(); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override @SuppressWarnings("unchecked")
        public <S extends FsuDeviceEntity> S saveAndFlush(S e) { return (S) save(e); }
        @Override @SuppressWarnings("unchecked")
        public <S extends FsuDeviceEntity> List<S> saveAll(Iterable<S> es) { List<S> r = new ArrayList<>(); for (S e : es) r.add((S) save(e)); return r; }
        @Override @SuppressWarnings("unchecked")
        public <S extends FsuDeviceEntity> List<S> saveAllAndFlush(Iterable<S> es) { List<S> r = new ArrayList<>(); for (S e : es) r.add((S) save(e)); return r; }
        @Override public void deleteAllInBatch(Iterable<FsuDeviceEntity> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public FsuDeviceEntity getOne(Long id) { return store.get(id); }
        @Override public FsuDeviceEntity getById(Long id) { return store.get(id); }
        @Override public FsuDeviceEntity getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends FsuDeviceEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends FsuDeviceEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends FsuDeviceEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends FsuDeviceEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends FsuDeviceEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<FsuDeviceEntity> findAll(org.springframework.data.domain.Pageable p) { return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(store.values()), p, store.size()); }
        @Override public List<FsuDeviceEntity> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
        @Override public List<FsuDeviceEntity> findAll() { return new ArrayList<>(store.values()); }
        @Override public List<FsuDeviceEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(FsuDeviceEntity e) { store.remove(e.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends FsuDeviceEntity> es) { es.forEach(e -> store.remove(e.getId())); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
    }

    static class StubFsuEndpointResolver implements FsuEndpointResolver {
        @Override
        public FsuEndpointResult resolve(String fsuCode) {
            return FsuEndpointResult.fromExplicitServiceUrl(fsuCode,
                    "http://192.168.100.100:8080/services/FSUService");
        }
    }

    // ==================== 辅助方法 ====================

    private FsuServiceResponse createSuccessResponse(XmlDataModel xmlData) {
        return FsuServiceResponse.success(null, null, null, xmlData);
    }

    private FsuServiceResponse createFailResponse(String code, String desc) {
        return FsuServiceResponse.fail(code, desc);
    }

    private XmlDataModel createXmlData(String cpu, String mem) {
        XmlDataModel model = new XmlDataModel();
        Map<String, String> item = new LinkedHashMap<>();
        item.put("CPUUsage", cpu);
        item.put("MEMUsage", mem);
        model.addItem(item);
        return model;
    }
}
