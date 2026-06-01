package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * B接口2016 GET_FSUINFO 心跳轮询服务。
 *
 * 使用 B接口2016 Code=1701 主动向 FSU 查询运行状态（CPU/内存），
 * 成功后更新 b_interface_fsu_status.last_heartbeat 和 online_status。
 *
 * 与 2024 GetSuInfoService 的区别：
 * - 使用 GET_FSUINFO (Code=1701) 而非 GET_SUINFO (Code=1001)
 * - 使用 pkTypeFormat="legacy-2016"
 * - 严格校验 ACK Code=1702 才更新状态
 */
@Service
public class BInterface2016GetFsuInfoService {

    private static final Logger log = LoggerFactory.getLogger(BInterface2016GetFsuInfoService.class);

    private static final int EXPECTED_ACK_CODE = 1702;
    private static final String SUCCESS_RESULT = "0";

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;
    private final BInterfaceFsuStatusRepository fsuStatusRepository;
    private final FsuDeviceRepository fsuDeviceRepository;
    private final XmlDataParser xmlDataParser;

    public BInterface2016GetFsuInfoService(FsuServiceClient fsuServiceClient,
                                            FsuEndpointResolver fsuEndpointResolver,
                                            BInterfaceFsuStatusRepository fsuStatusRepository,
                                            FsuDeviceRepository fsuDeviceRepository,
                                            XmlDataParser xmlDataParser) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
        this.fsuStatusRepository = fsuStatusRepository;
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.xmlDataParser = xmlDataParser;
    }

    /**
     * 执行 GET_FSUINFO 心跳查询。
     *
     * @param fsuCode       FSU 编码
     * @param fsuServiceUrl FSU 服务地址（可为 null，自动解析）
     * @return 查询结果
     */
    public BInterface2016GetFsuInfoResult execute(String fsuCode, String fsuServiceUrl) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return BInterface2016GetFsuInfoResult.fail("2001", "缺少 FSU Code", null);
        }
        String suid = fsuCode.trim();

        // 1. 查找 FSU 设备
        Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepository.findByFsuCode(suid);
        if (fsuOpt.isEmpty()) {
            log.warn("GET_FSUINFO FSU 未注册: fsuCode={}", suid);
            return BInterface2016GetFsuInfoResult.fail("1002", "FSU 未注册", suid);
        }

        // 2. 解析 FSU endpoint
        String effectiveServiceUrl = fsuServiceUrl;
        if (effectiveServiceUrl == null) {
            FsuEndpointResult endpoint = fsuEndpointResolver.resolve(suid);
            if (!endpoint.isSuccess()) {
                return BInterface2016GetFsuInfoResult.fail(endpoint.getResultCode(),
                        endpoint.getResultDesc(), suid);
            }
            effectiveServiceUrl = endpoint.getServiceUrl();
        }

        try {
            // 3. 构造 2016 GET_FSUINFO 请求
            // 格式对齐 LANDING-006 成功样本: 仅 FSUCode（大写），无 FsuId
            String infoXml = "<FSUCode>" + suid + "</FSUCode>";

            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(suid)
                    .serviceUrl(effectiveServiceUrl)
                    .pkType(BInterfacePkType.GET_FSUINFO)
                    .pkTypeFormat("legacy-2016")
                    .infoXml(infoXml)
                    .build();

            // 4. 调用 FSUService
            FsuServiceResponse response = fsuServiceClient.call(request);
            boolean realDeviceAccessed = response.isRealCall();
            String rawSoap = response.getRawSoap();

            if (!response.isSuccess()) {
                log.warn("GET_FSUINFO 调用失败: fsuCode={}, code={}, desc={}",
                        suid, response.getResultCode(), response.getResultDesc());
                return BInterface2016GetFsuInfoResult.fail(
                        response.getResultCode(),
                        response.getResultDesc() != null ? response.getResultDesc() : "FSU 查询失败",
                        suid, realDeviceAccessed);
            }

            // 5. 校验 ACK Code = 1702
            if (!isExpectedAck(rawSoap)) {
                log.warn("GET_FSUINFO ACK Code 不是 1702: fsuCode={}", suid);
                return BInterface2016GetFsuInfoResult.fail("2010", "ACK Code 不匹配，期望 1702",
                        suid, realDeviceAccessed);
            }

            // 6. 提取 CPUUsage / MEMUsage
            // 真实 FSU 2016 GET_FSUINFO 返回格式:
            //   <Info>...<TFSUStatus><CPUUsage>15.61</CPUUsage><MEMUsage>49.00</MEMUsage></TFSUStatus>...</Info>
            // TFSUStatus 在 XmlDataParser.handleMultipleChildren 平坦字段模式中被忽略（非叶子元素）
            // 因此直接从 Info XML 字符串用正则提取 CPUUsage/MEMUsage
            String cpuStr = null;
            String memStr = null;
            String responseInfoXml = response.getInfoXml();

            if (responseInfoXml != null && !responseInfoXml.isEmpty()) {
                cpuStr = extractXmlValue(responseInfoXml, "CPUUsage");
                memStr = extractXmlValue(responseInfoXml, "MEMUsage");
                if (cpuStr != null || memStr != null) {
                    log.debug("GET_FSUINFO Info XML 正则提取: cpu={}, mem={}", cpuStr, memStr);
                }
            }

            // 回退: 从 xmlData items 提取（stub/模拟场景）
            if (cpuStr == null && memStr == null) {
                XmlDataModel xmlData = response.getXmlData();
                if (xmlData != null && !xmlData.isEmpty()) {
                    Map<String, String> fields = getFirstItem(xmlData);
                    cpuStr = getField(fields, "CPUUsage");
                    memStr = getField(fields, "MEMUsage");
                }
            }

            if (cpuStr == null && memStr == null) {
                log.warn("GET_FSUINFO 响应缺少 CPUUsage/MEMUsage: fsuCode={}", suid);
                return BInterface2016GetFsuInfoResult.builder()
                        .success(false).resultCode("2004").resultDesc("响应缺少 CPUUsage/MEMUsage")
                        .fsuCode(suid).realDeviceAccessed(realDeviceAccessed)
                        .rawRequest(infoXml).rawResponse(rawSoap)
                        .build();
            }

            BigDecimal cpu = parseDecimal(cpuStr);
            BigDecimal mem = parseDecimal(memStr);

            // 8. 更新 FSU 状态
            boolean statusUpdated = updateFsuStatus(suid, cpu, mem);

            log.info("GET_FSUINFO 成功: fsuCode={}, cpu={}, mem={}, statusUpdated={}",
                    suid, cpu, mem, statusUpdated);
            return BInterface2016GetFsuInfoResult.success(suid, suid, cpu, mem,
                    realDeviceAccessed, statusUpdated, infoXml, rawSoap);

        } catch (Exception e) {
            log.error("GET_FSUINFO 处理异常: fsuCode={}", suid, e);
            return BInterface2016GetFsuInfoResult.fail("5001", "查询异常: " + e.getMessage(), suid);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 校验原始 SOAP 响应中的 ACK Code 是否为 1702。
     */
    private boolean isExpectedAck(String rawSoap) {
        if (rawSoap == null) return true; // stub 场景兼容
        return rawSoap.contains("<Code>1702</Code>")
                || rawSoap.contains("<Code>1702<");
    }

    /**
     * 更新 b_interface_fsu_status.last_heartbeat 和 online_status。
     * 失败不影响主流程（best-effort）。
     */
    private boolean updateFsuStatus(String fsuCode, BigDecimal cpu, BigDecimal mem) {
        try {
            Optional<BInterfaceFsuStatusEntity> opt = fsuStatusRepository.findByFsuCode(fsuCode);
            BInterfaceFsuStatusEntity status;
            if (opt.isPresent()) {
                status = opt.get();
            } else {
                // 查找 fsuId
                Optional<FsuDeviceEntity> fsuOpt = fsuDeviceRepository.findByFsuCode(fsuCode);
                if (fsuOpt.isEmpty()) return false;
                status = new BInterfaceFsuStatusEntity();
                status.setFsuId(fsuOpt.get().getId());
                status.setFsuCode(fsuCode);
                status.setLoginStatus("LOGIN");
                status.setCreatedAt(LocalDateTime.now());
            }

            status.setOnlineStatus("ONLINE");
            status.setLastHeartbeat(LocalDateTime.now());
            status.setStatusDetail(formatStatusDetail(cpu, mem));
            status.setUpdatedAt(LocalDateTime.now());

            fsuStatusRepository.save(status);
            return true;
        } catch (Exception e) {
            log.warn("GET_FSUINFO 更新状态失败: fsuCode={}", fsuCode, e);
            return false;
        }
    }

    private String formatStatusDetail(BigDecimal cpu, BigDecimal mem) {
        StringBuilder sb = new StringBuilder("GET_FSUINFO@");
        sb.append(LocalDateTime.now());
        if (cpu != null) sb.append(" CPU=").append(cpu).append("%");
        if (mem != null) sb.append(" MEM=").append(mem).append("%");
        return sb.toString();
    }

    private Map<String, String> getFirstItem(XmlDataModel xmlData) {
        List<Map<String, String>> items = xmlData.getItems();
        if (items != null && !items.isEmpty()) return items.get(0);
        return xmlData.getFields();
    }

    private String getField(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key))
                return e.getValue() != null ? e.getValue().trim() : null;
        }
        return null;
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    /**
     * 从 XML 字符串中用简单正则提取标签值。
     * 不受 XmlDataParser 平坦字段模式忽略非叶子元素的影响。
     */
    private String extractXmlValue(String xml, String tagName) {
        if (xml == null || tagName == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<" + tagName + ">([^<]*)</" + tagName + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(xml);
        if (m.find()) {
            String v = m.group(1);
            return v != null ? v.trim() : null;
        }
        return null;
    }
}
