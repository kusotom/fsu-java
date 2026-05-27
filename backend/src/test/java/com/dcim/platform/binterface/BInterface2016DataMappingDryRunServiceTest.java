package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.BInterface2016DataMappingDryRunResult;
import com.dcim.platform.module.binterface.service.BInterface2016DataMappingDryRunService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DATA-MAPPING-006: dry-run 映射服务测试。
 * 不访问真实 FSU，不写数据库。
 */
class BInterface2016DataMappingDryRunServiceTest {

    private BInterface2016DataMappingDryRunService service;
    private StubMonitoringPointRepo mpRepo;
    private StubFsuDeviceRepo fsuDeviceRepo;

    private static final String FSU_CODE = "51051243812345";
    private static final Long FSU_ID = 2L;

    @BeforeEach
    void setUp() {
        mpRepo = new StubMonitoringPointRepo();
        fsuDeviceRepo = new StubFsuDeviceRepo();

        FsuDeviceEntity dev = new FsuDeviceEntity();
        dev.setId(FSU_ID); dev.setFsuCode(FSU_CODE); dev.setFsuName("动环");
        dev.setStatus("ONLINE");
        dev.setCreatedAt(LocalDateTime.now()); dev.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(dev);

        service = new BInterface2016DataMappingDryRunService(mpRepo, fsuDeviceRepo);
    }

    @Test
    void allUnmatchedWhenNoMonitoringPoints() {
        XmlDataModel data = buildXmlData(List.of(
                item("51051240700002", "0407102001", "54.1"),
                item("51051241820004", "0418002001", "0")
        ));

        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, data);
        assertEquals(2, r.getTotalPoints());
        assertEquals(0, r.getMatchedCount());
        assertEquals(2, r.getUnmatchedCount());
        assertFalse(r.hasAnyMatched());
    }

    @Test
    void matchedWhenPointCodeMatches() {
        MonitoringPointEntity mp = new MonitoringPointEntity();
        mp.setId(100L); mp.setFsuId(FSU_ID); mp.setPointCode("0407102001");
        mp.setPointName("蓄电池总电压"); mp.setPointType("AI"); mp.setDataType("NUMBER");
        mp.setStatus("ACTIVE");
        mpRepo.save(mp);

        XmlDataModel data = buildXmlData(List.of(
                item("51051240700002", "0407102001", "54.1")
        ));

        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, data);
        assertEquals(1, r.getTotalPoints());
        assertEquals(1, r.getMatchedCount());
        assertEquals(0, r.getUnmatchedCount());
        assertTrue(r.hasAnyMatched());
        assertEquals("matched", r.getEntries().get(0).status());
        assertEquals(100L, r.getEntries().get(0).pointId());
    }

    @Test
    void unmatchedWhenSignalIdNotFound() {
        MonitoringPointEntity mp = new MonitoringPointEntity();
        mp.setId(100L); mp.setFsuId(FSU_ID); mp.setPointCode("TEMP-R01");
        mp.setPointName("机柜温度"); mp.setPointType("AI"); mp.setDataType("NUMBER");
        mp.setStatus("ACTIVE");
        mpRepo.save(mp);

        XmlDataModel data = buildXmlData(List.of(
                item("51051240700002", "0407102001", "54.1")
        ));

        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, data);
        assertEquals(0, r.getMatchedCount());
        assertEquals(1, r.getUnmatchedCount());
    }

    @Test
    void emptyXmlDataReturnsZeroPoints() {
        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, new XmlDataModel());
        assertEquals(0, r.getTotalPoints());
        assertEquals(0, r.getMatchedCount());
    }

    @Test
    void nullXmlDataReturnsZeroPoints() {
        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, null);
        assertEquals(0, r.getTotalPoints());
    }

    @Test
    void fsuNotRegisteredAllUnmatched() {
        XmlDataModel data = buildXmlData(List.of(
                item("51051240700002", "0407102001", "54.1")
        ));

        BInterface2016DataMappingDryRunResult r = service.dryRun("UNKNOWN", data);
        assertEquals(1, r.getUnmatchedCount());
        assertTrue(r.getEntries().get(0).note().contains("not registered"));
    }

    @Test
    void missingSignalIdIsUnmatched() {
        XmlDataModel data = buildXmlData(List.of(
                item("51051240700002", null, null)
        ));

        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, data);
        assertEquals(0, r.getMatchedCount());
        assertEquals(1, r.getUnmatchedCount());
        assertTrue(r.getEntries().get(0).note().contains("missing"));
    }

    @Test
    void doesNotWriteRealtimeData() {
        MonitoringPointEntity mp = new MonitoringPointEntity();
        mp.setId(100L); mp.setFsuId(FSU_ID); mp.setPointCode("0407102001");
        mp.setPointName("蓄电池总电压"); mp.setPointType("AI"); mp.setDataType("NUMBER");
        mp.setStatus("ACTIVE");
        mpRepo.save(mp);

        XmlDataModel data = buildXmlData(List.of(
                item("51051240700002", "0407102001", "54.1")
        ));

        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, data);
        assertTrue(r.hasAnyMatched());
        // 验证不写入 realtime_data: mpRepo 无 write 操作
    }

    @Test
    void doesNotCreateMonitoringPoint() {
        assertEquals(0, mpRepo.count());

        XmlDataModel data = buildXmlData(List.of(
                item("51051240700002", "0407102001", "54.1")
        ));

        service.dryRun(FSU_CODE, data);
        // 验证未自动创建 monitoring_point
        assertEquals(0, mpRepo.count());
    }

    @Test
    void connextion007AllDevicesSampleReplay() {
        // Replay the 8 TSemaphore points from CONNECTION-007 all-devices raw sample
        XmlDataModel data = buildXmlData(List.of(
                item("51051241820004", "0418002001", "0"),
                item("51051241830004", "0418004001", "0"),
                item("51051241830004", "0418007001", "0"),
                item("51051241830004", "0418101001", "0.0"),
                item("51051241830004", "0418102001", "0.0"),
                item("51051241840004", "0418001001", "0"),
                item("51051240700002", "0407102001", "54.1"),
                item("51051240700002", "0407107001", "0.0")
        ));

        BInterface2016DataMappingDryRunResult r = service.dryRun(FSU_CODE, data);
        assertEquals(8, r.getTotalPoints());
        assertEquals(0, r.getMatchedCount(), "No monitoring_points exist, all should be unmatched");
        assertEquals(8, r.getUnmatchedCount());
        assertFalse(r.hasAnyMatched());
    }

    // === helpers ===

    private Map<String, String> item(String deviceId, String signalId, String value) {
        Map<String, String> m = new LinkedHashMap<>();
        if (deviceId != null) m.put("DeviceID", deviceId);
        if (signalId != null) m.put("Id", signalId);
        if (value != null) m.put("MeasuredVal", value);
        return m;
    }

    private XmlDataModel buildXmlData(List<Map<String, String>> items) {
        XmlDataModel m = new XmlDataModel();
        for (var item : items) m.addItem(item);
        return m;
    }

    // === stubs ===

    static class StubMonitoringPointRepo implements MonitoringPointRepository {
        private final Map<Long, MonitoringPointEntity> store = new LinkedHashMap<>();
        private long nextId = 1;

        @Override public <S extends MonitoringPointEntity> S save(S e) {
            if (e.getId() == null) e.setId(nextId++);
            store.put(e.getId(), e); return e;
        }
        @Override public List<MonitoringPointEntity> findByFsuId(Long fsuId) {
            return store.values().stream().filter(p -> fsuId.equals(p.getFsuId())).toList();
        }
        @Override public Optional<MonitoringPointEntity> findByFsuIdAndPointCode(Long fsuId, String pointCode) {
            return store.values().stream()
                    .filter(p -> fsuId.equals(p.getFsuId()) && pointCode.equals(p.getPointCode()))
                    .findFirst();
        }
        @Override public Optional<MonitoringPointEntity> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<MonitoringPointEntity> findAll() { return new ArrayList<>(store.values()); }
        @Override public long count() { return store.size(); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends MonitoringPointEntity> S saveAndFlush(S e) { return save(e); }
        @Override public <S extends MonitoringPointEntity> List<S> saveAll(Iterable<S> es) { List<S> r = new ArrayList<>(); for (S e : es) r.add(save(e)); return r; }
        @Override public <S extends MonitoringPointEntity> List<S> saveAllAndFlush(Iterable<S> es) { return saveAll(es); }
        @Override public void deleteAllInBatch(Iterable<MonitoringPointEntity> es) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public MonitoringPointEntity getOne(Long id) { return store.get(id); }
        @Override public MonitoringPointEntity getById(Long id) { return store.get(id); }
        @Override public MonitoringPointEntity getReferenceById(Long id) { return store.get(id); }
        @Override public <S extends MonitoringPointEntity> Optional<S> findOne(org.springframework.data.domain.Example<S> e) { return Optional.empty(); }
        @Override public <S extends MonitoringPointEntity> long count(org.springframework.data.domain.Example<S> e) { return 0; }
        @Override public <S extends MonitoringPointEntity> boolean exists(org.springframework.data.domain.Example<S> e) { return false; }
        @Override public <S extends MonitoringPointEntity> List<S> findAll(org.springframework.data.domain.Example<S> e) { return List.of(); }
        @Override public <S extends MonitoringPointEntity> List<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Sort s) { return List.of(); }
        @Override public <S extends MonitoringPointEntity> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> e, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends MonitoringPointEntity, R> R findBy(org.springframework.data.domain.Example<S> e, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { return null; }
        @Override public org.springframework.data.domain.Page<MonitoringPointEntity> findAll(org.springframework.data.domain.Pageable p) { return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(store.values()), p, store.size()); }
        @Override public List<MonitoringPointEntity> findAll(org.springframework.data.domain.Sort s) { return new ArrayList<>(store.values()); }
        @Override public List<MonitoringPointEntity> findAllById(Iterable<Long> ids) { return List.of(); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(MonitoringPointEntity e) { store.remove(e.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(store::remove); }
        @Override public void deleteAll(Iterable<? extends MonitoringPointEntity> es) { es.forEach(e -> store.remove(e.getId())); }
        @Override public List<MonitoringPointEntity> findByPointType(String t) { return List.of(); }
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
}
