package com.dcim.platform.binterface;

import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.binterface.command.*;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.entity.BInterfaceSessionEntity;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceSessionRepository;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.binterface.service.SendAlarmService;
import com.dcim.platform.module.binterface.service.SendDataService;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
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

/**
 * B接口主链路集成测试。
 *
 * 验证 LOGIN → HEARTBEAT → SEND_DATA → SEND_ALARM 四个核心流程
 * 按顺序协同工作，包括登录态校验、状态变化、数据隔离。
 *
 * 使用内存 HashMap 替代 JPA Repository，不依赖 Spring 上下文。
 */
class BInterfaceMainFlowIntegrationTest {

    // ==================== 桩仓库 ====================
    private StubFsuDeviceRepository fsuDeviceRepo;
    private StubFsuStatusRepository fsuStatusRepo;
    private StubSessionRepository sessionRepo;
    private StubMonitoringPointRepository monitoringPointRepo;
    private StubRealtimeDataRepository realtimeDataRepo;
    private StubAlarmRecordRepository alarmRecordRepo;

    // ==================== 真实服务 ====================
    private LoginService loginService;
    private SendDataService sendDataService;
    private SendAlarmService sendAlarmService;

    // ==================== 真实 Handler ====================
    private LoginCommandHandler loginHandler;
    private HeartbeatCommandHandler heartbeatHandler;
    private SendDataCommandHandler sendDataHandler;
    private SendAlarmCommandHandler sendAlarmHandler;

    private static final String FSU_CODE = "FSU-001";
    private static final Long FSU_ID = 1L;

    @BeforeEach
    void setUp() {
        // --- 初始化桩仓库 ---
        fsuDeviceRepo = new StubFsuDeviceRepository();
        fsuStatusRepo = new StubFsuStatusRepository();
        sessionRepo = new StubSessionRepository();
        monitoringPointRepo = new StubMonitoringPointRepository();
        realtimeDataRepo = new StubRealtimeDataRepository();
        alarmRecordRepo = new StubAlarmRecordRepository();

        // --- 预注册 FSU 设备 ---
        FsuDeviceEntity fsu = new FsuDeviceEntity();
        fsu.setId(FSU_ID);
        fsu.setFsuCode(FSU_CODE);
        fsu.setFsuName("测试FSU-001");
        fsu.setStatus("ONLINE");
        fsu.setCreatedAt(LocalDateTime.now());
        fsu.setUpdatedAt(LocalDateTime.now());
        fsuDeviceRepo.save(fsu);

        // --- 预注册测点（供 SEND_DATA 使用） ---
        createPoint(10L, "TEMP-001", "温度1", "analog", "float");
        createPoint(20L, "HUMI-001", "湿度1", "analog", "float");
        createPoint(30L, "VOLT-001", "电压1", "analog", "float");

        // --- 实例化真实服务 ---
        loginService = new LoginService(fsuDeviceRepo, fsuStatusRepo, sessionRepo);
        sendDataService = new SendDataService(fsuDeviceRepo, monitoringPointRepo, realtimeDataRepo);
        sendAlarmService = new SendAlarmService(fsuDeviceRepo, alarmRecordRepo);

        // --- 实例化真实 Handler ---
        loginHandler = new LoginCommandHandler(loginService);
        heartbeatHandler = new HeartbeatCommandHandler(loginService);
        sendDataHandler = new SendDataCommandHandler(loginService, sendDataService);
        sendAlarmHandler = new SendAlarmCommandHandler(loginService, sendAlarmService);
    }

    private void createPoint(Long id, String pointCode, String pointName,
                              String pointType, String dataType) {
        MonitoringPointEntity point = new MonitoringPointEntity();
        point.setId(id);
        point.setFsuId(FSU_ID);
        point.setPointCode(pointCode);
        point.setPointName(pointName);
        point.setPointType(pointType);
        point.setDataType(dataType);
        point.setStatus("ACTIVE");
        point.setCreatedAt(LocalDateTime.now());
        point.setUpdatedAt(LocalDateTime.now());
        monitoringPointRepo.save(point);
    }

    // ============================================================
    // 1. 全链路：LOGIN → HEARTBEAT → SEND_DATA → SEND_ALARM 成功
    // ============================================================

    @Test
    void loginHeartbeatSendDataSendAlarmFullFlowShouldSucceed() {
        // ---- 1. LOGIN ----
        CommandContext loginCtx = loginContext();
        CommandResult loginResult = loginHandler.handle(loginCtx);
        assertTrue(loginResult.isSuccess(), "LOGIN 应成功");
        assertEquals("1", loginResult.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");

        // 验证 Session 已创建
        assertTrue(loginService.isLoggedIn(FSU_CODE), "LOGIN 后 isLoggedIn 应为 true");

        // ---- 2. HEARTBEAT ----
        CommandContext hbCtx = heartbeatContext(FSU_CODE);
        CommandResult hbResult = heartbeatHandler.handle(hbCtx);
        assertTrue(hbResult.isSuccess(), "HEARTBEAT 应成功");
        assertEquals("1", hbResult.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");

        // 验证 lastHeartbeat 已更新
        Optional<BInterfaceFsuStatusEntity> status = loginService.getStatus(FSU_CODE);
        assertTrue(status.isPresent());
        assertNotNull(status.get().getLastHeartbeat(), "lastHeartbeat 应被更新");

        // ---- 3. SEND_DATA ----
        CommandContext sdCtx = sendDataContext(FSU_CODE, "2026-05-14T10:30:00+08:00", createSendDataXml());
        CommandResult sdResult = sendDataHandler.handle(sdCtx);
        assertTrue(sdResult.isSuccess(), "SEND_DATA 应成功");
        assertEquals("1", sdResult.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");

        // 验证实时数据已写入
        assertEquals(3, realtimeDataRepo.findAll().size(), "应有 3 条实时数据");

        // ---- 4. SEND_ALARM ----
        CommandContext saCtx = sendAlarmContext(FSU_CODE, "2026-05-14T10:33:00+08:00", createSendAlarmXml());
        CommandResult saResult = sendAlarmHandler.handle(saCtx);
        assertTrue(saResult.isSuccess(), "SEND_ALARM 应成功");
        assertEquals("1", saResult.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");

        // 验证告警已写入
        assertEquals(2, alarmRecordRepo.findAll().size(), "应有 2 条告警记录");
    }

    // ============================================================
    // 2. 顺序依赖：未 LOGIN 时所有命令都失败
    // ============================================================

    @Test
    void heartbeatWithoutLoginShouldFail() {
        CommandResult result = heartbeatHandler.handle(heartbeatContext(FSU_CODE));
        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void sendDataWithoutLoginShouldFail() {
        CommandResult result = sendDataHandler.handle(
                sendDataContext(FSU_CODE, null, createSendDataXml()));
        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    @Test
    void sendAlarmWithoutLoginShouldFail() {
        CommandResult result = sendAlarmHandler.handle(
                sendAlarmContext(FSU_CODE, null, createSendAlarmXml()));
        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ============================================================
    // 3. 登录态校验：LOGIN 后 HEARTBEAT/SEND_DATA/SEND_ALARM 成功
    // ============================================================

    @Test
    void loginThenHeartbeatShouldSucceed() {
        loginHandler.handle(loginContext());
        CommandResult result = heartbeatHandler.handle(heartbeatContext(FSU_CODE));
        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");
    }

    @Test
    void loginThenSendDataShouldSucceed() {
        loginHandler.handle(loginContext());
        CommandResult result = sendDataHandler.handle(
                sendDataContext(FSU_CODE, null, createSendDataXml()));
        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");
    }

    @Test
    void loginThenSendAlarmShouldSucceed() {
        loginHandler.handle(loginContext());
        CommandResult result = sendAlarmHandler.handle(
                sendAlarmContext(FSU_CODE, null, createSendAlarmXml()));
        assertTrue(result.isSuccess());
        assertEquals("1", result.getResultCode(), "Result=1 SUCCESS per 2016 EnumResult");
    }

    // ============================================================
    // 4. 状态变化验证
    // ============================================================

    @Test
    void loginShouldCreateSession() {
        loginHandler.handle(loginContext());
        assertTrue(loginService.isLoggedIn(FSU_CODE));
        Optional<BInterfaceSessionEntity> session = loginService.getActiveSession(FSU_CODE);
        assertTrue(session.isPresent());
        assertEquals("ACTIVE", session.get().getStatus());
        assertEquals(FSU_CODE, session.get().getFsuCode());
    }

    @Test
    void heartbeatShouldUpdateLastSeen() {
        loginHandler.handle(loginContext());

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        heartbeatHandler.handle(heartbeatContext(FSU_CODE));

        Optional<BInterfaceFsuStatusEntity> status = loginService.getStatus(FSU_CODE);
        assertTrue(status.isPresent());
        assertNotNull(status.get().getLastHeartbeat());
        assertTrue(status.get().getLastHeartbeat().isAfter(before)
                || status.get().getLastHeartbeat().isEqual(before));
    }

    @Test
    void sendDataShouldPersistRealtimeData() {
        loginHandler.handle(loginContext());
        sendDataHandler.handle(
                sendDataContext(FSU_CODE, "2026-05-14T10:30:00+08:00", createSendDataXml()));

        List<RealtimeDataEntity> allData = realtimeDataRepo.findAll();
        assertEquals(3, allData.size());
        assertTrue(allData.stream().anyMatch(d -> "TEMP-001".equals(d.getPointCode())));
        assertTrue(allData.stream().anyMatch(d -> "HUMI-001".equals(d.getPointCode())));
        assertTrue(allData.stream().anyMatch(d -> "VOLT-001".equals(d.getPointCode())));
    }

    @Test
    void sendAlarmShouldPersistActiveAlarms() {
        loginHandler.handle(loginContext());
        sendAlarmHandler.handle(
                sendAlarmContext(FSU_CODE, "2026-05-14T10:33:00+08:00", createSendAlarmXml()));

        List<AlarmRecordEntity> allAlarms = alarmRecordRepo.findAll();
        assertEquals(2, allAlarms.size());
        assertTrue(allAlarms.stream().allMatch(a -> "ACTIVE".equals(a.getAlarmStatus())));
        assertTrue(allAlarms.stream().anyMatch(a -> "TEMP-HIGH".equals(a.getAlarmCode())));
        assertTrue(allAlarms.stream().anyMatch(a -> "DOOR-OPEN".equals(a.getAlarmCode())));
    }

    @Test
    void sendAlarmRecoverShouldUpdateAlarmStatus() {
        loginHandler.handle(loginContext());

        // 先产生告警
        sendAlarmHandler.handle(
                sendAlarmContext(FSU_CODE, "2026-05-14T10:33:00+08:00",
                        createSingleAlarmXml("TEMP-001", "TEMP-HIGH", "WARN", "0")));

        // 再恢复告警
        sendAlarmHandler.handle(
                sendAlarmContext(FSU_CODE, "2026-05-14T10:35:00+08:00",
                        createSingleAlarmXml("TEMP-001", "TEMP-HIGH", "WARN", "1")));

        List<AlarmRecordEntity> allAlarms = alarmRecordRepo.findAll();
        assertEquals(1, allAlarms.size(), "恢复应更新已有记录而非新建");
        assertEquals("RECOVERED", allAlarms.get(0).getAlarmStatus(), "告警状态应变为 RECOVERED");
    }

    // ============================================================
    // 5. 数据隔离验证
    // ============================================================

    @Test
    void sendDataShouldNotCreateAlarms() {
        loginHandler.handle(loginContext());
        sendDataHandler.handle(
                sendDataContext(FSU_CODE, null, createSendDataXml()));
        assertEquals(0, alarmRecordRepo.findAll().size(),
                "SEND_DATA 不应创建告警");
    }

    @Test
    void sendAlarmShouldNotCreateRealtimeData() {
        loginHandler.handle(loginContext());
        sendAlarmHandler.handle(
                sendAlarmContext(FSU_CODE, null, createSendAlarmXml()));
        assertEquals(0, realtimeDataRepo.findAll().size(),
                "SEND_ALARM 不应创建实时数据");
    }

    @Test
    void heartbeatShouldNotCreateDataOrAlarms() {
        loginHandler.handle(loginContext());
        heartbeatHandler.handle(heartbeatContext(FSU_CODE));

        assertEquals(0, realtimeDataRepo.findAll().size(),
                "HEARTBEAT 不应创建实时数据");
        assertEquals(0, alarmRecordRepo.findAll().size(),
                "HEARTBEAT 不应创建告警");
    }

    @Test
    void loginShouldNotCreateDataOrAlarms() {
        loginHandler.handle(loginContext());

        assertEquals(0, realtimeDataRepo.findAll().size(),
                "LOGIN 不应创建实时数据");
        assertEquals(0, alarmRecordRepo.findAll().size(),
                "LOGIN 不应创建告警");
    }

    // ============================================================
    // 6. 边界场景
    // ============================================================

    @Test
    void duplicateLoginShouldNotBreakSession() {
        loginHandler.handle(loginContext());
        // 第一次登录后 session 存在
        assertTrue(loginService.isLoggedIn(FSU_CODE));

        // 第二次登录
        CommandResult secondLogin = loginHandler.handle(loginContext());
        assertTrue(secondLogin.isSuccess());

        // 新的 Session 应仍有效
        assertTrue(loginService.isLoggedIn(FSU_CODE));
    }

    @Test
    void duplicateHeartbeatShouldKeepUpdatingLastSeen() {
        loginHandler.handle(loginContext());

        heartbeatHandler.handle(heartbeatContext(FSU_CODE));
        Optional<BInterfaceFsuStatusEntity> afterFirst = loginService.getStatus(FSU_CODE);
        LocalDateTime firstHeartbeat = afterFirst.get().getLastHeartbeat();

        // 第二次心跳
        heartbeatHandler.handle(heartbeatContext(FSU_CODE));
        Optional<BInterfaceFsuStatusEntity> afterSecond = loginService.getStatus(FSU_CODE);

        // lastHeartbeat 应更新（或至少不早于第一次）
        assertNotNull(afterSecond.get().getLastHeartbeat());
        assertTrue(!afterSecond.get().getLastHeartbeat().isBefore(firstHeartbeat));
    }

    @Test
    void sendDataAfterHeartbeatShouldKeepSessionValid() {
        loginHandler.handle(loginContext());
        heartbeatHandler.handle(heartbeatContext(FSU_CODE));

        // heartbeat 后 session 仍有效
        assertTrue(loginService.isLoggedIn(FSU_CODE));

        // SEND_DATA 应成功
        CommandResult result = sendDataHandler.handle(
                sendDataContext(FSU_CODE, null, createSendDataXml()));
        assertTrue(result.isSuccess());
    }

    @Test
    void sendAlarmAfterHeartbeatShouldKeepSessionValid() {
        loginHandler.handle(loginContext());
        heartbeatHandler.handle(heartbeatContext(FSU_CODE));

        assertTrue(loginService.isLoggedIn(FSU_CODE));

        CommandResult result = sendAlarmHandler.handle(
                sendAlarmContext(FSU_CODE, null, createSendAlarmXml()));
        assertTrue(result.isSuccess());
    }

    // ============================================================
    // 7. 缺少 FSUCode
    // ============================================================

    @Test
    void loginShouldFailOnMissingFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<OtherField>value</OtherField>");
        CommandContext ctx = new CommandContext(BInterfacePkType.LOGIN, msg, null, null, null);
        CommandResult result = loginHandler.handle(ctx);
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void sendDataShouldFailOnMissingFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<OtherField>value</OtherField>");
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_DATA, msg, null, null, null);
        CommandResult result = sendDataHandler.handle(ctx);
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void sendAlarmShouldFailOnMissingFsuCode() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo("<OtherField>value</OtherField>");
        CommandContext ctx = new CommandContext(BInterfacePkType.SEND_ALARM, msg, null, null, null);
        CommandResult result = sendAlarmHandler.handle(ctx);
        assertEquals("2001", result.getResultCode());
    }

    // ============================================================
    // 辅助方法：构建 CommandContext
    // ============================================================

    private CommandContext loginContext() {
        String infoXml = "<FSUCode>" + FSU_CODE + "</FSUCode>";
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.LOGIN);
        return new CommandContext(BInterfacePkType.LOGIN, msg, null, null, null);
    }

    private CommandContext heartbeatContext(String fsuCode) {
        String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.HEARTBEAT);
        return new CommandContext(BInterfacePkType.HEARTBEAT, msg, null, null, null);
    }

    private CommandContext sendDataContext(String fsuCode, String collectTime, XmlDataModel xmlData) {
        String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";
        if (collectTime != null) {
            infoXml = "<CollectTime>" + collectTime + "</CollectTime>" + infoXml;
        }
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.SEND_DATA);
        return new CommandContext(BInterfacePkType.SEND_DATA, msg, xmlData, null, null);
    }

    private CommandContext sendAlarmContext(String fsuCode, String alarmTime, XmlDataModel xmlData) {
        String infoXml = "<FSUCode>" + fsuCode + "</FSUCode>";
        if (alarmTime != null) {
            infoXml = "<AlarmTime>" + alarmTime + "</AlarmTime>" + infoXml;
        }
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setInfo(infoXml);
        msg.setPkType(BInterfacePkType.SEND_ALARM);
        return new CommandContext(BInterfacePkType.SEND_ALARM, msg, xmlData, null, null);
    }

    // ============================================================
    // 辅助方法：创建 XmlDataModel
    // ============================================================

    private XmlDataModel createSendDataXml() {
        XmlDataModel model = new XmlDataModel();
        model.addItem(signalItem("TEMP-001", "25.5"));
        model.addItem(signalItem("HUMI-001", "60.2"));
        model.addItem(signalItem("VOLT-001", "220.0"));
        return model;
    }

    private Map<String, String> signalItem(String signalId, String value) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("SignalID", signalId);
        item.put("Value", value);
        item.put("Quality", "0");
        item.put("Status", "normal");
        return item;
    }

    private XmlDataModel createSendAlarmXml() {
        XmlDataModel model = new XmlDataModel();
        model.addItem(alarmItem("TEMP-001", "TEMP-HIGH", "温度过高", "WARN", "62.0", "0"));
        model.addItem(alarmItem("DOOR-001", "DOOR-OPEN", "门磁告警", "CRITICAL", "1", "0"));
        return model;
    }

    private XmlDataModel createSingleAlarmXml(String signalId, String alarmCode,
                                               String alarmLevel, String alarmType) {
        XmlDataModel model = new XmlDataModel();
        model.addItem(alarmItem(signalId, alarmCode, null, alarmLevel, null, alarmType));
        return model;
    }

    private Map<String, String> alarmItem(String signalId, String alarmCode, String alarmName,
                                           String alarmLevel, String alarmValue, String alarmType) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("SignalID", signalId);
        item.put("AlarmCode", alarmCode);
        if (alarmName != null) item.put("AlarmName", alarmName);
        item.put("AlarmLevel", alarmLevel);
        if (alarmValue != null) item.put("AlarmValue", alarmValue);
        item.put("AlarmType", alarmType);
        return item;
    }

    // ============================================================
    // BaseStub — 所有桩仓库的基类
    // ============================================================

    @SuppressWarnings("unchecked")
    abstract static class BaseStub<T> {
        final Map<Long, T> store = new HashMap<>();
        long nextId = 1;

        public T saveEntity(T entity, Long id) {
            if (id == null) id = nextId++;
            setId(entity, id);
            store.put(id, entity);
            return entity;
        }

        protected abstract Long getId(T entity);
        protected abstract void setId(T entity, Long id);

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
        public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> qf) { return null; }
        public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable p) { return org.springframework.data.domain.Page.empty(); }
        public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable p) { return new org.springframework.data.domain.PageImpl<>(List.copyOf(store.values())); }
    }

    static class StubFsuDeviceRepository extends BaseStub<FsuDeviceEntity> implements FsuDeviceRepository {
        @Override protected Long getId(FsuDeviceEntity e) { return e.getId(); }
        @Override protected void setId(FsuDeviceEntity e, Long id) { e.setId(id); }
        @Override public FsuDeviceEntity save(FsuDeviceEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override
        public Optional<FsuDeviceEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> e.getFsuCode().equals(fsuCode)).findFirst();
        }
        @Override public List<FsuDeviceEntity> findBySiteId(Long siteId) { return List.of(); }
        @Override public List<FsuDeviceEntity> findByStatus(String status) { return List.of(); }
    }

    static class StubFsuStatusRepository extends BaseStub<BInterfaceFsuStatusEntity> implements BInterfaceFsuStatusRepository {
        @Override protected Long getId(BInterfaceFsuStatusEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceFsuStatusEntity e, Long id) { e.setId(id); }
        @Override public BInterfaceFsuStatusEntity save(BInterfaceFsuStatusEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override
        public Optional<BInterfaceFsuStatusEntity> findByFsuCode(String fsuCode) {
            return store.values().stream().filter(e -> fsuCode.equals(e.getFsuCode())).findFirst();
        }
        @Override
        public Optional<BInterfaceFsuStatusEntity> findByFsuId(Long fsuId) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId())).findFirst();
        }
        @Override
        public List<BInterfaceFsuStatusEntity> findByOnlineStatus(String onlineStatus) {
            return store.values().stream().filter(e -> onlineStatus.equals(e.getOnlineStatus())).collect(java.util.stream.Collectors.toList());
        }
    }

    static class StubSessionRepository extends BaseStub<BInterfaceSessionEntity> implements BInterfaceSessionRepository {
        @Override protected Long getId(BInterfaceSessionEntity e) { return e.getId(); }
        @Override protected void setId(BInterfaceSessionEntity e, Long id) { e.setId(id); }
        @Override public BInterfaceSessionEntity save(BInterfaceSessionEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override
        public Optional<BInterfaceSessionEntity> findBySessionId(String sessionId) {
            return store.values().stream().filter(e -> sessionId.equals(e.getSessionId())).findFirst();
        }
        @Override
        public Optional<BInterfaceSessionEntity> findByFsuIdAndStatus(Long fsuId, String status) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId()) && status.equals(e.getStatus())).findFirst();
        }
    }

    static class StubMonitoringPointRepository extends BaseStub<MonitoringPointEntity> implements MonitoringPointRepository {
        @Override protected Long getId(MonitoringPointEntity e) { return e.getId(); }
        @Override protected void setId(MonitoringPointEntity e, Long id) { e.setId(id); }
        @Override public MonitoringPointEntity save(MonitoringPointEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override
        public Optional<MonitoringPointEntity> findByFsuIdAndPointCode(Long fsuId, String pointCode) {
            return store.values().stream().filter(e -> fsuId.equals(e.getFsuId()) && pointCode.equals(e.getPointCode())).findFirst();
        }
        @Override public List<MonitoringPointEntity> findByFsuId(Long fsuId) { return List.of(); }
        @Override public List<MonitoringPointEntity> findByPointType(String pointType) { return List.of(); }
    }

    static class StubRealtimeDataRepository extends BaseStub<RealtimeDataEntity> implements RealtimeDataRepository {
        @Override protected Long getId(RealtimeDataEntity e) { return e.getId(); }
        @Override protected void setId(RealtimeDataEntity e, Long id) { e.setId(id); }
        @Override public RealtimeDataEntity save(RealtimeDataEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override
        public Optional<RealtimeDataEntity> findByPointId(Long pointId) {
            return store.values().stream().filter(e -> pointId.equals(e.getPointId())).findFirst();
        }
        @Override public List<RealtimeDataEntity> findByFsuId(Long fsuId) { return List.of(); }
    }

    static class StubAlarmRecordRepository extends BaseStub<AlarmRecordEntity> implements AlarmRecordRepository {
        @Override protected Long getId(AlarmRecordEntity e) { return e.getId(); }
        @Override protected void setId(AlarmRecordEntity e, Long id) { e.setId(id); }
        @Override public AlarmRecordEntity save(AlarmRecordEntity entity) { return saveEntity(entity, entity.getId()); }
        @Override public List<AlarmRecordEntity> findByFsuId(Long fsuId) { return List.of(); }
        @Override public List<AlarmRecordEntity> findByAlarmStatus(String alarmStatus) { return List.of(); }
        @Override public List<AlarmRecordEntity> findByAlarmLevelAndAlarmStatus(String level, String status) { return List.of(); }
        @Override
        public Optional<AlarmRecordEntity> findByFsuIdAndPointCodeAndAlarmCodeAndAlarmStatus(
                Long fsuId, String pointCode, String alarmCode, String alarmStatus) {
            return store.values().stream()
                    .filter(e -> fsuId.equals(e.getFsuId())
                            && pointCode.equals(e.getPointCode())
                            && alarmCode.equals(e.getAlarmCode())
                            && alarmStatus.equals(e.getAlarmStatus()))
                    .findFirst();
        }
        @Override public Optional<AlarmRecordEntity> findByFsuIdAndSerialNo(Long fsuId, String serialNo) { return Optional.empty(); }
        @Override public Optional<AlarmRecordEntity> findByFsuIdAndDeviceIdAndPointCodeAndAlarmStatus(Long fsuId, String deviceId, String pointCode, String status) { return Optional.empty(); }
        @Override public List<AlarmRecordEntity> findByFsuIdAndAlarmStatus(Long fsuId, String status) { return List.of(); }
    }
}
