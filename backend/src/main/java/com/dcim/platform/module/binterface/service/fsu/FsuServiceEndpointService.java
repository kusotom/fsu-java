package com.dcim.platform.module.binterface.service.fsu;

import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * FSU 服务 endpoint 解析服务。
 *
 * <p>基于 FsuDeviceRepository 实现 FsuEndpointResolver 接口。
 * 来源优先级：</p>
 * <ol>
 *   <li>显式 serviceUrl（预留给 FsuDeviceEntity 未来扩展字段）</li>
 *   <li>ip + port + 默认 path 拼接</li>
 *   <li>未配置则返回结构化错误</li>
 * </ol>
 */
@Service
public class FsuServiceEndpointService implements FsuEndpointResolver {

    private static final Logger log = LoggerFactory.getLogger(FsuServiceEndpointService.class);

    /**
     * 默认 FSUService SOAP endpoint 路径。
     * 标准路径，真实联调时根据 FSU 厂商实际情况调整。
     */
    static final String DEFAULT_ENDPOINT_PATH = "/services/FSUService";

    private final FsuDeviceRepository fsuDeviceRepository;

    public FsuServiceEndpointService(FsuDeviceRepository fsuDeviceRepository) {
        this.fsuDeviceRepository = fsuDeviceRepository;
    }

    @Override
    public FsuEndpointResult resolve(String fsuCode) {
        if (fsuCode == null || fsuCode.trim().isEmpty()) {
            return FsuEndpointResult.fail(fsuCode, "2001", "缺少 FSUCode");
        }

        Optional<FsuDeviceEntity> entityOpt = fsuDeviceRepository.findByFsuCode(fsuCode.trim());
        if (entityOpt.isEmpty()) {
            log.warn("FSU 未找到: fsuCode={}", fsuCode);
            return FsuEndpointResult.fail(fsuCode, "2003", "FSU 未找到: " + fsuCode);
        }

        FsuDeviceEntity entity = entityOpt.get();

        // 未来扩展：entity 如有显式 serviceUrl 字段，优先使用

        String ipAddr = entity.getIpAddr();
        Integer port = entity.getPort();

        if (ipAddr == null || ipAddr.trim().isEmpty()) {
            log.warn("FSU IP 地址未配置: fsuCode={}", fsuCode);
            return FsuEndpointResult.fail(fsuCode, "2001", "FSU IP 地址未配置: " + fsuCode);
        }

        if (port == null || port <= 0) {
            log.warn("FSU 端口未配置: fsuCode={}", fsuCode);
            return FsuEndpointResult.fail(fsuCode, "2001", "FSU 端口未配置: " + fsuCode);
        }

        String serviceUrl = buildServiceUrl(ipAddr.trim(), port);

        if (!isValidUrl(serviceUrl)) {
            log.warn("FSU 服务地址格式无效: fsuCode={}, url={}", fsuCode, serviceUrl);
            return FsuEndpointResult.fail(fsuCode, "5001", "FSU 服务地址格式无效");
        }

        log.debug("FSU endpoint 解析成功: fsuCode={}, url={}", fsuCode, serviceUrl);
        return FsuEndpointResult.fromHostPort(fsuCode, serviceUrl);
    }

    /**
     * 根据 IP 和端口构造 FSUService URL。
     */
    public static String buildServiceUrl(String ipAddr, int port) {
        return "http://" + ipAddr + ":" + port + DEFAULT_ENDPOINT_PATH;
    }

    /**
     * 校验 URL 格式是否合法。
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.trim().toLowerCase();
        return (lower.startsWith("http://") || lower.startsWith("https://"))
                && lower.length() > 10;
    }
}
