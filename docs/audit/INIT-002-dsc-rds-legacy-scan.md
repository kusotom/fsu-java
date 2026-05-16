# INIT-002 DSC/RDS 遗留内容扫描报告

## 扫描范围

全项目 fsu-platform-java，以及相邻项目 fsu-python、B接口协议/、WSDL协议/ 中的潜在 DSC/RDS 遗留。

---

## 一、fsu-platform-java（本项目）

### 1.1 扫描结论

**本项目（fsu-platform-java）不存在任何 DSC/RDS 实现代码。**

项目中所有包含 "rds" 或 "dsc" 字符串的文件，经逐文件确认均为以下两类：

### 1.2 误报说明（单词 "records" 匹配）

以下文件因方法名/类名包含 "Record" 而被匹配，**并非 DSC/RDS 引用**：

| 文件 | 匹配内容 | 实际含义 |
|------|---------|---------|
| `backend/.../alarm/controller/AlarmRecordController.java` | `AlarmRecordService` | 告警记录 |
| `backend/.../alarm/service/AlarmRecordService.java` | `AlarmRecordRepository` | 告警记录 |
| `backend/.../binterface/controller/BInterfaceCallRecordController.java` | `call-records` | 调用记录 |
| `backend/.../binterface/controller/FtpTransferRecordController.java` | `ftp-records` | FTP 记录 |
| `backend/.../binterface/soap/SoapMessageHandler.java` | — | 无 DSC/RDS 匹配 |
| `frontend/.../bInterface.ts` | `CallRecords`, `FtpRecords` | API 函数名 |
| `frontend/.../CallRecordView.vue` | `CallRecord` | 页面组件名 |
| `frontend/.../FtpRecordView.vue` | `ftpRecords` | 页面组件名 |
| `frontend/.../DashboardView.vue` | `cards` | 非 "rds" |
| `docs/reports/INIT-003-report.md` | `Records` | 表名/服务名中的 "Record" |
| `docs/tasks/project-working-memory.md` | `Record` | 表名中的 "Record" |

### 1.3 合规声明（明确说明不实现 DSC/RDS）

以下文件包含对 DSC/RDS 的政策性声明，属于**合规表述**：

| 文件 | 内容 | 性质 |
|------|------|------|
| `README.md` | `本项目不优先实现 DSC/RDS、UDP/TCP、MQTT` | 项目政策声明 |
| `docs/tasks/project-working-memory.md` | `不实现...DSC/RDS` | 安全边界说明 |
| `docs/reports/INIT-003-report.md` | `不实现 DSC/RDS` ✅ | 验收检查项 |

### 1.4 结论

| 分类 | 结果 |
|------|------|
| DSC/RDS 主业务代码 | ❌ 不存在 |
| DSC/RDS 引用/依赖 | ❌ 不存在 |
| DSC/RDS 配置 | ❌ 不存在 |
| DSC/RDS 文档内容 | ❌ 不存在 |
| 需从主流程移除的内容 | ❌ 不存在 |
| 需归档的遗留内容 | ❌ 不存在 |
| 合规声明中的 DSC/RDS 提及 | ✅ 3 处（均为政策声明） |

**本项目 DSC/RDS 残留风险：0%**

---

## 二、fsu-python（相邻项目，非本项目范围，仅供参考）

### 2.1 发现

fsu-python 项目中有**大量** DSC/RDS 相关实现内容，包括：

| 类别 | 内容 | 位置 |
|------|------|------|
| 🟡 UDP 实时监听 | DSC UDP 9000 端口监听 | `fsu-platform/backend/` |
| 🟡 UDP 实时监听 | RDS UDP 7000 端口监听 | `fsu-platform/backend/` |
| 🟡 网关逻辑 | DSC/RDS 网关协议实现 | `fsu-platform/backend/` |
| 🟡 协议解析 | DSC_CONFIG 解析逻辑 | `fsu-platform/backend/` |
| 🟡 报文分析 | RAW packet 分析、DSC/RDS annotation | `backend/logs/fsu_raw_packets/` |
| 🟡 脚本工具 | DSC config diff 分析脚本 | `backend/scripts/` |
| 🟡 环境变量 | `FSU_DSC_PORT=9000`, `FSU_RDS_PORT=7000` | 配置/README |
| 🟡 文档 | 大量 DSC/RDS 相关说明 | `README.md`, `docs/` |

### 2.2 风险评估（对本项目）

| 风险 | 等级 | 说明 |
|------|------|------|
| fsu-python 的 DSC/RDS 逻辑被误引入本项目 | 🟢 低 | 项目目录完全独立，无代码共享 |
| fsu-python 的协议分析结论被当作 B接口协议 | 🟢 低 | 协议来源独立，B接口以文档为准 |
| 开发者混淆 DSC/RDS 与 B接口 | 🟡 中 | 需在 AGENTS.md/CLAUDE.md 中持续警示 |

---

## 三、B接口协议/ 和 WSDL协议/ 目录

| 目录 | DSC/RDS 引用 | 结论 |
|------|-------------|------|
| `B接口协议/` | ❌ 不存在 | 纯 B接口协议文档，无 DSC/RDS |
| `WSDL协议/` | ❌ 不存在 | 纯 WSDL 定义，无 DSC/RDS |

---

## 四、AGENTS.md / CLAUDE.md / CODEX.md / PROJECT_ENGINEERING_RULES.md

这 4 个规范文件包含对 DSC/RDS 的**限制性规则**（非实现内容）：

| 文件 | 条款数 | 性质 |
|------|--------|------|
| `AGENTS.md` | 6 处 | 禁止引入、隔离要求、输出模板 |
| `CLAUDE.md` | 3 处 | 启动确认、禁止事项 |
| `CODEX.md` | 4 处 | 启动确认、禁止模式 |
| `PROJECT_ENGINEERING_RULES.md` | 8 处 | 禁止行为、隔离要求、输出模板 |

**这些属于项目规范层面的安全防护措施，不是 DSC/RDS 遗留。**

---

## 五、archive/ / legacy/ / research/ 目录检查

| 目录 | 状态 |
|------|------|
| `archive/` | ❌ 不存在（未创建） |
| `legacy/` | ❌ 不存在（未创建） |
| `research/` | ❌ 不存在（未创建） |
| `docs/legacy/` | ❌ 不存在（未创建） |
| `docs/research/` | ❌ 不存在（未创建） |

**结论：** 本项目中不存在任何需要归档的 DSC/RDS 内容，因此归档目录也未被创建。

---

## 六、最终结论

| 检查项 | 结果 |
|--------|------|
| 本项目是否存在 DSC/RDS 主业务代码 | ❌ 不存在 |
| 本项目是否存在 DSC/RDS 依赖或引用 | ❌ 不存在 |
| 是否需要从主流程移除 DSC/RDS | 不需要 |
| 是否需要创建 archive/legacy/research 目录归 | 目前不需要 |
| 是否需要标记风险项 | 暂无，保持持续监控 |
| 相邻项目 fsu-python 的 DSC/RDS 是否会污染本项目 | 🟢 低风险 |

**建议：** 保持当前安全策略，在 AGENTS.md/CLAUDE.md 中持续保留 DSC/RDS 禁止条款。如果后续需要引用 fsu-python 中的协议分析成果，必须在 docs/research/ 中归档并标注来源，不得直接接入主业务流程。
