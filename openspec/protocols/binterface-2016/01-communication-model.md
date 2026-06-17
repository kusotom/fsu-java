# 通信模型

## 1. 来源

| 项 | 内容 |
|---|---|
| 主源文件 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx` |
| 辅助同版文档 | `/home/tom/桌面/FSU/B接口协议/B接口协议2016.docx` |
| WSDL辅助文件 | `/home/tom/桌面/FSU/WSDL协议/SCService.wsdl`；`/home/tom/桌面/FSU/WSDL协议/FSUService.wsdl` |
| 主源SHA256 | `eb049933533db97761aa1514cca449dc6f143a686c1425ba4ff087eb38574c1b` |
| 辅助docx SHA256 | `d481a1f0b3d695b3a875b75976490319c4db4278b599b0f2191c5601b3b71bbf` |
| 抽取稿 | `/tmp/binterface2016_docx_extract.md`，3297行，仅作定位索引 |


## 2. SPEC 规则

### [SPEC-2016-COMM-001] WebService + FTP 双方式互联

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L168-L201，第5.1节  
原文摘要：FSU与SC之间通过WebService和FTP方式互联，二者同时形成完整B接口协议标准。  
规则内容：WebService承载实时/配置/状态类XML报文；FTP承载慢数据里的视频图像文件。  
适用范围：通信架构、FTP能力、文件采集。  
影响模块：SCService、FSUService、FTP客户端/服务配置。  
实现要求：不能用私有JSON/HTTP接口替代B接口主链路。  
测试要求：SOAP和FTP分别建立fixture/集成验证。  
备注：FTP文件规则见 `00-overview` 和 `matrices/not-implemented-but-specified.md`。

### [SPEC-2016-COMM-002] 慢数据方向

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L186-L190，第5.1接入双方要求  
原文摘要：SC轮询FSU获取温湿度、电压、电流、电量、频率、开关状态等慢数据，此时FSU为服务端，SC为客户端。  
规则内容：GET_DATA、GET_HISDATA、GET_THRESHOLD、GET_LOGININFO、GET_FTP、GET_FSUINFO等SC发起命令属于SC->FSU方向。  
适用范围：出站SOAP客户端。  
影响模块：FsuServiceClient、RealHttpFsuServiceClient、run-once测试。  
实现要求：请求URL必须指向FSUService；真实调用默认关闭。  
测试要求：出站请求必须验证PK_Type Name+Code、Info层级和raw request保存。  
备注：SET类虽然同方向，但默认安全禁用。

### [SPEC-2016-COMM-003] 快数据方向

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L198-L201，第5.1接入双方要求  
原文摘要：FSU主动上报设备事件数据（告警、状态切换等），FSU为客户端，SC为服务端。  
规则内容：LOGIN、LOGOUT、SEND_ALARM由FSU向SC发起；SC返回ACK。  
适用范围：入站SCService。  
影响模块：SCService Controller/Processor、CommandDispatcher、报文日志。  
实现要求：入站服务必须保存raw message并返回标准ACK结构。  
测试要求：入站fixture需覆盖LOGIN/SEND_ALARM/LOGOUT。  
备注：原文未定义HEARTBEAT/SEND_DATA命令码；它们只能作为工程/兼容项单独标注。

### [SPEC-2016-COMM-004] 连接建立和重连

规则类型：协议规定  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L445-L479，连接建立过程  
原文摘要：FSU先建立4G/3G和IPSec/L2TP隧道，随后LOGIN/LOGIN_ACK；意外中断后必须重新连接和注册；LOGOUT成功后FSU主动拆除隧道。  
规则内容：LOGIN成功是后续B接口通信前置条件；断链后不能复用旧注册态。  
适用范围：会话、在线状态、离线检测。  
影响模块：LoginService、Session/Status实体、OfflineDetection。  
实现要求：会话状态不得仅凭HTTP可达推断；需以LOGIN/LOGOUT/心跳轮询综合判断。  
测试要求：断链、重复注册、LOGOUT状态转换应有测试。  
备注：两次注册最小间隔见LOGIN命令。

### [SPEC-2016-COMM-005] 快慢数据通道分类

规则类型：协议规定 + 实现分类  
协议来源：/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx；抽取稿 L186-L201，第5.1接入双方要求；`matrices/fast-slow-data-matrix.md`  
原文摘要：SC轮询FSU获取温湿度、电压、电流、电量、频率、开关状态等慢数据；FSU主动上报告警、状态切换等设备事件数据为快数据。  
规则内容：`GET_DATA`、`GET_HISDATA`、`GET_THRESHOLD`、`GET_LOGININFO`、`GET_FTP`、`GET_FSUINFO` 等 SC -> FSU 查询命令归属慢通道；`SEND_ALARM` 等 FSU -> SC 主动事件上报归属快通道；`LOGIN/LOGOUT` 为注册/会话命令，不按测点数据分类，但位于 FSU -> SC 入站方向。  
适用范围：命令分类、服务拆分、调度频率、run-once测试和前端展示。  
影响模块：SCService、FSUService、ReadOnlyRunOnce、Scheduler、message_log分析。  
实现要求：代码和文档应显式标注命令快/慢类别；不得把 GET_DATA 误归为 FSU 主动快数据，也不得把 SEND_ALARM 当作 SC 轮询慢数据。  
测试要求：命令矩阵和测试覆盖矩阵应保留快/慢分类；真实FSU测试必须标注通信方向。  
备注：`GET_DATA` 虽然读取实时测点值，但协议通信模型属于 SC 轮询 FSU 的慢数据查询通道。
