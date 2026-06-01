# 安全边界

## 1. 来源

| 项 | 内容 |
|---|---|
| 主源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 辅助同版文档 | `/home/tom/桌面/FSU/B接口协议/B接口协议2016.docx` |
| WSDL辅助文件 | `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl`；`/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl` |
| 主源SHA256 | `eb049933533db97761aa1514cca449dc6f143a686c1425ba4ff087eb38574c1b` |
| 辅助docx SHA256 | `d481a1f0b3d695b3a875b75976490319c4db4278b599b0f2191c5601b3b71bbf` |
| 抽取稿 | `/tmp/binterface2016_docx_extract.md`，3297行，仅作定位索引 |


## 2. 总原则

当前阶段只允许整理规范，不允许实现 SET 业务逻辑，不访问真实 FSU，不启动 Scheduler。

## 3. SPEC 规则

### [SPEC-2016-SECURITY-SET-001] 所有SET类命令默认禁用

规则类型：工程解释  
协议来源：SET_POINT表20-21、SET_LOGININFO表28-29、SET_FTP表32-33、SET_FSUREBOOT表38-39、SET_THRESHOLD表24-25  
原文摘要：协议定义了多类写配置/控制/重启命令。  
规则内容：所有SET类命令在当前项目默认禁用，后续实现必须显式授权、二次确认、操作审计、raw报文保存、禁止默认调度执行、禁止测试环境误触真实设备。  
适用范围：SET_POINT、SET_LOGININFO、SET_FTP、SET_FSUREBOOT、SET_THRESHOLD。  
影响模块：安全门禁、服务层、调度器、真实联调。  
实现要求：默认配置必须false；真实调用必须人工确认和审计。  
测试要求：安全门禁拒绝测试、dry-run测试、审计日志测试。  
备注：TIME_CHECK虽非SET命名但会改FSU时间，应按运维写操作审计。

### [SPEC-2016-SECURITY-SET-POINT-001] SET_POINT高风险

规则类型：工程解释  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L1763-L1951，SET_POINT命令  
原文摘要：客户端发送监控点标识ID和新设置值，服务端设置新设置值并返回成功与否。  
规则内容：SET_POINT属于遥控/遥调控制类高风险命令，默认禁用。  
适用范围：远程控制。  
影响模块：SetPointCommandHandler、安全门禁。  
实现要求：不得默认真实执行。  
测试要求：禁止真实调用测试。

### [SPEC-2016-SECURITY-SET-THRESHOLD-001] SET_THRESHOLD高风险

规则类型：工程解释  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L2161-L2364，SET_THRESHOLD命令  
原文摘要：客户端发送监控点标识ID和新门限数据，服务端设置新门限数据。  
规则内容：SET_THRESHOLD属于修改设备门限类高风险命令，默认禁用。  
适用范围：门限配置。  
影响模块：SetThresholdService、安全门禁。  
实现要求：必须授权、审计、二次确认。  
测试要求：门限写入防误触测试。

### [SPEC-2016-SECURITY-SET-FTP-001] SET_FTP高风险

规则类型：工程解释  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L2762-L2849，SET_FTP命令  
原文摘要：客户端发送设置FTP用户、密码数据，服务端存储并返还成功标志。  
规则内容：SET_FTP属于修改设备配置和敏感凭据类高风险命令，默认禁用。  
适用范围：FTP配置。  
影响模块：SetFtpCommandHandler、安全门禁、日志脱敏。  
实现要求：密码不得明文入日志/报告。  
测试要求：脱敏和禁用测试。

### [SPEC-2016-SECURITY-SET-FSUREBOOT-001] SET_FSUREBOOT高风险

规则类型：工程解释  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L3111-L3208，SET_FSUREBOOT命令；L3245-L3248自动升级  
原文摘要：服务端返回成功标志后重启，用于升级等操作。  
规则内容：SET_FSUREBOOT属于远程重启类高风险命令，默认禁用。  
适用范围：升级、远程重启。  
影响模块：运维入口、安全门禁。  
实现要求：禁止调度器自动触发。  
测试要求：重启命令全路径禁止真实执行测试。
