# FE-STATUS-CHINESE-SEMANTIC-FIX-001 前端状态值中文语义化修复

**日期**: 2026-06-03 | **类型**: 前端展示修正

## 执行摘要

FE-STATUS-CHINESE-SEMANTIC-FIX-001 完成。普通业务页面不再裸露英文状态码。

## 修复

`monitorAdapters.ts` 新增 `normalizeBusinessStatusText()`:

| 英文 | 中文 |
|------|------|
| CLOSE / CLOSED | 闭合 / 门磁闭合 |
| OPEN | 打开 / 门磁打开 |
| DRY | 无水浸 |
| WET | 有水浸 |
| NORMAL | 正常 |
| ALARM / ABNORMAL | 告警 |
| ONLINE | 在线 |
| OFFLINE | 离线 |
| ACTIVE | 未恢复 |
| RECOVERED / CLEARED | 已恢复 |
| GOOD | 正常 |
| BAD / POOR | 异常 |

`buildDisplayValueWithUnit` 对文本型值调用该函数翻译后再展示。

## 种子数据验证

| 值 | sensorName | 结果 |
|-----|------------|------|
| CLOSE | 门磁/状态 | **门磁闭合** |
| DRY | 水浸/状态 | **无水浸** |
| 25.5 | 温度 | **25.50 ℃** |

## 验证

- `npm run build`: 通过 (0 errors)

## 修改文件

`frontend/src/utils/monitorAdapters.ts` — normalizeBusinessStatusText + buildDisplayValueWithUnit 适配

## 安全边界

- [x] 不修改后端
- [x] 不访问真实 FSU
