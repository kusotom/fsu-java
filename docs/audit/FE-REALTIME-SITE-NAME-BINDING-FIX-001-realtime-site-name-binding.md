# FE-REALTIME-SITE-NAME-BINDING-FIX-001 实时数据页绑定站点列表名称

**日期**: 2026-06-03 | **类型**: 前后端联合修复

## 执行摘要

FE-REALTIME-SITE-NAME-BINDING-FIX-001 完成。

## 1. 为什么显示 FSU-1/FSU-2

前端 `monitorAdapters.ts` 中 `fsuCode` 原逻辑: `row.fsuCode || row.fsuId && 'FSU-${row.fsuId}'`。当 DTO 缺少 `stationName` 时, `fsuPointName` fallback 链最终落到 `fsuCode` 显示为 `FSU-1`。

## 2. 站点列表字段

SiteEntity: `siteName = '朝阳区户外柜站点A'`, `siteCode = 'SITE-001'`。站点列表页面使用 `siteName` 显示。

## 3. 实时数据接口是否返回站点名称

**修复后已返回**。`RealtimeDataService.toDto()` 通过 `siteNameByFsuId()` 查找 `SiteEntity.siteName` 并填充 `dto.stationName`。

## 4. 修复方式

**后端**: `RealtimePointDto` +`stationName` +`siteName`, `RealtimeDataService` 注入 `SiteRepository`, `siteNameByFsuId()` 构建 `fsuId → siteName` 映射.

**前端**: `fsuPointName` 优先级: `stationName > siteName > fsuName > fsuPointName > fsuCode > '未知站点'`. 移除 `FSU-${id}` 生成.

## 5. 修改文件

| 文件 | 操作 |
|------|------|
| `BInterfaceFrontendDtos.java` | RealtimePointDto +stationName +siteName |
| `RealtimeDataService.java` | SiteRepository注入 + siteNameByFsuId() |
| `RealtimeDataServiceMappingClassificationTest.java` | mock SiteRepository适配 |
| `monitorAdapters.ts` | fsuPointName优先级调整 + 移除FSU-${id}生成 |

## 6. 页面修复后显示

第一列显示 `朝阳区户外柜站点A` (来自 SiteEntity.siteName), 相同站点仍合并展示。

## 7. 相同站点合并

span-method 按 `fsuPointName` 合并, 修复后按站点名称合并。

## 8. DataScope

站点名称通过已有的 `FsuDeviceRepository` + `SiteRepository` 查询, 不绕过 DataScope。三方用户通过 `DataScopeService.filterByFsuScope()` 过滤实时数据行, 站点名称自然只对授权数据可见。

## 9. 验证

- 后端编译: 通过
- 后端 RealtimeData 测试: 3/3 通过
- 前端 `npm run build`: 通过

## 10. 遗留问题

无。站点名称来源已修正为 `SiteEntity.siteName`, 前后端链路完整。
