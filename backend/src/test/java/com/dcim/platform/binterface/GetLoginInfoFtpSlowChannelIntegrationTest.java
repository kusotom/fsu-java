package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.GetFtpCommandHandler;
import com.dcim.platform.module.binterface.command.GetLoginInfoCommandHandler;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.binterface.service.GetFtpService;
import com.dcim.platform.module.binterface.service.GetLoginInfoService;
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
 * GET_LOGININFO / GET_FTP 慢数据通道集成测试。
 *
 * <p>使用真实 Handler + 真实 Service + Stub Repository + Stub FSU Client，
 * 验证查询全链路的正确性和登录态校验。</p>
 */
class GetLoginInfoFtpSlowChannelIntegrationTest {

    private LoginService loginService;
    private GetLoginInfoService getLoginInfoService;
    private GetFtpService getFtpService;
    private GetLoginInfoCommandHandler getLoginInfoHandler;
    private GetFtpCommandHandler getFtpHandler;

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
        getLoginInfoService = new GetLoginInfoService(fsuClient, null);
        getFtpService = new GetFtpService(fsuClient, null);
        getLoginInfoHandler = new GetLoginInfoCommandHandler(loginService, getLoginInfoService);
        getFtpHandler = new GetFtpCommandHandler(loginService, getFtpService);

        // 预置 FSU endpoint 数据
        fsu.setIpAddr("192.168.1.100");
        fsu.setPort(8080);
    }

    // ==================== GET_LOGININFO 已登录可查询 ====================

    @Test
    void shouldQueryLoginInfoWhenLoggedIn() {
        loginService.login("FSU-001", null);

        CommandResult result = getLoginInfoHandler.handle(createLoginInfoContext("FSU-001"));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertNotNull(result.toXmlDataXml());
    }

    @Test
    void shouldReturnLoginStatusFields() {
        loginService.login("FSU-001", null);

        CommandResult result = getLoginInfoHandler.handle(createLoginInfoContext("FSU-001"));

        String xmlData = result.toXmlDataXml();
        assertNotNull(xmlData);
        assertTrue(xmlData.contains("LoginStatus"));
        assertTrue(xmlData.contains("OnlineStatus"));
    }

    // ==================== GET_LOGININFO 未登录拒绝 ====================

    @Test
    void shouldFailWhenNotLoggedInForLoginInfo() {
        CommandResult result = getLoginInfoHandler.handle(createLoginInfoContext("FSU-001"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void shouldFailWhenFsuOfflineForLoginInfo() {
        loginService.login("FSU-001", null);
        loginService.clearSession("FSU-001");

        CommandResult result = getLoginInfoHandler.handle(createLoginInfoContext("FSU-001"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== GET_FTP 已登录可查询 ====================

    @Test
    void shouldQueryFtpConfigWhenLoggedIn() {
        loginService.login("FSU-001", null);

        CommandResult result = getFtpHandler.handle(createFtpContext("FSU-001", "IMAGE"));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertNotNull(result.toXmlDataXml());
    }

    @Test
    void shouldReturnFtpConfigFields() {
        loginService.login("FSU-001", null);

        CommandResult result = getFtpHandler.handle(createFtpContext("FSU-001", "IMAGE"));

        String xmlData = result.toXmlDataXml();
        assertNotNull(xmlData);
        assertTrue(xmlData.contains("Host"));
        assertTrue(xmlData.contains("Port"));
        assertTrue(xmlData.contains("Username"));
        assertTrue(xmlData.contains("PassiveMode"));
        assertTrue(xmlData.contains("BasePath"));
    }

    // ==================== GET_FTP 未登录拒绝 ====================

    @Test
    void shouldFailWhenNotLoggedInForFtp() {
        CommandResult result = getFtpHandler.handle(createFtpContext("FSU-001", "IMAGE"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== 参数校验 ====================

    @Test
    void shouldFailOnMissingFsuCodeForLoginInfo() {
        loginService.login("FSU-001", null);

        CommandResult result = getLoginInfoHandler.handle(createLoginInfoContext(null));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCodeForFtp() {
        loginService.login("FSU-001", null);

        CommandResult result = getFtpHandler.handle(createFtpContext(null, "IMAGE"));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    // ==================== 安全验证 ====================

    @Test
    void shouldNotModifyLoginState() {
        // 验证查询不修改 FSU 登录状态
        loginService.login("FSU-001", null);

        getLoginInfoHandler.handle(createLoginInfoContext("FSU-001"));
        getFtpHandler.handle(createFtpContext("FSU-001", "IMAGE"));

        assertTrue(loginService.isLoggedIn("FSU-001"));
    }

    @Test
    void shouldNotAccessRealNetwork() {
        loginService.login("FSU-001", null);

        CommandResult result = getLoginInfoHandler.handle(createLoginInfoContext("FSU-001"));
        assertTrue(result.isSuccess());

        result = getFtpHandler.handle(createFtpContext("FSU-001", "IMAGE"));
        assertTrue(result.isSuccess());
        // StubFsuClient 保证不访问网络
    }

    // ==================== Helper ====================

    private CommandContext createLoginInfoContext(String fsuCode) {
        BInterfaceMessage msg = new BInterfaceMessage();
        if (fsuCode != null) {
            msg.setInfo("<FSUCode>" + fsuCode + "</FSUCode>");
        } else {
            msg.setInfo("<ResultCode>0</ResultCode>");
        }
        msg.setPkType(BInterfacePkType.GET_LOGININFO);
        return new CommandContext(BInterfacePkType.GET_LOGININFO, msg, null, null, null);
    }

    private CommandContext createFtpContext(String fsuCode, String fileType) {
        BInterfaceMessage msg = new BInterfaceMessage();
        StringBuilder info = new StringBuilder();
        if (fsuCode != null) {
            info.append("<FSUCode>").append(fsuCode).append("</FSUCode>");
        }
        if (fileType != null) {
            info.append("<FileType>").append(fileType).append("</FileType>");
        }
        if (info.length() == 0) {
            info.append("<ResultCode>0</ResultCode>");
        }
        msg.setInfo(info.toString());
        msg.setPkType(BInterfacePkType.GET_FTP);
        return new CommandContext(BInterfacePkType.GET_FTP, msg, null, null, null);
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
        public List<T> findAll(Sort sort) { return List.copyOf(store.values()); }
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
        @Override
        public FsuServiceResponse call(FsuServiceRequest request) {
            if (request.getPkType() == BInterfacePkType.GET_LOGININFO) {
                return buildLoginInfoResponse();
            } else if (request.getPkType() == BInterfacePkType.GET_FTP) {
                return buildFtpResponse();
            }
            return FsuServiceResponse.fail("1", "不支持的 PK_Type");
        }

        private FsuServiceResponse buildLoginInfoResponse() {
            XmlDataModel xmlData = new XmlDataModel();
            xmlData.setRootName("LoginInfo");
            var item = new LinkedHashMap<String, String>();
            item.put("FSUCode", "FSU-001");
            item.put("LoginStatus", "LOGIN");
            item.put("OnlineStatus", "ONLINE");
            item.put("SessionID", "SESSION-FSU-001-20260513-A3B8");
            item.put("LoginTime", "2026-05-13T10:30:00+08:00");
            item.put("LastHeartbeat", "2026-05-13T10:40:00+08:00");
            xmlData.addItem(item);

            return FsuServiceResponse.success(
                    "<soap:Envelope><soap:Body><Response>"
                    + "<PK_Type>GET_LOGININFO</PK_Type>"
                    + "<Info><ResultCode>0</ResultCode></Info>"
                    + "<xmlData><LoginInfo>"
                    + "<FSUCode>FSU-001</FSUCode>"
                    + "<LoginStatus>LOGIN</LoginStatus>"
                    + "<OnlineStatus>ONLINE</OnlineStatus>"
                    + "<SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>"
                    + "<LoginTime>2026-05-13T10:30:00+08:00</LoginTime>"
                    + "<LastHeartbeat>2026-05-13T10:40:00+08:00</LastHeartbeat>"
                    + "</LoginInfo></xmlData></Response></soap:Body></soap:Envelope>",
                    "<ResultCode>0</ResultCode>",
                    "<LoginInfo><FSUCode>FSU-001</FSUCode><LoginStatus>LOGIN</LoginStatus>"
                    + "<OnlineStatus>ONLINE</OnlineStatus><SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>"
                    + "<LoginTime>2026-05-13T10:30:00+08:00</LoginTime>"
                    + "<LastHeartbeat>2026-05-13T10:40:00+08:00</LastHeartbeat></LoginInfo>",
                    xmlData);
        }

        private FsuServiceResponse buildFtpResponse() {
            XmlDataModel xmlData = new XmlDataModel();
            xmlData.setRootName("FTPConfig");
            var item = new LinkedHashMap<String, String>();
            item.put("Host", "192.168.1.200");
            item.put("Port", "21");
            item.put("Username", "fsu_ftp");
            item.put("PassiveMode", "true");
            item.put("BasePath", "/fsu/images/");
            xmlData.addItem(item);

            return FsuServiceResponse.success(
                    "<soap:Envelope><soap:Body><Response>"
                    + "<PK_Type>GET_FTP</PK_Type>"
                    + "<Info><ResultCode>0</ResultCode></Info>"
                    + "<xmlData><FTPConfig>"
                    + "<Host>192.168.1.200</Host><Port>21</Port>"
                    + "<Username>fsu_ftp</Username><PassiveMode>true</PassiveMode>"
                    + "<BasePath>/fsu/images/</BasePath>"
                    + "</FTPConfig></xmlData></Response></soap:Body></soap:Envelope>",
                    "<ResultCode>0</ResultCode>",
                    "<FTPConfig><Host>192.168.1.200</Host><Port>21</Port>"
                    + "<Username>fsu_ftp</Username><PassiveMode>true</PassiveMode>"
                    + "<BasePath>/fsu/images/</BasePath></FTPConfig>",
                    xmlData);
        }
    }
}
