# 标准信号字典索引

## 1. 字典来源

| 文件 | 类型 | 路径 | 记录数 | 状态 |
|---|---|---|---:|---|
| 中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx | 原始 Excel | `/home/tom/桌面/FSU/B接口协议/中国铁塔动环监控设备信号字典表-精简版-20150416.xlsx` | 见抽取 CSV | 已纳入工程索引 |
| standard-signal-dictionary-dry-run.csv | 标准信号字典抽取 | `/home/tom/桌面/FSU/docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv` | 484 | 已全量读取 |
| device-type-dictionary-dry-run.csv | 设备类型码表抽取 | `/home/tom/桌面/FSU/docs/mapping/dry-run/device-type-dictionary-dry-run.csv` | 39 | 已全量读取 |
| dictionary-code-matrix.md | openspec 字典矩阵 | `../matrices/dictionary-code-matrix.md` | 1168 行 | 已同步索引 |

## 2. SPEC-DICT-MAPPING-001 实测 SignalID 索引

| SPEC 编号 | SignalID | 标准点位 | 设备类型 | 信号类型 | 单位 | 字典来源 | 状态 |
|---|---|---|---|---|---|---|---|
| SPEC-2016-DICT-SIGNAL-0418002001 | 0418002001 | 烟雾告警 | 18 机房/基站环境 | 遥信 DI | 无单位 | Excel 行 102 / CSV exact；单位列为空 | DI 0/1编码待复核 |
| SPEC-2016-DICT-SIGNAL-0418004001 | 0418004001 | 温度告警 | 18 机房/基站环境 | 遥信 DI | 无单位 | Excel 行 104 / CSV exact；单位列为空 | 标准signal_name为温度，logic_category为温度告警 |
| SPEC-2016-DICT-SIGNAL-0418007001 | 0418007001 | 湿度告警 | 18 机房/基站环境 | 遥信 DI | 无单位 | Excel 行 107 / CSV exact；单位列为空 | 标准signal_name为湿度，logic_category为湿度告警 |
| SPEC-2016-DICT-SIGNAL-0418101001 | 0418101001 | 环境温度 | 18 机房/基站环境 | 遥测 AI | ℃ | Excel 行 109 / CSV exact | 已纠偏，不是CPU利用率 |
| SPEC-2016-DICT-SIGNAL-0418102001 | 0418102001 | 环境湿度 | 18 机房/基站环境 | 遥测 AI | %RH | Excel 行 110 / CSV exact | 已纠偏，不是内存利用率 |
| SPEC-2016-DICT-SIGNAL-0418001001 | 0418001001 | 水浸告警 | 18 机房/基站环境 | 遥信 DI | 无单位 | Excel 行 101 / CSV exact；单位列为空 | DI 0/1编码待复核 |
| SPEC-2016-DICT-SIGNAL-0407102001 | 0407102001 | 总电压 | 07 蓄电池组 | 遥测 AI | V | Excel 行 67 / CSV exact | 已入库验收 |
| SPEC-2016-DICT-SIGNAL-0407107001 | 0407107001 | 后半组电压 | 07 蓄电池组 | 遥测 AI | 单位待确认 | Excel 行 70 / CSV exact；原始 Excel 与 CSV 单位列均为空 | 单位仍不可补造；已入库验收 |

## 3. SPEC-UNIT-COMPLETE-001 单位核验结论

| SignalID | 字典精确匹配 | 原始 Excel 单位列 | CSV 单位列 | 处理结论 |
|---|---|---|---|---|
| 0418002001 | exact | 空 | 空 | DI 遥信点无工程单位，前端显示“无单位 + DI编码待复核” |
| 0418004001 | exact | 空 | 空 | DI 遥信点无工程单位，前端显示“无单位 + DI编码待复核” |
| 0418007001 | exact | 空 | 空 | DI 遥信点无工程单位，前端显示“无单位 + DI编码待复核” |
| 0418101001 | exact | ℃ | ℃ | AI 单位确认：℃ |
| 0418102001 | exact | %RH | %RH | AI 单位确认：%RH |
| 0418001001 | exact | 空 | 空 | DI 遥信点无工程单位，前端显示“无单位 + DI编码待复核” |
| 0407102001 | exact | V | V | AI 单位确认：V |
| 0407107001 | exact | 空 | 空 | AI 单位仍待确认；不得凭“电压”二字补造为 V |

## 4. 约束

本索引用于 B接口2016 标准字典反查。不得把 Emerson 实测行为写成标准协议，不得混用 B接口2024 字典，不得将 weak_inferred 当作 exact。
