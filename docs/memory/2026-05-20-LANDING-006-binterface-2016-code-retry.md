# LANDING-006 B接口2016码表兼容只读重试

## 任务目标
新增 B接口2016 命令码表，使用 2016 Code 重试真实 FSU 只读查询，确认设备协议版本。

## 架构判断
- 新增: BInterfaceCommand2016 (2016 命令码表)
- 修改: RealHttpFsuServiceClient, Landing003RealFsuIntegration
- 不涉及: SET 安全体系, Scheduler, alarm_record 状态

## 协议一致性判断
- 2016 码表依据 B接口2016 标准: GET_DATA=401, SEND_ALARM=501, GET_LOGININFO=1501, GET_FTP=1601, GET_FSUINFO=1701
- 使用 Name+Code PK_Type 格式 + 2016 码值
- 真实 FSU 返回正确的 ACK 码: GET_LOGININFO_ACK=1502, GET_FTP_ACK=1602, GET_FSUINFO_ACK=1702

## 修改前论证
见 LANDING-005 结论修正: FSU 返回 SEND_ALARM Code=501 符合 2016 码表，需新增 2016 码表验证。

## 新增/修改文件

| 文件 | 操作 | 说明 |
|------|------|------|
| `BInterfaceCommand2016.java` | 新增 | 2016 Name→Code 映射 |
| `RealHttpFsuServiceClient.java` | 修改 | 支持 pkTypeFormat=legacy-2016 |
| `Landing003RealFsuIntegration.java` | 修改 | Phase 3: 2016 Code 重试 |

## 核心突破
使用 2016 Code 后，FSU **首次返回真实数据**:

1. **GET_LOGININFO (1501)**: FsuID=51051243812345 确认, **4 个 DeviceID**:
   - 51051241820004, 51051241830004, 51051241840004, 51051240700002
2. **GET_FSUINFO (1701)**: CPU=14.95%, MEM=62.84%, Result=0
3. **GET_FTP (1601)**: FTP 用户名/密码配置
4. **GET_DATA (401)**: 路由正确 (不再错误到 SEND_ALARM)，但信号 ID 不匹配

## 协议版本确认
**真实 FSU 使用 B接口2016 码表**。证据: 4/4 命令在 2016 Code 下收到正确 ACK，2024 Code 下错误路由或空响应。

## 测试命令和结果
```
mvn test → 1164 tests, 0 failures, 0 errors, 5 skipped
mvn test -Dtest='Landing003RealFsuIntegration' -DrealFsuTest.enabled=true → BUILD SUCCESS
12 次真实调用 (4命令×3格式), 3 个命令首次返回真实数据
```

## 风险
- GET_FTP 返回明文密码 (已记录，未传播)
- 2016 码表当前仅覆盖 7 个命令，需根据后续联调扩展

## 遗留问题
1. GET_DATA (401) 信号 ID 不匹配 — 需用真实 DeviceID 构造查询
2. FSU 仍不支持 GET_THRESHOLD (无 2016 映射)
3. 需补全 2016 码表剩余命令

## 下一步建议
1. 用真实 DeviceID 重试 GET_DATA
2. 基于 2016 码表补全平台命令映射
3. 接入 FSU 上报数据 (SEND_DATA/SEND_ALARM)
4. 提交 Codex 集中复审
