package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.BInterface2016GetDataResult;
import com.dcim.platform.module.binterface.service.BInterface2016GetDataService;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.mapping.service.UnmappedSignalObservationService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * BInterface2016GetDataService 单元测试 (BIF2016-P0-001).
 */
class BInterface2016GetDataServiceTest {

    private BInterface2016GetDataService service;
    private StubFsuServiceClient fsuClient;
    private StubFsuDeviceRepo fsuDeviceRepo;
    private StubMonitoringPointRepo pointRepo;
    private StubRealtimeDataRepo rtRepo;

    private static final String FSU_CODE = "51051243812345";
    private static final Long FSU_ID = 1L;

    @BeforeEach
    void setUp() {
        fsuClient = new StubFsuServiceClient();
        fsuDeviceRepo = new StubFsuDeviceRepo();
        pointRepo = new StubMonitoringPointRepo();
        rtRepo = new StubRealtimeDataRepo();
        service = new BInterface2016GetDataService(fsuClient,
                fsuCode -> FsuEndpointResult.fromExplicitServiceUrl(fsuCode, "http://192.168.100.100:8080/services/FSUService"),
                fsuDeviceRepo, pointRepo, rtRepo);

        FsuDeviceEntity dev = new FsuDeviceEntity();
        dev.setId(FSU_ID); dev.setFsuCode(FSU_CODE); dev.setFsuName("FSU"); dev.setStatus("ONLINE");
        dev.setCreatedAt(LocalDateTime.now()); dev.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(dev);
    }

    // ==================== 基本 Code 验证 ====================

    @Test
    void shouldUse2016Code401() {
        fsuClient.nextResponse = createAckWithDevices(List.of());
        service.execute(FSU_CODE, List.of("51051241820004"), null);
        assertEquals(BInterfacePkType.GET_DATA, fsuClient.lastReq.getPkType());
        assertEquals("legacy-2016", fsuClient.lastReq.getPkTypeFormat());
    }

    @Test
    void shouldNotUse2024Code501() {
        fsuClient.nextResponse = createAckWithDevices(List.of());
        service.execute(FSU_CODE, List.of("51051241820004"), null);
        assertEquals("legacy-2016", fsuClient.lastReq.getPkTypeFormat());
        assertTrue(fsuClient.lastReq.getInfoXml().contains("DeviceList"));
    }

    // ==================== 请求结构 ====================

    @Test
    void shouldConstructDeviceListInInfo() {
        fsuClient.nextResponse = createAckWithDevices(List.of());
        service.execute(FSU_CODE, List.of("51051241820004", "51051241830004"), null);
        String info = fsuClient.lastReq.getInfoXml();
        assertTrue(info.contains("DeviceList"));
        assertTrue(info.contains("51051241820004"));
        assertTrue(info.contains("51051241830004"));
    }

    @Test
    void shouldIncludeDeviceIdAndCodeAttributes() {
        fsuClient.nextResponse = createAckWithDevices(List.of());
        service.execute(FSU_CODE, List.of("51051241820004"), null);
        String info = fsuClient.lastReq.getInfoXml();
        assertTrue(info.contains("Id=") && info.contains("Code="));
    }

    @Test
    void shouldUseIdElementForSignalIdsInRequest() {
        Map<String, List<String>> semaphores = Map.of(
                "51051241820004", List.of("SP-001", "SP-002"));
        fsuClient.nextResponse = createAckWithDevices(List.of());
        service.execute(FSU_CODE, List.of("51051241820004"), semaphores);
        String info = fsuClient.lastReq.getInfoXml();
        assertTrue(info.contains("<Id>SP-001</Id>"));
        assertTrue(info.contains("<Id>SP-002</Id>"));
        assertFalse(info.contains("TSemaphore"), "GET_DATA request must not contain TSemaphore");
    }

    // ==================== XML escape ====================

    @Test
    void shouldEscapeFsuIdAndFsuCodeSpecialChars() {
        String specialFsu = "FS<U>&\"'001";
        // Register the special FSU code so the service doesn't reject it
        FsuDeviceEntity dev = new FsuDeviceEntity();
        dev.setId(99L); dev.setFsuCode(specialFsu); dev.setFsuName("Special");
        dev.setStatus("ONLINE");
        dev.setCreatedAt(LocalDateTime.now()); dev.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(dev);

        fsuClient.nextResponse = createAckWithDevices(List.of());
        service.execute(specialFsu, List.of("51051241820004"), null);
        String info = fsuClient.lastReq.getInfoXml();
        assertTrue(info.contains("&lt;"), "Should escape <");
        assertTrue(info.contains("&gt;"), "Should escape >");
        assertTrue(info.contains("&amp;"), "Should escape &");
        assertTrue(info.contains("&quot;"), "Should escape \"");
        assertTrue(info.contains("&apos;"), "Should escape '");
        assertFalse(info.contains("<FsuId>FS<U>"), "Should not contain raw <");
    }

    // ==================== blank signalId 过滤 ====================

    @Test
    void shouldFilterNullAndBlankSignalIds() {
        Map<String, List<String>> semaphores = Map.of(
                "51051241820004", Arrays.asList("SP-001", null, "", "   ", "SP-002"));
        fsuClient.nextResponse = createAckWithDevices(List.of());
        service.execute(FSU_CODE, List.of("51051241820004"), semaphores);
        String info = fsuClient.lastReq.getInfoXml();
        assertTrue(info.contains("<Id>SP-001</Id>"), "Valid signalId must be present");
        assertTrue(info.contains("<Id>SP-002</Id>"), "Valid signalId must be present");
        assertFalse(info.contains("<Id></Id>"), "Must not contain empty Id element");
        assertFalse(info.contains("<Id>   </Id>"), "Must not contain blank Id element");
    }

    // ==================== 空数据 ====================

    @Test
    void shouldReturnSuccessWhenAckResult1AndDeviceListEmpty() {
        fsuClient.nextResponse = createAckWithDevices(List.of());
        BInterface2016GetDataResult r = service.execute(FSU_CODE, List.of("51051241820004"), null);
        assertTrue(r.isSuccess());
        assertTrue(r.isEmptyData());
        assertEquals(0, r.getDevices().size());
    }

    // ==================== 解析测量值 ====================

    @Test
    void shouldParseTSemaphoreValues() {
        XmlDataModel xmlData = new XmlDataModel();
        Map<String, String> s1 = new LinkedHashMap<>();
        s1.put("DeviceID", "51051241820004");
        s1.put("Id", "SP-001"); s1.put("Code", "SP-001");
        s1.put("MeasuredVal", "25.5"); s1.put("Status", "NORMAL"); s1.put("Time", "2026-05-21T10:00:00");
        xmlData.addItem(s1);
        fsuClient.nextResponse = createAckWithXmlData(xmlData);

        BInterface2016GetDataResult r = service.execute(FSU_CODE, List.of("51051241820004"), null);
        assertTrue(r.isSuccess());
        assertFalse(r.isEmptyData());
        // SP-001 not in monitoring_point → goes to unmapped
        assertEquals(1, r.getUnmapped().size());
    }

    @Test
    void shouldNotRecordObservationWhenSignalIsMappedCandidateButPointNotBound() {
        EStoneIIMappingService mappingService = mock(EStoneIIMappingService.class);
        UnmappedSignalObservationService unmappedService = mock(UnmappedSignalObservationService.class);
        when(mappingService.resolveRealtime(eq(FSU_CODE), eq("51051240700002"), isNull(),
                eq("0407107001"), eq("0407107001"), eq("0.0000")))
                .thenReturn(standardCandidate("0407107001", "后半组电压", "", "0.0000"));
        BInterface2016GetDataService serviceWithMapping = new BInterface2016GetDataService(fsuClient,
                fsuCode -> FsuEndpointResult.fromExplicitServiceUrl(fsuCode, "http://192.168.100.100:8080/services/FSUService"),
                fsuDeviceRepo, pointRepo, rtRepo, mappingService, unmappedService);

        XmlDataModel xmlData = new XmlDataModel();
        Map<String, String> s1 = new LinkedHashMap<>();
        s1.put("DeviceID", "51051240700002");
        s1.put("Id", "0407107001");
        s1.put("Code", "0407107001");
        s1.put("MeasuredVal", "0.0000");
        xmlData.addItem(s1);
        fsuClient.nextResponse = createAckWithXmlData(xmlData);

        BInterface2016GetDataResult r = serviceWithMapping.execute(FSU_CODE, List.of("51051240700002"), null);

        assertTrue(r.isSuccess());
        assertEquals(1, r.getUnmapped().size());
        verify(unmappedService, never()).record(any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any());
    }

    // ==================== FSU 未注册 ====================

    @Test
    void shouldFailWhenFsuNotRegistered() {
        BInterface2016GetDataResult r = service.execute("UNKNOWN", List.of("123"), null);
        assertFalse(r.isSuccess());
    }

    // ==================== FSU 调用失败 ====================

    @Test
    void shouldFailWhenFsuCallFails() {
        fsuClient.nextResponse = FsuServiceResponse.fail("500", "timeout");
        BInterface2016GetDataResult r = service.execute(FSU_CODE, List.of("51051241820004"), null);
        assertFalse(r.isSuccess());
    }

    // ==================== raw 保存 ====================

    @Test
    void shouldReturnSuccessForEmptyAck() {
        fsuClient.nextResponse = createAckWithDevices(List.of());
        BInterface2016GetDataResult r = service.execute(FSU_CODE, List.of("51051241820004"), null);
        assertTrue(r.isSuccess());
        assertTrue(r.isEmptyData());
    }

    // ==================== Stubs ====================

    static class StubFsuServiceClient implements FsuServiceClient {
        FsuServiceResponse nextResponse;
        String nextResponseRawSoap;
        FsuServiceRequest lastReq;

        @Override
        public FsuServiceResponse call(FsuServiceRequest req) {
            this.lastReq = req;
            return nextResponse != null ? nextResponse : FsuServiceResponse.fail("500", "no response");
        }
    }

    static class StubFsuDeviceRepo implements FsuDeviceRepository {
        private final Map<Long, FsuDeviceEntity> store = new HashMap<>();
        long nextId = 1;
        @Override public <S extends FsuDeviceEntity> S save(S e) { if(e.getId()==null)e.setId(nextId++); store.put(e.getId(),e); return e; }
        @Override public Optional<FsuDeviceEntity> findByFsuCode(String c) { return store.values().stream().filter(e->c.equals(e.getFsuCode())).findFirst(); }
        @Override public Optional<FsuDeviceEntity> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<FsuDeviceEntity> findBySiteId(Long id) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String s) { return List.of(); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override @SuppressWarnings("unchecked") public <S extends FsuDeviceEntity> S saveAndFlush(S e) { return (S)save(e); }
        @Override @SuppressWarnings("unchecked") public <S extends FsuDeviceEntity> List<S> saveAll(Iterable<S> es) { List<S> r=new ArrayList<>(); for(S e:es)r.add((S)save(e)); return r; }
        @Override @SuppressWarnings("unchecked") public <S extends FsuDeviceEntity> List<S> saveAllAndFlush(Iterable<S> es) { List<S> r=new ArrayList<>(); for(S e:es)r.add((S)save(e)); return r; }
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
        @Override public void deleteAll(Iterable<? extends FsuDeviceEntity> es) { es.forEach(e->store.remove(e.getId())); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
    }

    static class StubMonitoringPointRepo implements MonitoringPointRepository {
        @Override public Optional<MonitoringPointEntity> findByFsuIdAndPointCode(Long fsuId, String pointCode) { return Optional.empty(); }
        @Override public List<MonitoringPointEntity> findByFsuId(Long fsuId) { return List.of(); }
        @Override public List<MonitoringPointEntity> findByPointType(String t) { return List.of(); }
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends MonitoringPointEntity> S save(S e) { return e; }
        @Override public <S extends MonitoringPointEntity> S saveAndFlush(S e) { return e; }
        @Override public <S extends MonitoringPointEntity> List<S> saveAll(Iterable<S> es) { return List.of(); }
        @Override public <S extends MonitoringPointEntity> List<S> saveAllAndFlush(Iterable<S> es) { return List.of(); }
        @Override public Optional<MonitoringPointEntity> findById(Long id) { return Optional.empty(); }
        @Override public List<MonitoringPointEntity> findAll() { return List.of(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(MonitoringPointEntity e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends MonitoringPointEntity> es) {}
        @Override public void deleteAllInBatch(Iterable<MonitoringPointEntity> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public MonitoringPointEntity getOne(Long id) { return null; }
        @Override public MonitoringPointEntity getById(Long id) { return null; }
        @Override public MonitoringPointEntity getReferenceById(Long id) { return null; }
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

    static class StubRealtimeDataRepo implements RealtimeDataRepository {
        final List<RealtimeDataEntity> saved = new ArrayList<>();
        @Override public Optional<RealtimeDataEntity> findByPointId(Long pointId) { return Optional.empty(); }
        @Override public List<RealtimeDataEntity> findByFsuId(Long fsuId) { return saved; }
        @Override public <S extends RealtimeDataEntity> S save(S e) { saved.add(e); return e; }
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override @SuppressWarnings("unchecked") public <S extends RealtimeDataEntity> S saveAndFlush(S e) { saved.add(e); return e; }
        @Override @SuppressWarnings("unchecked") public <S extends RealtimeDataEntity> List<S> saveAll(Iterable<S> es) { List<S> r=new ArrayList<>(); for(S e:es){saved.add(e);r.add(e);} return r; }
        @Override @SuppressWarnings("unchecked") public <S extends RealtimeDataEntity> List<S> saveAllAndFlush(Iterable<S> es) { List<S> r=new ArrayList<>(); for(S e:es){saved.add(e);r.add(e);} return r; }
        @Override public void deleteAllInBatch(Iterable<RealtimeDataEntity> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public RealtimeDataEntity getOne(Long id) { return null; }
        @Override public RealtimeDataEntity getById(Long id) { return null; }
        @Override public RealtimeDataEntity getReferenceById(Long id) { return null; }
        @Override public Optional<RealtimeDataEntity> findById(Long id) { return Optional.empty(); }
        @Override public List<RealtimeDataEntity> findAll() { return saved; }
        @Override public long count() { return saved.size(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(RealtimeDataEntity e) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends RealtimeDataEntity> es) {}
        @Override public <S extends RealtimeDataEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends RealtimeDataEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends RealtimeDataEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends RealtimeDataEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends RealtimeDataEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends RealtimeDataEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends RealtimeDataEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<RealtimeDataEntity> findAll(org.springframework.data.domain.Pageable p) { return new org.springframework.data.domain.PageImpl<>(saved, p, saved.size()); }
        @Override public List<RealtimeDataEntity> findAll(org.springframework.data.domain.Sort s) { return saved; }
        @Override public List<RealtimeDataEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public boolean existsById(Long id) { return false; }
    }

    // ==================== helpers ====================

    private FsuServiceResponse createAckWithDevices(List<Map<String,String>> semaphores) {
        XmlDataModel xmlData = new XmlDataModel();
        for (var s : semaphores) xmlData.addItem(s);
        return createAckWithXmlData(xmlData);
    }

    private FsuServiceResponse createAckWithXmlData(XmlDataModel xmlData) {
        return createResponse(true, "0", "OK", xmlData);
    }

    private FsuServiceResponse createResponse(boolean success, String code, String desc, XmlDataModel xmlData) {
        String rawSoap = "<soap:Envelope><soap:Body><Response>"
                + "<PK_Type><Name>GET_DATA_ACK</Name><Code>402</Code></PK_Type>"
                + "<Info><FsuId>" + FSU_CODE + "</FsuId><FsuCode>" + FSU_CODE
                + "</FsuCode><Result>1</Result><Values/></Info>"
                + "</Response></soap:Body></soap:Envelope>";
        return success
                ? FsuServiceResponse.successReal(rawSoap, null, null, xmlData)
                : FsuServiceResponse.fail(code, desc);
    }

    private static EStoneIIMappingResult standardCandidate(String signalId, String signalName,
                                                           String unit, String value) {
        return new EStoneIIMappingResult(FSU_CODE, "51051240700002", null, null,
                signalId, signalId, signalName, unit, value, null,
                "AI/模拟量", "AI", null, null, null, null,
                "MAPPED_CANDIDATE", "LOW", "BINTERFACE_2016_STANDARD",
                true, false, false, "BINTERFACE_2016_STANDARD", null);
    }
}
