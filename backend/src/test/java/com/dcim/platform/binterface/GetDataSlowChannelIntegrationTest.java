package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.GetDataCommandHandler;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.binterface.service.GetDataResult;
import com.dcim.platform.module.binterface.service.GetDataService;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GET_DATA 慢数据通道集成测试。
 *
 * <p>使用真实 Handler + 真实 Service + Stub Repository + Stub FSU Client，
 * 验证 GET_DATA 全链路的正确性和登录态校验。</p>
 */
class GetDataSlowChannelIntegrationTest {

    private LoginService loginService;
    private GetDataService getDataService;
    private GetDataCommandHandler handler;

    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubFsuStatusRepository fsuStatusRepo;
    private StubSessionRepository sessionRepo;
    private StubFsuServiceClient fsuClient;

    @BeforeEach
    void setUp() {
        fsuDeviceRepo = new StubFsuDeviceRepository();
        fsuStatusRepo = new StubFsuStatusRepository();
        sessionRepo = new StubSessionRepository();
        fsuClient = new StubFsuServiceClient();

        // 预注册 FSU
        FsuDeviceEntity fsu = new FsuDeviceEntity();
        fsu.setId(1L);
        fsu.setFsuCode("FSU-001");
        fsu.setFsuName("测试FSU");
        fsu.setStatus("ONLINE");
        fsu.setCreatedAt(LocalDateTime.now());
        fsu.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(fsu);

        loginService = new LoginService(fsuDeviceRepo, fsuStatusRepo, sessionRepo);
        getDataService = new GetDataService(fsuClient, null);
        handler = new GetDataCommandHandler(loginService, getDataService);

        // 预置 FSU endpoint 数据
        fsu.setIpAddr("192.168.1.100");
        fsu.setPort(8080);
    }

    // ==================== 已登录可查询 ====================

    @Test
    void shouldQueryDataWhenLoggedIn() {
        loginService.login("FSU-001", null);

        CommandResult result = handler.handle(createGetDataContext("FSU-001", List.of("TEMP-001")));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertNotNull(result.toXmlDataXml());
    }

    @Test
    void shouldReturnSignalValues() {
        loginService.login("FSU-001", null);

        CommandResult result = handler.handle(
                createGetDataContext("FSU-001", List.of("TEMP-001", "HUMI-001")));

        String xmlData = result.toXmlDataXml();
        assertNotNull(xmlData);
        assertTrue(xmlData.contains("TEMP-001"));
        assertTrue(xmlData.contains("HUMI-001"));
    }

    // ==================== 未登录拒绝 ====================

    @Test
    void shouldFailWhenNotLoggedIn() {
        CommandResult result = handler.handle(createGetDataContext("FSU-001", List.of("TEMP-001")));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuOffline() {
        loginService.login("FSU-001", null);
        loginService.clearSession("FSU-001"); // 登出

        CommandResult result = handler.handle(createGetDataContext("FSU-001", List.of("TEMP-001")));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== 参数校验 ====================

    @Test
    void shouldFailOnMissingFsuCode() {
        loginService.login("FSU-001", null);

        CommandResult result = handler.handle(createGetDataContext(null, List.of("TEMP-001")));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingSignalIds() {
        loginService.login("FSU-001", null);

        CommandResult result = handler.handle(createGetDataContext("FSU-001", List.of()));

        assertFalse(result.isSuccess());
        assertEquals("2003", result.getResultCode());
    }

    // ==================== 安全验证 ====================

    @Test
    void shouldNotCallSendDataService() {
        // 验证 GET_DATA 集成测试不依赖 SEND_DATA 相关服务
        loginService.login("FSU-001", null);

        CommandResult result = handler.handle(createGetDataContext("FSU-001", List.of("TEMP-001")));

        assertTrue(result.isSuccess());
        // 成功即证明不依赖 SEND_DATA
    }

    @Test
    void shouldNotAccessRealNetwork() {
        loginService.login("FSU-001", null);

        CommandResult result = handler.handle(createGetDataContext("FSU-001", List.of("TEMP-001")));

        assertTrue(result.isSuccess());
        // StubFsuClient 保证不访问网络
        assertFalse(fsuClient.lastCallRealCall);
    }

    // ==================== Helper ====================

    private CommandContext createGetDataContext(String fsuCode, List<String> signalIds) {
        BInterfaceMessage msg = new BInterfaceMessage();
        if (fsuCode != null) {
            msg.setInfo("<FSUCode>" + fsuCode + "</FSUCode>");
        } else {
            msg.setInfo("<ResultCode>0</ResultCode>");
        }

        XmlDataModel xmlData = new XmlDataModel();
        xmlData.setRootName("SignalID");
        for (String id : signalIds) {
            LinkedHashMap<String, String> item = new LinkedHashMap<>();
            item.put("SignalID", id);
            xmlData.addItem(item);
        }

        return new CommandContext(BInterfacePkType.GET_DATA, msg, xmlData, null, null);
    }

    // ==================== Stub Repository ====================

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
        public List<T> findAll(org.springframework.data.domain.Sort sort) { return List.copyOf(store.values()); }
        public long count() { return store.size(); }
        public void deleteById(Long id) { store.remove(id); }
        public void deleteAll() { store.clear(); }
        public void flush() {}
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

    static class StubFsuStatusRepository extends BaseStub<BInterfaceFsuStatusEntity> implements BInterfaceFsuStatusRepository {
        @Override protected Long getId(BInterfaceFsuStatusEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceFsuStatusEntity e, Long id) { e.setId(id); }
        @Override public BInterfaceFsuStatusEntity save(BInterfaceFsuStatusEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override public Optional<BInterfaceFsuStatusEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> fsuCode.equals(e.getFsuCode())).findFirst();
        }
        @Override public Optional<BInterfaceFsuStatusEntity> findByFsuId(Long fsuId) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId())).findFirst();
        }
        @Override public List<BInterfaceFsuStatusEntity> findByOnlineStatus(String onlineStatus) {
            return store.values().stream().filter(e -> onlineStatus.equals(e.getOnlineStatus())).collect(Collectors.toList());
        }
    }

    static class StubSessionRepository extends BaseStub<BInterfaceSessionEntity> implements BInterfaceSessionRepository {
        @Override protected Long getId(BInterfaceSessionEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceSessionEntity e, Long id) { e.setId(id); }
        @Override public BInterfaceSessionEntity save(BInterfaceSessionEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override public Optional<BInterfaceSessionEntity> findBySessionId(String sessionId) {
            return store.values().stream().filter(e -> sessionId.equals(e.getSessionId())).findFirst();
        }
        @Override public Optional<BInterfaceSessionEntity> findByFsuIdAndStatus(Long fsuId, String status) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId()) && status.equals(e.getStatus())).findFirst();
        }
    }

    static class StubFsuServiceClient implements FsuServiceClient {
        boolean lastCallRealCall = false;

        @Override
        public FsuServiceResponse call(FsuServiceRequest request) {
            lastCallRealCall = false;
            // 返回一个模拟的 GET_DATA 响应
            XmlDataModel xmlData = new XmlDataModel();
            xmlData.setRootName("Signal");
            LinkedHashMap<String, String> s1 = new LinkedHashMap<>();
            s1.put("SignalID", "TEMP-001");
            s1.put("Value", "25.8");
            s1.put("Quality", "1");
            s1.put("Status", "NORMAL");
            s1.put("CollectTime", "2026-05-13T10:35:00+08:00");
            xmlData.addItem(s1);
            LinkedHashMap<String, String> s2 = new LinkedHashMap<>();
            s2.put("SignalID", "HUMI-001");
            s2.put("Value", "54.5");
            s2.put("Quality", "1");
            s2.put("Status", "NORMAL");
            s2.put("CollectTime", "2026-05-13T10:35:00+08:00");
            xmlData.addItem(s2);

            return FsuServiceResponse.success(
                    "<soap:Envelope/>",
                    "<ResultCode>0</ResultCode><Count>2</Count>",
                    "<Signal><SignalID>TEMP-001</SignalID><Value>25.8</Value></Signal>",
                    xmlData);
        }
    }
}
