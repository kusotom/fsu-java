package com.dcim.platform.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * BE-AUTH-P0-001: 数据范围过滤服务 (最小闭环).
 *
 * 当前 entity 层不含 tenantId，数据范围通过内存过滤实现。
 * admin/super_admin/platform_admin 角色不限制范围 (getAllowedFsuCodes 返回 null 表示全部可见)。
 * 无 scope 的普通用户看不到任何数据 (返回空集合)。
 *
 * TODO: 后续为 entity 加 tenantId/stationId 列，改为 SQL WHERE 子句过滤。
 */
@Service
public class DataScopeService {

    private static final Logger log = LoggerFactory.getLogger(DataScopeService.class);

    /**
     * @return 当前用户可见的 fsuCode 集合, null 表示全部可见 (admin), 空集合表示无权限查看任何 FSU
     */
    public Set<String> getAllowedFsuCodes() {
        RequestContext ctx = RequestContext.getCurrent();
        if (ctx == null) return Collections.emptySet();
        if (ctx.isAdminLike()) return null; // null = 不限制
        return ctx.getFsuScope();
    }

    /**
     * @return 当前用户可见的 stationId 集合, null 表示全部可见
     */
    public Set<Long> getAllowedStationIds() {
        RequestContext ctx = RequestContext.getCurrent();
        if (ctx == null) return Collections.emptySet();
        if (ctx.isAdminLike()) return null;
        return ctx.getStationScope();
    }

    /**
     * 按 fsuScope 过滤列表.
     * @param data 待过滤数据
     * @param fsuCodeExtractor 从数据项提取 fsuCode
     * @param <T> 数据类型
     * @return 过滤后的列表. 如果 admin 角色返回原列表.
     */
    public <T> List<T> filterByFsuScope(List<T> data, Function<T, String> fsuCodeExtractor) {
        Set<String> allowed = getAllowedFsuCodes();
        if (allowed == null) return data; // admin = 全部可见
        if (allowed.isEmpty()) return Collections.emptyList(); // 无 scope
        return data.stream()
                .filter(item -> {
                    String fsuCode = fsuCodeExtractor.apply(item);
                    return fsuCode != null && allowed.contains(fsuCode);
                })
                .collect(Collectors.toList());
    }

    /**
     * 按 stationScope 过滤列表.
     */
    public <T> List<T> filterByStationScope(List<T> data, Function<T, Long> stationIdExtractor) {
        Set<Long> allowed = getAllowedStationIds();
        if (allowed == null) return data;
        if (allowed.isEmpty()) return Collections.emptyList();
        return data.stream()
                .filter(item -> {
                    Long stationId = stationIdExtractor.apply(item);
                    return stationId != null && allowed.contains(stationId);
                })
                .collect(Collectors.toList());
    }
}
