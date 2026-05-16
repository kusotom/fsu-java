---
name: BIF-P2-002-slow-data-channel-audit
description: 全面审计 12 个慢数据通道命令的实现状态、风险等级、开发优先级，输出优先级矩阵与阶段建议
metadata:
  type: project
---

# BIF-P2-002：慢数据通道（SC→FSU）审计与优先级规划

> 对应阶段：BIF-P2-002
> 完成日期：2026-05-14
> 操作代理：Claude Code
> 影响范围：仅只读审计，不修改业务代码

**关联记忆：** [[BIF-P1-005-main-flow-integration]], [[BIF-P2-001-offline-detection]]

---

## 任务目标

对所有 SC→FSU 方向的慢数据通道命令进行全面审计，评估实现状态、风险等级、开发优先级。

## 规则加载确认

- 已读取 CLAUDE.md / PROJECT_ENGINEERING_RULES.md（位于项目根目录上级）
- 已读取 docs/memory/README.md 及前序审计/记忆
- 已确认本阶段为只读审计，不修改业务代码

## 整体结论

### 实现状态

所有 8 个已注册 Handler（GET_DATA / GET_THRESHOLD / SET_THRESHOLD / SET_POINT / GET_FTP / SET_FTP / GET_LOGININFO / TIME_CHECK）均为纯桩返回 notImplemented()。另有 4 个枚举命令（GET_HISTORY_DATA / GET_FSUINFO / SET_DATA / SET_FSUREBOOT）连 Handler 都未创建。

### 优先级排序

| P0 | GET_DATA | 核心监控轮询 |
| P1 | GET_THRESHOLD / SET_THRESHOLD / TIME_CHECK | 阈值管理 + 时间同步 |
| P2 | GET_LOGININFO / GET_FTP / SET_FTP | 状态查询 + FTP 配置 |
| P3 | SET_POINT / GET_HISTORY_DATA / GET_FSUINFO / SET_DATA | 遥控/历史/信息查询 |
| P4 | SET_FSUREBOOT | 极高风险，需审批流程 |

### 高风险命令

SET_POINT（遥控遥调）、SET_FSUREBOOT（远程重启）、SET_FTP（网络配置）、SET_THRESHOLD（阈值修改）必须在有二次确认、操作审计、鉴权门禁的前提下实现。

## 架构判断

慢数据通道命令在 SC 侧涉及两个角色：CommandDispatcher 处理入站（FSU 响应），FsuServiceClient 处理出站（SC→FSU 请求）。当前 FsuServiceClient 为全桩，需实现真实 HTTP SOAP 客户端。

## 协议一致性判断

严格基于 B接口协议 2016 规范分析，所有结论均未超出协议定义的命令集和字段范围。

## 实际新增/修改文件

### 新增文件
| 文件 | 说明 |
|------|------|
| `docs/audit/BIF-P2-002-slow-data-channel-audit.md` | 完整审计文档（优先级矩阵/风险分类/Fixture 覆盖/架构分析） |
| `docs/memory/2026-05-14-BIF-P2-002-slow-data-channel-audit.md` | 操作记忆 |

## 核心结论

1. **GET_DATA**（P0）是下一个最有价值的实现目标，但需先完成 FsuServiceClient HTTP 客户端
2. **GET_LOGININFO** 和 **TIME_CHECK** 可在不依赖 FSU 通信的前提下实现服务端逻辑
3. 4 个命令（SET_POINT / SET_FSUREBOOT / SET_FTP / SET_THRESHOLD）因风险等级必须有二次确认门禁
4. Fixtures 覆盖良好（8 个命令 xmldata + soap 均完备），但 4 个命令无任何 fixture
5. FSU 地址管理是隐式前提，当前 FsuDeviceEntity 可能缺少 endpoint 字段

## 建议下一步

BIF-P3-001：慢数据通道基础设施 — FsuServiceClient HTTP 客户端 + FSU 地址管理 + GET_DATA 首命令闭环

## 是否修改业务代码

否。本阶段为只读审计。

## 是否访问真实设备

否。

## 是否涉及 DSC/RDS

否。全程基于 B接口协议 2016 分析。
