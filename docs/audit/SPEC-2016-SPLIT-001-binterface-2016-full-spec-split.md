# 审计：SPEC-2016-SPLIT-001 B接口2016全规范拆分

## 1. 日期
2026-05-21

## 2. 审计类型
协议规范化（纯文档生成）

## 3. 来源

`docs/protocol/b-interface-2016-handbook.md` (1516行, 14章)

## 4. 输出

37 个文件, 6 个目录: `openspec/protocols/binterface-2016/`

### 文件分布

| 目录 | 文件数 | 内容 |
|------|--------|------|
| 根 | 15 | README + 00~13 + audit-template |
| commands/ | 15 | LOGIN~SET_FSUREBOOT 完整规范 |
| profiles/ | 3 | standard-2016, emerson-2016, future-2024 |
| matrices/ | 5 | code/field/xml/实现/测试 矩阵 |

## 5. 未明确协议点

- GET_FSUINFO xmlData 结构: 协议手册标注"待确认"
- GET_HISDATA 详细定义: 协议手册仅有目录提及
- FSU 自动升级: 协议手册未包含专门章节
- SET_LOGININFO 具体字段: 协议原文待确认

## 6. Java 修改

0 修改。纯文档任务。
