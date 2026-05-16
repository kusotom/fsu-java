# BIF-P4-013：GET_SUFTP 查询闭环

> 基于 BIF-P4-010/011/012
> 完成日期：2026-05-15

---

## 一、本阶段目标
实现 2024 标准 FTP 参数查询命令 GET_SUFTP (Code=801)。

## 二、协议依据

| 字段 | GET_SUFTP | GET_SUFTP_ACK |
|------|-----------|---------------|
| Code | 801 | 802 |
| 方向 | SC→FSU | FSU→SC |
| Info | SUID | SUID, Result, FailureCode |
| xmlData | (空) | SUFTPConfig(UserName, Password, FTPPort) |

## 三、实现文件

| 文件 | 说明 |
|------|------|
| GetSuFtpResult | 结果模型, Password脱敏 |
| GetSuFtpService | 查询服务 |
| GetSuFtpCommandHandler | 命令处理器 |
| BInterfacePkType | +GET_SUFTP (19 values) |
| StubFsuServiceClient | +GET_SUFTP stub + 内置响应 |

## 四、脱敏策略
- toString() 不输出明文 Password/UserName
- getMaskedPassword/getMaskedUserName 提供脱敏输出
- 日志仅输出脱敏值

## 五、与 GET_FTP 关系
- GET_FTP 旧路径保留兼容
- GET_FTP alias→GET_SUFTP via BInterfaceCommandAliasMapper
- GET_SUFTP 为 2024 主路径

## 六、测试
GetSuFtpServiceTest: 11 tests (SUID/UserName/Password/FTPPort解析, toString脱敏, alias映射)

## 七、全量回归
934 tests, 0 failures
