# LANDING-010 Plan：BInterfaceMessageLog 日志清理与查询增强

> 计划版本：1.0
> 创建日期：2026-05-20
> 对应 Spec：`openspec/specs/landing-010-binterface-message-log-retention.md`

---

## 1. 读取资料

| # | 文件 | 用途 |
|---|------|------|
| 1 | `BInterfaceMessageLogRepository.java` | 当前 Repository（2 查询方法） |
| 2 | `BInterfaceMessageLogService.java` | 当前 Service（save + 2 query） |
| 3 | `BInterfaceMessageLogQueryService.java` | 现有查询服务（list/getById） |
| 4 | `BInterfaceMessageLogController.java` | 现有 Controller（GET /list, GET /{id}） |
| 5 | `BInterfaceMessageLogEntity.java` | Entity 字段确认 |
| 6 | `BInterfaceMessageLogServiceTest.java` | 现有测试（11 tests） |
| 7 | `ApiResponse.java` | 响应格式 |

## 2. 现有代码核对

| 组件 | 方法 | 返回 |
|------|------|------|
| Repository | `findByFsuCodeOrderByCreatedAtDesc(String)` | `List` |
| Repository | `findByCommandOrderByCreatedAtDesc(String)` | `List` |
| Service | `saveInbound/saveOutbound` | `Entity/null` |
| Service | `findByFsuCode/findByCommand` | `List` |
| QueryService | `list()` | `List` |
| QueryService | `getById(Long)` | `Entity` |
| Controller | `GET /` | `ApiResponse<List>` |
| Controller | `GET /{id}` | `ApiResponse<Entity>` |

## 3. TDD 测试计划

### 3.1 新增测试：BInterfaceMessageLogServiceTest

| 测试 | 覆盖 |
|------|------|
| shouldQueryByDirectionWithPagination | direction=INBOUND, page=0, size=10 |
| shouldQueryByFsuCodeWithPagination | fsuCode, 分页 |
| shouldQueryByMessageTypeWithPagination | messageType=SOAP |
| shouldQueryByTimeRange | startTime/endTime 过滤 |
| shouldCleanBeforeCutoff | 删除 3 条旧记录，保留 2 条新记录 |
| shouldCleanOlderThanDays | 删除 >7 天记录 |
| shouldRejectOlderThanDaysZero | days=0 → IllegalArgumentException |
| shouldRejectOlderThanDaysNegative | days=-1 → IllegalArgumentException |
| shouldCleanReturnZeroWhenNoMatch | 无可删除记录 → 0 |

### 3.2 新增测试：BInterfaceMessageLogControllerTest

| 测试 | 覆盖 |
|------|------|
| shouldReturnPaginatedResults | GET query 分页 |
| shouldFilterByCommand | GET ?command=SEND_ALARM |
| shouldFilterByFsuCode | GET ?fsuCode=FSU-001 |
| shouldFilterByDirection | GET ?direction=INBOUND |
| shouldFilterByMessageType | GET ?messageType=SOAP |
| shouldDeleteByCleanupBefore | DELETE cleanup?before=... |
| shouldDeleteByCleanupOlderThanDays | DELETE cleanup?olderThanDays=30 |
| shouldRejectCleanupInvalidDays | DELETE cleanup?olderThanDays=0 → 400 |
| shouldKeepBackwardCompatibleList | GET / still works |
| shouldKeepBackwardCompatibleGetById | GET /{id} still works |

## 4. 实现步骤

### Step 1: Repository 增强

```java
// BInterfaceMessageLogRepository 新增:
Page<BInterfaceMessageLogEntity> findByDirectionOrderByCreatedAtDesc(String direction, Pageable pageable);
Page<BInterfaceMessageLogEntity> findByCommandOrderByCreatedAtDesc(String command, Pageable pageable);
Page<BInterfaceMessageLogEntity> findByFsuCodeOrderByCreatedAtDesc(String fsuCode, Pageable pageable);
Page<BInterfaceMessageLogEntity> findByMessageTypeOrderByCreatedAtDesc(String messageType, Pageable pageable);
@Modifying @Transactional long deleteByCreatedAtBefore(LocalDateTime cutoff);
```

### Step 2: Service 增强

```java
// BInterfaceMessageLogService 新增:
Page<BInterfaceMessageLogEntity> query(String direction, String command, String fsuCode, String messageType, Pageable pageable);
long cleanBefore(LocalDateTime cutoff);
long cleanOlderThanDays(int days);  // days > 0
```

### Step 3: Controller 增强

保留现有端点，新增：
```java
@GetMapping("/query")     // 分页查询
@DeleteMapping("/cleanup") // 清理
```

### Step 4: 测试验证

```bash
mvn test -Dtest='*BInterfaceMessageLog*'
mvn test -Dtest='*BInterface*'
mvn test
```

## 5. 验证命令

```bash
# 1. 单元测试
mvn test -Dtest='*BInterfaceMessageLog*'

# 2. B接口回归
mvn test -Dtest='*BInterface*'

# 3. 全量回归
mvn test

# 4. 确认无 Java 意外修改
git diff --stat -- backend/src/main/java/
```

## 6. 输出文档

| # | 文件 | 说明 |
|---|------|------|
| 1 | `openspec/specs/landing-010-*.md` | Spec |
| 2 | `openspec/plans/landing-010-*.md` | Plan（本文件） |
| 3 | `docs/audit/LANDING-010-*.md` | 审计文档 |
| 4 | `docs/memory/2026-05-20-LANDING-010-*.md` | 工程记忆 |
| 5 | `docs/memory/README.md` | 索引更新 |
| 6 | `docs/memory/WORKING-MEMORY.md` | 工作记忆更新 |
| 7 | `docs/landing/PASSIVE-REPORTING-INTEGRATION-PLAN.md` | 集成方案更新 |
| 8 | `docs/landing/PASSIVE-REPORTING-CHECKLIST.md` | 检查清单更新 |

## 7. 完成条件

| # | 条件 |
|---|------|
| 1 | Repository 新增 4 个分页查询 + 1 个删除方法 |
| 2 | Service 新增 query/cleanBefore/cleanOlderThanDays |
| 3 | Controller 新增分页查询 + 清理端点，保留旧接口兼容 |
| 4 | 全量测试 0 failures, 0 errors |
| 5 | 未访问真实 FSU |
| 6 | 未执行 SET |
| 7 | 未启用 Scheduler |
| 8 | memory + audit 已写入 |
