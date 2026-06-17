# 09 — FTP 接口能力

## 1. 协议定位

B接口2016 包含 FTP 文件/图片传输能力。FSU 可将告警图片、历史数据文件等通过 FTP 上传到 SC 指定的服务器。

## 2. 相关命令

| 命令 | Code | 方向 | 说明 |
|------|------|------|------|
| GET_FTP | 1601 | SC→FSU | 获取 FSU 当前 FTP 配置 |
| SET_FTP | 1603 | SC→FSU | 设置 FSU 的 FTP 服务器参数 |

## 3. FTP 配置结构

```xml
<FTPConfig>
  <Host>192.168.1.200</Host>
  <Port>21</Port>
  <Username>fsu_ftp</Username>
  <Password>encrypted_password</Password>
  <PassiveMode>true</PassiveMode>
  <BasePath>/fsu/images/</BasePath>
</FTPConfig>
```

## 4. 当前项目实现状态

| 项目 | 状态 |
|------|------|
| GET_FTP Service | ✅ 已实现 (`GetFtpService`) |
| GET_FTP 真实联调 | ✅ LANDING-006 验证通过 |
| SET_FTP | ❌ 未实现 |

## 5. 安全注意事项

- FTP 密码在 GET_FTP 响应中以明文返回（Emerson FSU 实测）
- 日志和报告不应包含 FTP 密码
- SET_FTP 属于 SET 类命令，需安全门禁
