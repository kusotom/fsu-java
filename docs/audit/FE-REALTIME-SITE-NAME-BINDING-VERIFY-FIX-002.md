# FE-REALTIME-SITE-NAME-BINDING-VERIFY-FIX-002 修复第一列显示"-"问题

**日期**: 2026-06-03 | **类型**: 前端适配器鲁棒性修复

## 问题

`FSU/点位` 列显示 `-`。

## 根因

前端 `fsuCode = firstText(row.fsuCode, row.fsu_code) || '-'` — 当后端未返回 fsuCode 时（如 FSU 设备表未播种），fallback 为 '-'。

## 修复

`monitorAdapters.ts` fsuPointName fallback 链:

```
stationName > siteName > fsuName > fsuPointName > fsuCode > 站点${fsuId} > '未知站点'
```

1. `fsuCode` 不再硬编码 `'-'` 作为 fallback
2. `fsuId` 作为倒数第二兜底，格式化为 `站点 N`
3. `'未知站点'` 作为最终兜底

## 后端已验证

`RealtimeDataService.toDto()` 通过 `siteNameByFsuId()` 填充 `dto.stationName`。种子数据 (SiteEntity.siteName="朝阳区户外柜站点A") 经 FsuDeviceEntity.siteId 正确关联。

## 验证

- 后端 RealtimeData 测试: 3/3 通过
- `npm run build`: 通过

## 修改

`frontend/src/utils/monitorAdapters.ts` — fsuPointName fallback 链修正
