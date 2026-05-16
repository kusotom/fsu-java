package com.dcim.platform.module.binterface.service.fsu;

/**
 * FSU endpoint 解析器。
 *
 * <p>根据 FSU 编码解析 FSUService 的完整 URL。
 * 实现类负责从数据库、配置或其他来源获取 FSU 地址信息。</p>
 */
public interface FsuEndpointResolver {

    /**
     * 根据 FSU 编码解析 FSUService endpoint URL。
     *
     * @param fsuCode FSU 编码，不可为空
     * @return 解析结果，包含 success/serviceUrl/resultCode/resultDesc
     */
    FsuEndpointResult resolve(String fsuCode);
}
