package com.dcim.platform.module.binterface.log;

import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * B接口报文日志服务。
 *
 * 保存完整的 SOAP/XML 原始报文，用于协议分析和审计。
 *
 * 安全规则：
 * - save 方法失败时记录日志但不抛异常，不阻塞主链路 ACK
 * - 同步保存（项目未启用 @Async，不使用异步线程池）
 * - 保存失败返回 null，调用方应检查返回值
 */
@Service
public class BInterfaceMessageLogService {

    private static final Logger log = LoggerFactory.getLogger(BInterfaceMessageLogService.class);

    public static final String DIRECTION_INBOUND = "INBOUND";
    public static final String DIRECTION_OUTBOUND = "OUTBOUND";
    public static final String MESSAGE_TYPE_SOAP = "SOAP";

    private final BInterfaceMessageLogRepository repository;

    public BInterfaceMessageLogService(BInterfaceMessageLogRepository repository) {
        this.repository = repository;
    }

    /**
     * 保存入站报文（FSU → SC）。
     *
     * @param command    命令类型（LOGIN/HEARTBEAT/SEND_DATA/SEND_ALARM，解析失败可为 UNKNOWN）
     * @param fsuCode    FSU 编码（解析失败可为 null）
     * @param rawMessage 完整原始 SOAP/XML 请求体
     * @return 保存成功的实体，失败时返回 null（不抛异常）
     */
    @Transactional
    public BInterfaceMessageLogEntity saveInbound(String command, String fsuCode, String rawMessage) {
        return save(DIRECTION_INBOUND, command, fsuCode, MESSAGE_TYPE_SOAP, rawMessage);
    }

    /**
     * 保存出站报文（SC → FSU）。
     *
     * @param command    命令类型
     * @param fsuCode    FSU 编码（可为 null）
     * @param rawMessage 完整原始 SOAP/XML 响应体
     * @return 保存成功的实体，失败时返回 null（不抛异常）
     */
    @Transactional
    public BInterfaceMessageLogEntity saveOutbound(String command, String fsuCode, String rawMessage) {
        return save(DIRECTION_OUTBOUND, command, fsuCode, MESSAGE_TYPE_SOAP, rawMessage);
    }

    /**
     * 按 FSU 编码查询报文日志。
     */
    public List<BInterfaceMessageLogEntity> findByFsuCode(String fsuCode) {
        try {
            return repository.findByFsuCodeOrderByCreatedAtDesc(fsuCode);
        } catch (Exception e) {
            log.error("报文日志查询失败 fsuCode={}", fsuCode, e);
            return Collections.emptyList();
        }
    }

    /**
     * 按命令类型查询报文日志。
     */
    public List<BInterfaceMessageLogEntity> findByCommand(String command) {
        try {
            return repository.findByCommandOrderByCreatedAtDesc(command);
        } catch (Exception e) {
            log.error("报文日志查询失败 command={}", command, e);
            return Collections.emptyList();
        }
    }

    /**
     * 分页多条件查询报文日志。
     *
     * @param direction   方向（INBOUND/OUTBOUND），null 表示不过滤
     * @param command     命令类型，null 表示不过滤
     * @param fsuCode     FSU 编码，null 表示不过滤
     * @param messageType 消息类型，null 表示不过滤
     * @param pageable    分页参数
     * @return 分页结果，查询失败返回空 Page
     */
    public Page<BInterfaceMessageLogEntity> query(String direction, String command,
                                                   String fsuCode, String messageType,
                                                   Pageable pageable) {
        try {
            if (direction != null && !direction.isBlank()) {
                return repository.findByDirectionOrderByCreatedAtDesc(direction, pageable);
            }
            if (command != null && !command.isBlank()) {
                return repository.findByCommandOrderByCreatedAtDesc(command, pageable);
            }
            if (fsuCode != null && !fsuCode.isBlank()) {
                return repository.findByFsuCodeOrderByCreatedAtDesc(fsuCode, pageable);
            }
            if (messageType != null && !messageType.isBlank()) {
                return repository.findByMessageTypeOrderByCreatedAtDesc(messageType, pageable);
            }
            // 无条件时返回所有（按 FSU Code 排序）
            return repository.findAll(pageable);
        } catch (Exception e) {
            log.error("报文日志查询失败 direction={} command={} fsuCode={} messageType={}: {}",
                    direction, command, fsuCode, messageType, e.getMessage(), e);
            return Page.empty(pageable);
        }
    }

    /**
     * 删除指定时间之前的报文日志。
     *
     * 仅删除 binterface_message_log 表记录，不影响任何业务数据。
     * 清理失败时记录日志但不抛异常。
     *
     * @param cutoff 删除截止时间（删除 createdAt < cutoff 的记录）
     * @return 删除的记录数
     */
    public long cleanBefore(LocalDateTime cutoff) {
        if (cutoff == null) {
            log.warn("cleanBefore cutoff 为 null，跳过清理");
            return 0;
        }
        try {
            long deleted = repository.deleteByCreatedAtBefore(cutoff);
            log.info("报文日志清理完成: 删除 {} 条记录 (cutoff={})", deleted, cutoff);
            return deleted;
        } catch (Exception e) {
            log.error("报文日志清理失败 cutoff={}: {}", cutoff, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 删除 N 天前的报文日志。
     *
     * 仅删除 binterface_message_log 表记录，不影响任何业务数据。
     * 必须显式调用，不会自动触发。
     *
     * @param days 保留天数（必须 > 0，否则抛 IllegalArgumentException）
     * @return 删除的记录数
     * @throws IllegalArgumentException days <= 0
     */
    public long cleanOlderThanDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days 必须大于 0，当前值: " + days);
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        return cleanBefore(cutoff);
    }

    // ==================== 内部方法 ====================

    /**
     * 保存报文日志。
     *
     * 失败时记录错误日志但不抛异常 — 报文日志不是主链路，不应影响 FSU ACK。
     */
    private BInterfaceMessageLogEntity save(String direction, String command,
                                             String fsuCode, String messageType, String rawMessage) {
        if (rawMessage == null) {
            rawMessage = "";
        }

        try {
            BInterfaceMessageLogEntity entity = new BInterfaceMessageLogEntity();
            entity.setDirection(direction);
            entity.setCommand(command != null ? command : "UNKNOWN");
            entity.setFsuCode(fsuCode);
            entity.setMessageType(messageType);
            entity.setRawMessage(rawMessage);
            entity.setCreatedAt(LocalDateTime.now());

            BInterfaceMessageLogEntity saved = repository.saveAndFlush(entity);
            log.info("报文日志已保存: command={} fsuCode={} id={} direction={}",
                    command, fsuCode, saved.getId(), direction);
            return saved;

        } catch (Exception e) {
            log.error("报文日志保存失败: command={} fsuCode={} direction={}",
                    command, fsuCode, direction, e);
            return null;
        }
    }
}
