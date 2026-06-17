# XML 样例索引

| 样例编号 | 所属命令 | 请求/响应 | 原文位置 | 样例文件 | 是否已落为 fixture | 备注 |
|---|---|---|---|---|---|---|
| XML-2016-001 | LOGIN | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L974-L1071 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-002 | LOGIN_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1087-L1123 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-003 | LOGOUT | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1138-L1165 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-004 | LOGOUT_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1178-L1205 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-005 | SEND_ALARM | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1226-L1335 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-006 | SEND_ALARM_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1353-L1382 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-007 | GET_DATA | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1405-L1469 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-008 | GET_DATA_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1492-L1558 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-031 | GET_DATA | 真实请求 | BIF2016-CONNECTION-007 | `docs/landing/raw-samples/bif2016-connection-007-get-data-all-devices-request-info.xml` | 否 | Emerson实测；显式 `<Id>` request，不含 `<TSemaphore>` |
| XML-2016-032 | GET_DATA_ACK | 真实响应 | BIF2016-CONNECTION-007 | `docs/landing/raw-samples/bif2016-connection-007-get-data-all-devices-unwrapped.xml` | 否 | Emerson实测；ACK=402 Result=1，返回 8 个 TSemaphore |
| XML-2016-033 | GET_DATA | 真实请求 | BIF2016-CONNECTION-007 | `docs/landing/raw-samples/bif2016-connection-007-get-data-single-device-request-info.xml` | 否 | Emerson实测；单设备显式 `<Id>` request |
| XML-2016-034 | GET_DATA_ACK | 真实响应 | BIF2016-CONNECTION-007 | `docs/landing/raw-samples/bif2016-connection-007-get-data-single-device-unwrapped.xml` | 否 | Emerson实测；ACK=402 Result=1，返回 2 个 TSemaphore |
| XML-2016-035 | GET_DATA | 真实请求 | BIF2016-CONNECTION-007 | `docs/landing/raw-samples/bif2016-connection-007-get-data-minimal-request-info.xml` | 否 | Emerson实测；全 9 通配请求，响应读取超时 |
| XML-2016-036 | GET_DATA | 真实SOAP请求 | DATA-MAPPING-008-RETRY | `docs/landing/raw-samples/data-mapping-008-retry-get-data-request.xml` | 否 | Emerson实测；`soap:Envelope` + `xmlData xsi:type="xsd:string"` + escaped `<Request>`；请求2个Id |
| XML-2016-037 | GET_DATA_ACK | 真实SOAP响应 | DATA-MAPPING-008-RETRY | `docs/landing/raw-samples/data-mapping-008-retry-get-data-response.xml` | 否 | Emerson实测；响应为 `SOAP-ENV:Envelope` + escaped `invokeReturn`；`0407102001=54.2`、`0407107001=0.0` |
| XML-2016-038 | GET_DATA | 正式API请求留痕 | DATA-MAPPING-008 | `docs/landing/raw-samples/data-mapping-008-formal-api-get-data-request-20260527-232436.xml` | 否 | 正式API request侧留痕；FSU当时超时无响应 |
| XML-2016-039 | GET_DATA | 请求样本缺失记录 | BIF2016-RPCXML-001 | `docs/landing/raw-samples/bif2016-rpcxml-001-formal-api-get-data-request.xml` | 否 | 用户指定路径当前不存在；以 XML-2016-036/038 替代引用 |
| XML-2016-040 | GET_DATA_ACK | 响应样本缺失记录 | BIF2016-RPCXML-001 | `docs/landing/raw-samples/bif2016-rpcxml-001-formal-api-get-data-response.xml` | 否 | 用户指定路径当前不存在；以 XML-2016-037 替代引用 |
| XML-2016-009 | GET_HISDATA | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1588-L1655 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-010 | GET_HISDATA_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1681-L1748 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-011 | SET_POINT | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1775-L1840 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-012 | SET_POINT_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1865-L1951 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-013 | GET_THRESHOLD | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L1991-L2064 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-014 | GET_THRESHOLD_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2079-L2145 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-015 | SET_THRESHOLD | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2173-L2237 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-016 | SET_THRESHOLD_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2262-L2364 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-017 | GET_LOGININFO | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2384-L2417 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-018 | GET_LOGININFO_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2439-L2500 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-019 | SET_LOGININFO | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2527-L2568 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-020 | SET_LOGININFO_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2604-L2640 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-021 | GET_FTP | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2659-L2692 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-022 | GET_FTP_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2711-L2737 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-023 | SET_FTP | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2774-L2807 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-024 | SET_FTP_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2833-L2849 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-025 | TIME_CHECK | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2887-L2938 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-026 | TIME_CHECK_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L2953-L2983 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-027 | GET_FSUINFO | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L3002-L3035 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-028 | GET_FSUINFO_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L3053-L3102 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-029 | SET_FSUREBOOT | 请求 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L3121-L3154 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
| XML-2016-030 | SET_FSUREBOOT_ACK | 响应 | `/home/tom/桌面/中国铁塔动环监控系统_统一互联B接口技术规范_分页排版版.docx`；抽取稿 L3171-L3208 | 原文内联样例 | 否 | 后续应建立fixture并保留原始大小写/拼写 |
