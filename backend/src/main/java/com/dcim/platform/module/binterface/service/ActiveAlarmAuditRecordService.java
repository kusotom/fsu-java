package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.ActiveAlarmAuditRecordEntity;
import com.dcim.platform.module.binterface.repository.ActiveAlarmAuditRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 活动告警审计结果持久化与查询服务 (BIF-P4-023)。
 *
 * <p>负责审计结果转换、持久化和只读查询。不调用 FSU、不执行 SET、不修改 alarm_record。</p>
 */
@Service
public class ActiveAlarmAuditRecordService {

    private static final Logger log = LoggerFactory.getLogger(ActiveAlarmAuditRecordService.class);

    private final ActiveAlarmAuditRecordRepository repository;
    private final ObjectMapper objectMapper;

    public ActiveAlarmAuditRecordService(ActiveAlarmAuditRecordRepository repository,
                                          ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    /**
     * 持久化审计结果。
     */
    public ActiveAlarmAuditRecordEntity save(String fsuCode,
                                              ActiveAlarmConsistencyAuditResult result,
                                              String errorMessage) {
        String resultJson = buildResultJson(result);
        ActiveAlarmAuditRecordEntity entity = ActiveAlarmAuditRecordEntity.from(
                fsuCode, result, errorMessage, resultJson);
        ActiveAlarmAuditRecordEntity saved = repository.save(entity);
        log.debug("审计记录已持久化: id={}, fsuCode={}, success={}", saved.getId(), fsuCode, saved.isSuccess());
        return saved;
    }

    /**
     * 查询最近一次审计记录。
     */
    public Optional<ActiveAlarmAuditRecordEntity> findLatest() {
        return repository.findTopByOrderByRunAtDesc();
    }

    /**
     * 查询指定 FSU 最近一次审计记录。
     */
    public Optional<ActiveAlarmAuditRecordEntity> findLatestByFsuCode(String fsuCode) {
        return repository.findTopByFsuCodeOrderByRunAtDesc(fsuCode);
    }

    /**
     * 分页查询审计历史。fsuCode 为 null 时返回全部记录。
     */
    public Page<ActiveAlarmAuditRecordEntity> findHistory(String fsuCode, Pageable pageable) {
        if (fsuCode != null && !fsuCode.trim().isEmpty()) {
            return repository.findByFsuCodeOrderByRunAtDesc(fsuCode.trim(), pageable);
        }
        return repository.findAllByOrderByRunAtDesc(pageable);
    }

    private String buildResultJson(ActiveAlarmConsistencyAuditResult result) {
        if (result == null) return null;
        try {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("success", result.isSuccess());
            summary.put("suid", result.getSuid());
            summary.put("fsuQueryExecuted", result.isFsuQueryExecuted());
            summary.put("realDeviceAccessed", result.isRealDeviceAccessed());
            summary.put("queryResultCode", result.getQueryResultCode());
            summary.put("fsuCount", result.getFsuCount());
            summary.put("localCount", result.getLocalCount());
            summary.put("matchedCount", result.getMatchedCount());
            summary.put("fsuOnlyCount", result.getFsuOnlyCount());
            summary.put("localOnlyCount", result.getLocalOnlyCount());
            summary.put("mismatchCount", result.getMismatchCount());
            summary.put("hasInconsistency", result.hasInconsistency());
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            log.warn("审计结果 JSON 序列化失败: fsuCode={}", result.getSuid(), e);
            return null;
        }
    }
}
