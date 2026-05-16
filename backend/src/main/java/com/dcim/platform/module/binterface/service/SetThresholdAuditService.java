package com.dcim.platform.module.binterface.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * SET_THRESHOLD 操作审计服务。
 *
 * <p>记录每次门限设置操作的完整上下文。
 * 当前为内存+日志实现，重启后丢失审计记录。</p>
 *
 * <p>后续可扩展为 {@code SetThresholdAuditRepository} 持久化实现。
 * 接口方法当前为 final，子类可重写以支持持久化存储。</p>
 */
@Service
public class SetThresholdAuditService {

    private static final Logger log = LoggerFactory.getLogger(SetThresholdAuditService.class);

    private final List<SetThresholdAuditRecord> records =
            Collections.synchronizedList(new ArrayList<>());

    /**
     * 创建操作 ID 并构建审计 Builder 的起始部分。
     */
    public SetThresholdAuditRecord.Builder begin() {
        return SetThresholdAuditRecord.builder()
                .operationId(UUID.randomUUID().toString().replace("-", ""))
                .requestTime(java.time.LocalDateTime.now());
    }

    /**
     * 记录审计条目。
     *
     * @param record 完整的审计记录
     */
    public void record(SetThresholdAuditRecord record) {
        records.add(record);
        if (record.isSuccess()) {
            log.info("SET_THRESHOLD 审计: operationId={}, fsuCode={}, signalId={}, result={}",
                    record.getOperationId(), record.getFsuCode(), record.getSignalId(), record.getResultCode());
        } else {
            log.warn("SET_THRESHOLD 审计: operationId={}, fsuCode={}, signalId={}, result={}, errors={}",
                    record.getOperationId(), record.getFsuCode(), record.getSignalId(),
                    record.getResultCode(), record.getErrors());
        }
    }

    /**
     * 获取所有审计记录（按时间顺序）。
     */
    public List<SetThresholdAuditRecord> getRecords() {
        return List.copyOf(records);
    }

    /**
     * 清空审计记录（测试用）。
     */
    public void clear() {
        records.clear();
    }
}
