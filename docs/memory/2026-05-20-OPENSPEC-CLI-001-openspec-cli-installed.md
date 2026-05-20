# OPENSPEC-CLI-001 OpenSpec CLI 安装与校验

## 1. 任务目标

记录 OpenSpec CLI 安装结果与项目结构校验结果，确认 CLI 能正确识别项目 OpenSpec 结构。

## 2. 架构判断

纯环境/工具记录，不涉及任何业务域、协议层、数据层。

## 3. 协议一致性判断

本次不涉及 B接口协议。

## 4. 修改前论证

**论据**：OPENSPEC-001 已创建项目 OpenSpec 规范文件结构，需确认 CLI 工具可用且能识别该结构。

**方案**：记录 Node.js / npm / openspec 版本信息，执行 `openspec list` / `openspec list --specs` 等校验命令，确认 CLI 与项目结构兼容。

## 5. 写入前验证

- [x] 已确认工作目录：`/home/tom/桌面/FSU/fsu-platform-java/`
- [x] 已排除父目录
- [x] 不修改任何业务代码
- [x] 不执行 `openspec init`

## 6. 实际修改文件

### 新增 (2)

| 文件 | 说明 |
|------|------|
| `docs/memory/2026-05-20-OPENSPEC-CLI-001-openspec-cli-installed.md` | 本记忆文件 |
| `docs/audit/OPENSPEC-CLI-001-openspec-cli-installed.md` | 审计文件 |

### 修改 (2)

| 文件 | 改动 |
|------|------|
| `docs/memory/README.md` | 新增索引 |
| `docs/memory/WORKING-MEMORY.md` | 更新 CLI 状态 |

### 未修改

- openspec/project.md
- openspec/specs / changes / archive
- docs/rules/
- 父目录 docs
- 所有业务代码

## 7. 核心改动

记录 OpenSpec CLI 安装与校验结果。

## 8. CLI 环境信息

| 项目 | 值 |
|------|-----|
| Node.js | v24.15.0 |
| npm | 11.12.1 |
| openspec 路径 | `/home/tom/.npm-global/bin/openspec` |
| openspec 版本 | 1.3.1 |
| 全局安装 | `npm install -g @fission-ai/openspec@latest` |

## 9. CLI 校验命令与结果

| 命令 | 结果 |
|------|------|
| `openspec --version` | 1.3.1 |
| `openspec --help` | 命令列表正常显示 |
| `openspec list` | No active changes found. |
| `openspec list --specs` | No specs found. |
| `openspec list --help` | 帮助正常显示 |
| `openspec validate --help` | 命令可用 |
| `openspec status --help` | 命令可用 |

## 10. CLI 项目结构识别

- CLI 能正确识别当前项目目录下的 `openspec/` 结构
- `openspec list` 正确读取 `openspec/changes/`（当前为空）
- `openspec list --specs` 正确读取 `openspec/specs/`（当前为 .gitkeep）
- **未执行 `openspec init`**（项目已有手动创建的 OpenSpec 结构）

## 11. 测试命令和结果

本次纯文档记录，未运行 Maven 测试。

## 12. 风险

无。

## 13. 遗留问题

1. `openspec/specs/` 为空目录，`openspec list --specs` 返回 "No specs found"
2. `openspec validate` 和 `openspec status` 待后续有 change 文件时验证
3. 后续如 `openspec` CLI 有新版本，可更新

## 14. 下一步建议

1. 后续创建第一个 OpenSpec change 时，验证 `openspec validate` 和 `openspec status` 功能
2. 为已有 B接口命令逐个补充 `openspec/specs/` 规格文件
