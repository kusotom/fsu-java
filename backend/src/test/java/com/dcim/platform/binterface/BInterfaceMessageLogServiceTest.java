package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.log.BInterfaceMessageLogEntity;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BInterfaceMessageLogService 单元测试。
 *
 * 使用内存 HashMap 替代 JPA Repository，不依赖数据库。
 */
class BInterfaceMessageLogServiceTest {

    private BInterfaceMessageLogService logService;
    private StubMessageLogRepository repository;

    @BeforeEach
    void setUp() {
        repository = new StubMessageLogRepository();
        logService = new BInterfaceMessageLogService(repository);
    }

    // ==================== 正常保存 ====================

    @Test
    void shouldSaveInboundMessageSuccessfully() {
        BInterfaceMessageLogEntity saved = logService.saveInbound(
                "LOGIN", "51051243812345",
                "<soap:Envelope>...</soap:Envelope>");

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals("INBOUND", saved.getDirection());
        assertEquals("LOGIN", saved.getCommand());
        assertEquals("51051243812345", saved.getFsuCode());
        assertEquals("SOAP", saved.getMessageType());
        assertEquals("<soap:Envelope>...</soap:Envelope>", saved.getRawMessage());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void shouldSaveOutboundMessageSuccessfully() {
        BInterfaceMessageLogEntity saved = logService.saveOutbound(
                "GET_DATA_ACK", "51051243812345",
                "<soap:Envelope>...</soap:Envelope>");

        assertEquals("OUTBOUND", saved.getDirection());
        assertEquals("GET_DATA_ACK", saved.getCommand());
    }

    @Test
    void shouldSaveMessageWithNullFsuCode() {
        // FSUCode 解析失败时可为空
        BInterfaceMessageLogEntity saved = logService.saveInbound(
                "UNKNOWN", null,
                "<raw/>");

        assertNotNull(saved);
        assertEquals("UNKNOWN", saved.getCommand());
        assertNull(saved.getFsuCode());
    }

    @Test
    void shouldSaveMessageWithEmptyRawMessage() {
        BInterfaceMessageLogEntity saved = logService.saveInbound(
                "HEARTBEAT", "FSU-001", "");

        assertNotNull(saved);
        assertEquals("", saved.getRawMessage());
    }

    // ==================== 不回抛异常 ====================

    @Test
    void shouldNotThrowWhenSaveFails() {
        // 模拟 Repository 异常
        BInterfaceMessageLogService brokenService = new BInterfaceMessageLogService(
                new ThrowingMessageLogRepository());

        // 不应该抛异常
        BInterfaceMessageLogEntity result = brokenService.saveInbound(
                "LOGIN", "FSU-001", "<soap/>");

        // 失败时返回 null
        assertNull(result);
    }

    @Test
    void shouldNotThrowWhenSaveFailsWithNullInput() {
        BInterfaceMessageLogService brokenService = new BInterfaceMessageLogService(
                new ThrowingMessageLogRepository());

        // 不应该抛异常
        assertDoesNotThrow(() -> {
            brokenService.saveInbound(null, null, null);
        });
    }

    // ==================== 完整 SOAP 保存 ====================

    @Test
    void shouldSaveCompleteSoapEnvelope() {
        String completeSoap = """
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <invoke>
                      <xmlData>
                        <PK_Type><Name>LOGIN</Name><Code>101</Code></PK_Type>
                        <Info><FsuCode>51051243812345</FsuCode></Info>
                      </xmlData>
                    </invoke>
                  </soap:Body>
                </soap:Envelope>""";

        BInterfaceMessageLogEntity saved = logService.saveInbound(
                "LOGIN", "51051243812345", completeSoap);

        assertEquals(completeSoap, saved.getRawMessage());
        assertTrue(saved.getRawMessage().contains("soap:Envelope"));
        assertTrue(saved.getRawMessage().contains("PK_Type"));
    }

    // ==================== 查询 ====================

    @Test
    void shouldQueryByFsuCode() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-A", "<msg2/>");
        logService.saveInbound("LOGIN", "FSU-B", "<msg3/>");

        List<BInterfaceMessageLogEntity> result = logService.findByFsuCode("FSU-A");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(m -> "FSU-A".equals(m.getFsuCode())));
    }

    @Test
    void shouldQueryByCommand() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");
        logService.saveInbound("LOGIN", "FSU-C", "<msg3/>");

        List<BInterfaceMessageLogEntity> result = logService.findByCommand("LOGIN");

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(m -> "LOGIN".equals(m.getCommand())));
    }

    @Test
    void shouldReturnEmptyListWhenNoMatch() {
        List<BInterfaceMessageLogEntity> result = logService.findByFsuCode("NONEXISTENT");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 批量保存 ====================

    @Test
    void shouldSaveMultipleMessages() {
        logService.saveInbound("LOGIN", "FSU-001", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-001", "<msg2/>");
        logService.saveInbound("SEND_ALARM", "FSU-001", "<msg3/>");

        List<BInterfaceMessageLogEntity> all = repository.findAll();
        assertEquals(3, all.size());
    }

    // ==================== 分页查询 ====================

    @Test
    void shouldQueryByDirectionWithPagination() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");
        logService.saveInbound("SEND_ALARM", "FSU-A", "<msg3/>");

        var page = logService.query("INBOUND", null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 2));

        assertEquals(3, page.getTotalElements());
        assertEquals(2, page.getContent().size());
        assertTrue(page.getContent().stream().allMatch(m -> "INBOUND".equals(m.getDirection())));
    }

    @Test
    void shouldQueryByFsuCodeWithPagination() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");
        logService.saveInbound("SEND_ALARM", "FSU-A", "<msg3/>");

        var page = logService.query(null, null, "FSU-A", null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(m -> "FSU-A".equals(m.getFsuCode())));
    }

    @Test
    void shouldQueryByMessageTypeWithPagination() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");

        var page = logService.query(null, null, null, "SOAP",
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
    }

    @Test
    void shouldQueryByCommandWithPagination() {
        logService.saveInbound("LOGIN", "FSU-A", "<msg1/>");
        logService.saveInbound("HEARTBEAT", "FSU-B", "<msg2/>");
        logService.saveInbound("LOGIN", "FSU-C", "<msg3/>");

        var page = logService.query(null, "LOGIN", null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(2, page.getTotalElements());
        assertTrue(page.getContent().stream().allMatch(m -> "LOGIN".equals(m.getCommand())));
    }

    @Test
    void shouldReturnEmptyPageWhenNoMatch() {
        var page = logService.query("OUTBOUND", null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        assertEquals(0, page.getTotalElements());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void shouldQueryPageSizeLimit() {
        for (int i = 0; i < 10; i++) {
            logService.saveInbound("LOGIN", "FSU-001", "<msg/>");
        }

        var page = logService.query(null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 3));

        assertEquals(10, page.getTotalElements());
        assertEquals(3, page.getContent().size());
    }

    // ==================== 清理 ====================

    @Test
    void shouldCleanBeforeCutoff() {
        // 创建 5 条记录（使用不同 createdAt 模拟）
        BInterfaceMessageLogEntity e1 = newEntity("LOGIN", "FSU-A");
        e1.setCreatedAt(java.time.LocalDateTime.now().minusDays(10));
        repository.save(e1);
        BInterfaceMessageLogEntity e2 = newEntity("HEARTBEAT", "FSU-A");
        e2.setCreatedAt(java.time.LocalDateTime.now().minusDays(8));
        repository.save(e2);
        BInterfaceMessageLogEntity e3 = newEntity("SEND_ALARM", "FSU-B");
        e3.setCreatedAt(java.time.LocalDateTime.now().minusDays(1));
        repository.save(e3);
        BInterfaceMessageLogEntity e4 = newEntity("LOGIN", "FSU-C");
        e4.setCreatedAt(java.time.LocalDateTime.now().minusHours(1));
        repository.save(e4);
        BInterfaceMessageLogEntity e5 = newEntity("HEARTBEAT", "FSU-C");
        e5.setCreatedAt(java.time.LocalDateTime.now());
        repository.save(e5);

        // 清理 7 天前的记录
        long deleted = logService.cleanOlderThanDays(7);
        assertEquals(2, deleted);

        // 剩余 3 条
        assertEquals(3, repository.findAll().size());
    }

    @Test
    void shouldCleanBeforeExactCutoff() {
        BInterfaceMessageLogEntity old = newEntity("LOGIN", "FSU-A");
        old.setCreatedAt(java.time.LocalDateTime.of(2026, 1, 1, 0, 0));
        repository.save(old);
        BInterfaceMessageLogEntity recent = newEntity("HEARTBEAT", "FSU-B");
        recent.setCreatedAt(java.time.LocalDateTime.of(2026, 6, 1, 0, 0));
        repository.save(recent);

        long deleted = logService.cleanBefore(java.time.LocalDateTime.of(2026, 3, 1, 0, 0));
        assertEquals(1, deleted);
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldRejectOlderThanDaysZero() {
        assertThrows(IllegalArgumentException.class, () -> logService.cleanOlderThanDays(0));
    }

    @Test
    void shouldRejectOlderThanDaysNegative() {
        assertThrows(IllegalArgumentException.class, () -> logService.cleanOlderThanDays(-1));
    }

    @Test
    void shouldCleanReturnZeroWhenNoMatch() {
        BInterfaceMessageLogEntity recent = newEntity("LOGIN", "FSU-A");
        recent.setCreatedAt(java.time.LocalDateTime.now());
        repository.save(recent);

        long deleted = logService.cleanOlderThanDays(30);
        assertEquals(0, deleted);
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldCleanNotAffectOtherRepositories() {
        // 清理只应调用 BInterfaceMessageLogRepository，不影响业务 Repository
        logService.saveInbound("LOGIN", "FSU-A", "<msg/>");

        long deleted = logService.cleanBefore(java.time.LocalDateTime.now().plusDays(1));
        assertEquals(1, deleted);
        assertEquals(0, repository.findAll().size());
    }

    // ==================== 辅助 ====================

    private BInterfaceMessageLogEntity newEntity(String command, String fsuCode) {
        BInterfaceMessageLogEntity e = new BInterfaceMessageLogEntity();
        e.setDirection("INBOUND");
        e.setCommand(command);
        e.setFsuCode(fsuCode);
        e.setMessageType("SOAP");
        e.setRawMessage("<msg/>");
        e.setCreatedAt(java.time.LocalDateTime.now());
        return e;
    }

    // ==================== Stub ====================

    static class StubMessageLogRepository implements BInterfaceMessageLogRepository {
        private final AtomicLong idGen = new AtomicLong(1);
        private final Map<Long, BInterfaceMessageLogEntity> store = new LinkedHashMap<>();

        @Override
        public <S extends BInterfaceMessageLogEntity> S save(S entity) {
            if (entity.getId() == null) {
                entity.setId(idGen.getAndIncrement());
            }
            store.put(entity.getId(), entity);
            return entity;
        }

        @Override
        public List<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode) {
            return store.values().stream()
                    .filter(m -> fsuCode.equals(m.getFsuCode()))
                    .collect(Collectors.toList());
        }

        @Override
        public List<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command) {
            return store.values().stream()
                    .filter(m -> command.equals(m.getCommand()))
                    .collect(Collectors.toList());
        }

        @Override
        public Page<BInterfaceMessageLogEntity> findByDirectionOrderByCreatedAtDesc(String direction, Pageable pageable) {
            return pagedResult(
                    store.values().stream()
                            .filter(m -> direction.equals(m.getDirection()))
                            .collect(Collectors.toList()),
                    pageable);
        }

        @Override
        public Page<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode, Pageable pageable) {
            return pagedResult(
                    store.values().stream()
                            .filter(m -> fsuCode.equals(m.getFsuCode()))
                            .collect(Collectors.toList()),
                    pageable);
        }

        @Override
        public Page<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command, Pageable pageable) {
            return pagedResult(
                    store.values().stream()
                            .filter(m -> command.equals(m.getCommand()))
                            .collect(Collectors.toList()),
                    pageable);
        }

        @Override
        public Page<BInterfaceMessageLogEntity> findByMessageTypeOrderByCreatedAtDesc(String messageType, Pageable pageable) {
            return pagedResult(
                    store.values().stream()
                            .filter(m -> messageType.equals(m.getMessageType()))
                            .collect(Collectors.toList()),
                    pageable);
        }

        @Override
        public long deleteByCreatedAtBefore(java.time.LocalDateTime cutoff) {
            List<Long> toRemove = store.values().stream()
                    .filter(m -> m.getCreatedAt() != null && m.getCreatedAt().isBefore(cutoff))
                    .map(BInterfaceMessageLogEntity::getId)
                    .toList();
            toRemove.forEach(store::remove);
            return toRemove.size();
        }

        private Page<BInterfaceMessageLogEntity> pagedResult(List<BInterfaceMessageLogEntity> list, Pageable pageable) {
            int offset = (int) pageable.getOffset();
            int size = pageable.getPageSize();
            if (offset >= list.size()) {
                return new PageImpl<>(List.of(), pageable, list.size());
            }
            int end = Math.min(offset + size, list.size());
            return new PageImpl<>(list.subList(offset, end), pageable, list.size());
        }

        @Override
        public List<BInterfaceMessageLogEntity> findAll() {
            return new ArrayList<>(store.values());
        }

        // ===== 以下为 JpaRepository 接口方法（部分未实现，仅 stub） =====

        @Override public <S extends BInterfaceMessageLogEntity> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public Optional<BInterfaceMessageLogEntity> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public boolean existsById(Long id) { return store.containsKey(id); }
        @Override public List<BInterfaceMessageLogEntity> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return store.size(); }
        @Override public void deleteById(Long id) { store.remove(id); }
        @Override public void delete(BInterfaceMessageLogEntity entity) { store.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll(Iterable<? extends BInterfaceMessageLogEntity> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAll() { store.clear(); }
        @Override public void flush() {}
        @Override public <S extends BInterfaceMessageLogEntity> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends BInterfaceMessageLogEntity> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<BInterfaceMessageLogEntity> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch() { store.clear(); }
        @Override public BInterfaceMessageLogEntity getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public BInterfaceMessageLogEntity getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public BInterfaceMessageLogEntity getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public List<BInterfaceMessageLogEntity> findAll(Sort sort) { return findAll(); }
        @Override public Page<BInterfaceMessageLogEntity> findAll(Pageable pageable) { return pagedResult(new ArrayList<>(store.values()), pageable); }
    }

    static class ThrowingMessageLogRepository implements BInterfaceMessageLogRepository {
        @Override
        public <S extends BInterfaceMessageLogEntity> S save(S entity) {
            throw new RuntimeException("Simulated DB failure");
        }
        @Override
        public List<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode) {
            throw new RuntimeException("Simulated DB failure");
        }
        @Override
        public List<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command) {
            throw new RuntimeException("Simulated DB failure");
        }
        @Override
        public Page<BInterfaceMessageLogEntity> findByDirectionOrderByCreatedAtDesc(String direction, Pageable pageable) {
            throw new RuntimeException("Simulated DB failure");
        }
        @Override
        public Page<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode, Pageable pageable) {
            throw new RuntimeException("Simulated DB failure");
        }
        @Override
        public Page<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command, Pageable pageable) {
            throw new RuntimeException("Simulated DB failure");
        }
        @Override
        public Page<BInterfaceMessageLogEntity> findByMessageTypeOrderByCreatedAtDesc(String messageType, Pageable pageable) {
            throw new RuntimeException("Simulated DB failure");
        }
        @Override
        public long deleteByCreatedAtBefore(java.time.LocalDateTime cutoff) {
            throw new RuntimeException("Simulated DB failure");
        }
        // Minimal stubs for unimplemented methods
        @Override public Optional<BInterfaceMessageLogEntity> findById(Long id) { return Optional.empty(); }
        @Override public List<BInterfaceMessageLogEntity> findAll() { return List.of(); }
        @Override public void deleteAll() {}
        @Override public void flush() {}
        @Override public <S extends BInterfaceMessageLogEntity> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends BInterfaceMessageLogEntity> List<S> saveAll(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public boolean existsById(Long id) { return false; }
        @Override public List<BInterfaceMessageLogEntity> findAllById(Iterable<Long> ids) { throw new UnsupportedOperationException(); }
        @Override public long count() { return 0; }
        @Override public void deleteById(Long id) {}
        @Override public void delete(BInterfaceMessageLogEntity entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends BInterfaceMessageLogEntity> entities) {}
        @Override public <S extends BInterfaceMessageLogEntity> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        @Override public void deleteAllInBatch(Iterable<BInterfaceMessageLogEntity> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public BInterfaceMessageLogEntity getOne(Long id) { throw new UnsupportedOperationException(); }
        @Override public BInterfaceMessageLogEntity getById(Long id) { throw new UnsupportedOperationException(); }
        @Override public BInterfaceMessageLogEntity getReferenceById(Long id) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> Optional<S> findOne(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> List<S> findAll(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> List<S> findAll(Example<S> example, Sort sort) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> Page<S> findAll(Example<S> example, Pageable pageable) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> long count(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity> boolean exists(Example<S> example) { throw new UnsupportedOperationException(); }
        @Override public <S extends BInterfaceMessageLogEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { throw new UnsupportedOperationException(); }
        @Override public List<BInterfaceMessageLogEntity> findAll(Sort sort) { return findAll(); }
        @Override public Page<BInterfaceMessageLogEntity> findAll(Pageable pageable) { throw new UnsupportedOperationException(); }
    }
}
