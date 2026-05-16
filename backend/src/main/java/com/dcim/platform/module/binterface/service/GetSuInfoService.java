package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResolver;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResult;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GET_SUINFO 在线状态查询服务。
 *
 * <p>BIF-P4-012: SC 主动向 FSU 查询运行状态（CPU/内存/本地时间），
 * 用于心跳检测和在线状态维护。2024 协议 GET_SUINFO (Code=1001)。
 * 通过 FsuServiceClient 发送 SOAP 请求到 FSUService。</p>
 *
 * <p>与 HEARTBEAT 边界：</p>
 * <ul>
 *   <li>GET_SUINFO：SC→FSU，2024 标准心跳/状态查询</li>
 *   <li>HEARTBEAT：FSU→SC，2016 旧路径，保留兼容</li>
 *   <li>两者共同更新 lastSeen，不冲突</li>
 * </ul>
 */
@Service
public class GetSuInfoService {

    private static final Logger log = LoggerFactory.getLogger(GetSuInfoService.class);
    private static final DateTimeFormatter SU_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;
    private final BInterfaceFsuStatusRepository fsuStatusRepository;

    public GetSuInfoService(FsuServiceClient fsuServiceClient,
                            FsuEndpointResolver fsuEndpointResolver,
                            BInterfaceFsuStatusRepository fsuStatusRepository) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
        this.fsuStatusRepository = fsuStatusRepository;
    }

    /**
     * 执行 GET_SUINFO 查询。
     *
     * @param fsuCode       FSU 编码 (SUID)
     * @param fsuServiceUrl FSU 服务地址（可为 null，自动解析）
     * @return 查询结果
     */
    public GetSuInfoResult execute(String fsuCode, String fsuServiceUrl) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return GetSuInfoResult.fail("2001", "缺少 SUID");
        }
        String suid = fsuCode.trim();

        // 解析 FSU endpoint
        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null && fsuEndpointResolver != null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(suid);
            if (!endpoint.isSuccess()) {
                log.warn("FSU endpoint 解析失败: suid={}, code={}", suid, endpoint.getResultCode());
                return GetSuInfoResult.fail(endpoint.getResultCode(), endpoint.getResultDesc(), suid);
            }
            effectiveServiceUrl = endpoint.getServiceUrl();
        }

        try {
            // 1. 构造 Info (2024 字段: SUID)
            String infoXml = "<SUID>" + suid + "</SUID>";

            // 2. 构造 FsuServiceRequest
            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(suid)
                    .serviceUrl(effectiveServiceUrl)
                    .pkType(BInterfacePkType.GET_SUINFO)
                    .infoXml(infoXml)
                    .build();

            // 3. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);

            if (!response.isSuccess()) {
                log.warn("GET_SUINFO 调用失败: suid={}, code={}, desc={}",
                        suid, response.getResultCode(), response.getResultDesc());
                return GetSuInfoResult.fail(response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 查询失败", suid);
            }

            // 4. 解析响应 xmlData
            XmlDataModel xmlData = response.getXmlData();
            if (xmlData == null || xmlData.isEmpty()) {
                log.warn("GET_SUINFO 响应无数据: suid={}", suid);
                return GetSuInfoResult.fail("2003", "无有效响应数据", suid);
            }

            // 5. 提取 TSUStatus 字段
            List<Map<String, String>> items = xmlData.getItems();
            Map<String, String> statusMap;
            if (items != null && !items.isEmpty()) {
                statusMap = items.get(0);
            } else {
                statusMap = xmlData.getFields();
            }

            String cpuStr = getField(statusMap, "CPUUsage");
            String memStr = getField(statusMap, "MEMUsage");
            String timeStr = getField(statusMap, "SUDateTime");

            BigDecimal cpu = parseDecimal(cpuStr);
            BigDecimal mem = parseDecimal(memStr);
            LocalDateTime suTime = parseSuDateTime(timeStr);

            // 6. 更新在线状态
            updateOnlineStatus(suid, cpu, mem, suTime);

            log.debug("GET_SUINFO 成功: suid={}, cpu={}, mem={}, time={}", suid, cpu, mem, suTime);
            return GetSuInfoResult.success(suid, cpu, mem, suTime);

        } catch (Exception e) {
            log.error("GET_SUINFO 处理异常: suid={}", suid, e);
            return GetSuInfoResult.fail("5001", "查询异常: " + e.getMessage(), suid);
        }
    }

    // ==================== 内部方法 ====================

    private void updateOnlineStatus(String suid, BigDecimal cpu, BigDecimal mem, LocalDateTime suTime) {
        try {
            Optional<BInterfaceFsuStatusEntity> opt = fsuStatusRepository.findByFsuCode(suid);
            if (opt.isPresent()) {
                BInterfaceFsuStatusEntity status = opt.get();
                status.setOnlineStatus("ONLINE");
                status.setLastHeartbeat(LocalDateTime.now());
                if (suTime != null) {
                    status.setLastLoginTime(suTime);
                }
                fsuStatusRepository.save(status);
                log.debug("GET_SUINFO 更新在线状态: suid={}", suid);
            }
        } catch (Exception e) {
            log.warn("GET_SUINFO 更新状态失败: suid={}, error={}", suid, e.getMessage());
        }
    }

    private String getField(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key)) return e.getValue() != null ? e.getValue().trim() : null;
        }
        return null;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDateTime parseSuDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim(), SU_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            log.warn("SUDateTime 格式非法: {}", value);
            return null;
        }
    }
}
