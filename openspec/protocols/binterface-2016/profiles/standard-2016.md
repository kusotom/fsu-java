# 标准 B接口2016 Profile

本文件只记录原始B接口2016文档明确说明的内容，不包含Emerson实测差异和工程兼容策略。

## 标准规则摘要

| SPEC编号 | 内容 | 来源 |
|---|---|---|
| SPEC-2016-OVERVIEW-001 | B接口为SC与FSU互联数据传输规范 | 抽取稿 L93-L101 |
| SPEC-2016-COMM-001 | WebService+FTP共同形成完整B接口标准 | 抽取稿 L168-L201 |
| SPEC-2016-MSG-001 | Request/Response下必须含PK_Type和Info | 抽取稿 L223-L234 |
| SPEC-2016-RESULT-001 | Result: FAILURE=0, SUCCESS=1 | 抽取稿 L711-L713 |
| SPEC-2016-PKTYPE-001 | 命令码按报文类型表和XML样例Name+Code | 抽取稿 L894-L931 |
| SPEC-2016-CMD-GET-DATA-001 | GET_DATA=401, GET_DATA_ACK=402 | 抽取稿 L1392-L1558 |
| SPEC-2016-CMD-SEND-ALARM-001 | SEND_ALARM=501, SEND_ALARM_ACK=502 | 抽取稿 L1217-L1382 |
| SPEC-2016-CMD-GET-FSUINFO-001 | GET_FSUINFO=1701, ACK=1702 | 抽取稿 L2992-L3102 |
| SPEC-2016-SECURITY-SET-001 | SET类命令需作为写配置/控制风险处理 | 命令表和项目安全边界工程解释 |

## 标准命令全集

详见 `../matrices/command-code-matrix.md`。注意：本docx标准报文类型表不包含独立HEARTBEAT命令码，SC心跳功能通过“定期获取FSU状态信息”描述，工程上应引用GET_FSUINFO或标注待确认。
