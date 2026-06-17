# 审计：LANDING-015-FIX-002 GET_FSUINFO 请求格式对齐

## 1. 日期
2026-05-21

## 2. 根因

LANDING-015 Service 的 Info XML 格式与 LANDING-006 成功格式不一致：

| 项目 | LANDING-006 (成功) | LANDING-015 (失败) |
|------|-------------------|-------------------|
| Info XML | `<FSUCode>51051243812345</FSUCode>` | `<FsuId>xxx</FsuId><FsuCode>xxx</FsuCode>` |
| 标签数 | 1 (FSUCode) | 2 (FsuId + FsuCode) |
| 大小写 | FSUCode (大写) | FsuCode (驼峰) |

## 3. 修复

1. `BInterface2016GetFsuInfoService.java`: Info XML 改为 `<FSUCode>suid</FSUCode>`
2. `BInterface2016GetFsuInfoResult.java`: fail() 新增 realDeviceAccessed 重载
3. 测试断言更新为对齐 LANDING-006 格式

## 4. 测试

1216 tests, 0/0/6.
