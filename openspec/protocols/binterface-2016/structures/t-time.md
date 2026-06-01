# TTime

## 1. 来源

| 项 | 内容 |
|---|---|
| 源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 原章节 | 数据结构定义 / TIME_CHECK |
| 原文位置 | 表8 数据结构定义，抽取稿 L861-L867；TIME_CHECK样例 L2878-L2938 |

## 2. 结构用途

TTime 是时间结构，用于 TIME_CHECK 请求中向 FSU 下发本机时间。

## 3. 字段定义

| 字段 | 类型 | 必填 | 说明 | 所属命令 | SPEC 编号 | 备注 |
|---|---|---|---|---|---|---|
| Years | short | 是 | 年 | TIME_CHECK | SPEC-2016-STRUCT-TTIME-001 |  |
| Month | char | 是 | 月 | TIME_CHECK | SPEC-2016-STRUCT-TTIME-002 |  |
| Day | char | 是 | 日 | TIME_CHECK | SPEC-2016-STRUCT-TTIME-003 |  |
| Hour | char | 是 | 时 | TIME_CHECK | SPEC-2016-STRUCT-TTIME-004 | 24小时制 |
| Minute | char | 是 | 分 | TIME_CHECK | SPEC-2016-STRUCT-TTIME-005 |  |
| Second | char | 是 | 秒 | TIME_CHECK | SPEC-2016-STRUCT-TTIME-006 |  |

## 3.1 SPEC 规则明细

### [SPEC-2016-STRUCT-TTIME-001] Years

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L867  
原文摘要：TTime时间结构包含Years字段。  
规则内容：Years类型为short，表示年。  
适用范围：TIME_CHECK。  
影响模块：TIME_CHECK XML构造与解析。  
实现要求：不得用字符串时间替代TTime结构。  
测试要求：TIME_CHECK fixture覆盖Years。  
备注：无。

### [SPEC-2016-STRUCT-TTIME-002] Month

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L867  
原文摘要：TTime时间结构包含Month字段。  
规则内容：Month类型为char，表示月。  
适用范围：TIME_CHECK。  
影响模块：TIME_CHECK XML构造与解析。  
实现要求：按TTime结构输出。  
测试要求：TIME_CHECK fixture覆盖Month。  
备注：无。

### [SPEC-2016-STRUCT-TTIME-003] Day

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L867  
原文摘要：TTime时间结构包含Day字段。  
规则内容：Day类型为char，表示日。  
适用范围：TIME_CHECK。  
影响模块：TIME_CHECK XML构造与解析。  
实现要求：按TTime结构输出。  
测试要求：TIME_CHECK fixture覆盖Day。  
备注：无。

### [SPEC-2016-STRUCT-TTIME-004] Hour

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L867  
原文摘要：TTime时间结构包含Hour字段。  
规则内容：Hour类型为char，表示时。  
适用范围：TIME_CHECK。  
影响模块：TIME_CHECK XML构造与解析。  
实现要求：按TTime结构输出。  
测试要求：TIME_CHECK fixture覆盖Hour。  
备注：结合原文时间说明，应按24小时制处理。

### [SPEC-2016-STRUCT-TTIME-005] Minute

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L867  
原文摘要：TTime时间结构包含Minute字段。  
规则内容：Minute类型为char，表示分。  
适用范围：TIME_CHECK。  
影响模块：TIME_CHECK XML构造与解析。  
实现要求：按TTime结构输出。  
测试要求：TIME_CHECK fixture覆盖Minute。  
备注：无。

### [SPEC-2016-STRUCT-TTIME-006] Second

规则类型：协议规定  
协议来源：`/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；表8 数据结构定义，抽取稿 L861-L867  
原文摘要：TTime时间结构包含Second字段。  
规则内容：Second类型为char，表示秒。  
适用范围：TIME_CHECK。  
影响模块：TIME_CHECK XML构造与解析。  
实现要求：按TTime结构输出。  
测试要求：TIME_CHECK fixture覆盖Second。  
备注：无。

## 4. XML 示例

```xml
<Time>
  <Years/>
  <Month/>
  <Day/>
  <Hour/>
  <Minute/>
  <Second/>
</Time>
```

## 5. 平台映射

| 协议字段 | 平台字段 | 表 / DTO / Entity | 备注 |
|---|---|---|---|
| Years | years | 待代码审计确认 | TIME_CHECK |
| Month | month | 待代码审计确认 | TIME_CHECK |
| Day | day | 待代码审计确认 | TIME_CHECK |
| Hour | hour | 待代码审计确认 | TIME_CHECK |
| Minute | minute | 待代码审计确认 | TIME_CHECK |
| Second | second | 待代码审计确认 | TIME_CHECK |

## 6. 不明确事项

原文未说明时区，当前不得擅自写成本地时区或UTC强规则。

## 7. 测试要求

TIME_CHECK测试必须覆盖TTime完整字段、非法日期、越界时分秒和raw request保存。
