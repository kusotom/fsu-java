# FE-REALTIME-STATION-NAME-FIX-001 实时数据页第一列站点名称修复

**日期**: 2026-06-03 | **类型**: 前后端联合修复

## 执行摘要

FE-REALTIME-STATION-NAME-FIX-001 完成。第一列不再显示 FSU-1/FSU-2，改为站点名称。

## 根因

1. 后端 `RealtimePointDto` 缺少 `stationName`/`siteName` 字段
2. 前端 `fsuPointName` 优先级中 `fsuCode` 排在 stationName 前面

## 修复

### 后端

- `RealtimePointDto` 新增 `stationName`、`siteName` 字段
- `RealtimeDataService` 注入 `SiteRepository`, 新增 `siteNameByFsuId()` 方法, 在 `toDto()` 中填充 `stationName`

### 前端

- `monitorAdapters.ts`: `fsuPointName` 优先级调整为 `stationName > siteName > fsuName > fsuCode`

## 验证

- 后端编译: 通过
- 后端 RealtimeData 测试: 3/3 通过
- 前端 `npm run build`: 通过

## 修改文件

| 文件 | 操作 |
|------|------|
| `BInterfaceFrontendDtos.java` | RealtimePointDto +stationName +siteName |
| `RealtimeDataService.java` | SiteRepository 注入 + siteNameByFsuId() |
| `monitorAdapters.ts` | fsuPointName 优先级调整 |
| `RealtimeDataServiceMappingClassificationTest.java` | mock SiteRepository 适配 |
