package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.dto.auth.BInterfaceFsuRegistrationContextDto;
import com.dcim.platform.module.binterface.dto.runonce.ReadOnlyRunOnceDtos;
import com.dcim.platform.module.binterface.dto.runonce.ReadOnlyRunOnceDtos.*;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.*;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FAST-REALDATA-001: B接口2016 只读 run-once 服务。
 *
 * 提供人工触发的 GET_FSUINFO / GET_DATA probe，只读，不写正式点位。
 */
@Service
public class BInterface2016ReadOnlyRunOnceService {

    private static final Logger log = LoggerFactory.getLogger(BInterface2016ReadOnlyRunOnceService.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final BInterface2016GetFsuInfoService getFsuInfoService;
    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;
    private final BInterfaceFsuRegistrationContextService registrationContextService;

    @Value("${b-interface.fsu-client.real-call-enabled:false}")
    private boolean realCallEnabled;

    public BInterface2016ReadOnlyRunOnceService(
            BInterface2016GetFsuInfoService getFsuInfoService,
            FsuServiceClient fsuServiceClient,
            FsuEndpointResolver fsuEndpointResolver,
            BInterfaceFsuRegistrationContextService registrationContextService) {
        this.getFsuInfoService = getFsuInfoService;
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
        this.registrationContextService = registrationContextService;
    }

    // ==================== GET_FSUINFO run-once ====================

    public FsuInfoRunOnceResponse runFsuInfoOnce(FsuInfoRunOnceRequest req) {
        validateReadOnly(req.getOperator(), req.getReason(), req.isConfirmReadOnly());

        String fsuCode = req.getFsuCode() != null ? req.getFsuCode().trim() : "51051243812345";
        String readTime = LocalDateTime.now().format(TS_FMT);

        BInterface2016GetFsuInfoResult result = getFsuInfoService.execute(fsuCode, null);

        // Save raw samples
        String rawPath = null;
        if (result.getRawRequest() != null || result.getRawResponse() != null) {
            rawPath = saveRawSample("GET_FSUINFO", fsuCode, result.getRawRequest(), result.getRawResponse());
        }

        FsuInfoRunOnceResponse resp = new FsuInfoRunOnceResponse();
        resp.setSuccess(result.isSuccess());
        resp.setFsuCode(fsuCode);
        resp.setCpuUsage(result.getCpuUsage());
        resp.setMemUsage(result.getMemUsage());
        resp.setRealDeviceAccessed(result.isRealDeviceAccessed());
        resp.setStatusUpdated(result.isStatusUpdated());
        resp.setReadTime(readTime);
        resp.setResultCode(result.getResultCode());
        resp.setResultDesc(result.getResultDesc());
        resp.setRawSamplePath(rawPath);

        return resp;
    }

    // ==================== GET_DATA probe ====================

    public GetDataProbeResponse runGetDataProbe(GetDataProbeRequest req) {
        validateReadOnly(req.getOperator(), req.getReason(), req.isConfirmReadOnly());

        String fsuCode = req.getFsuCode() != null ? req.getFsuCode().trim() : "51051243812345";
        int maxSignals = req.getMaxSignalsPerDevice() > 0 ? req.getMaxSignalsPerDevice() : 30;
        String probeTime = LocalDateTime.now().format(TS_FMT);

        // 0. 决定 targetSource: 优先 LOGIN 注册上下文
        boolean useRegCtx = req.isUseRegistrationContext();
        BInterfaceFsuRegistrationContextDto regCtx = null;
        List<String> deviceIds;
        String fsuIpSource = "static_config";
        String deviceListSource = "static_config";
        String effectiveServiceUrl = null;

        if (useRegCtx) {
            try {
                regCtx = registrationContextService.buildContext(fsuCode);
            } catch (Exception e) {
                log.warn("注册上下文获取失败: fsuCode={}", fsuCode, e);
            }

            if (regCtx == null) {
                return buildRegCtxError(fsuCode, probeTime, null,
                        "REGISTRATION_CONTEXT_MISSING",
                        "FSU registration context unavailable.");
            }

            // MISSING completeness = hard reject
            if ("MISSING".equals(regCtx.getContextCompleteness())) {
                return buildRegCtxError(fsuCode, probeTime, regCtx,
                        "REGISTRATION_CONTEXT_MISSING",
                        "FSU registration context completeness is MISSING.");
            }

            // 核心硬性条件: FsuCode + FsuIP + valid DeviceCapabilities
            // SessionID/UserName/PaSCword 为平台扩展字段, 非 B接口2016 硬性阻塞
            if (regCtx.getFsuIp() == null || regCtx.getFsuIp().trim().isEmpty()) {
                return buildRegCtxError(fsuCode, probeTime, regCtx,
                        "REGISTRATION_CONTEXT_INCOMPLETE",
                        "FsuIP missing from registration context.");
            }

            List<BInterfaceFsuRegistrationContextDto.DeviceCapabilityDto> validCaps = new ArrayList<>();
            if (regCtx.getDeviceCapabilities() != null) {
                for (var cap : regCtx.getDeviceCapabilities()) {
                    if (cap.isValid() && cap.getDeviceId() != null) validCaps.add(cap);
                }
            }
            if (validCaps.isEmpty()) {
                return buildRegCtxError(fsuCode, probeTime, regCtx,
                        "REGISTRATION_CONTEXT_INCOMPLETE",
                        "No valid device capabilities in registration context.");
            }

            // Check online status (if available and explicitly OFFLINE/LOGOUT)
            if ("OFFLINE".equals(regCtx.getOnlineStatus()) || "LOGOUT".equals(regCtx.getLoginStatus())) {
                return buildRegCtxError(fsuCode, probeTime, regCtx,
                        "REGISTRATION_CONTEXT_FSU_OFFLINE",
                        "FSU is OFFLINE/LOGOUT; cannot execute GET_DATA.");
            }

            // Use registration context — FsuIP as host only, URL template from config
            deviceIds = new ArrayList<>();
            for (var cap : validCaps) {
                deviceIds.add(cap.getDeviceId());
            }
            fsuIpSource = "login_registration_context";
            deviceListSource = "login_registration_context";

            log.info("GET_DATA probe: 使用注册上下文, fsuCode={}, fsuIp={}, devices={}, completeness={}",
                    fsuCode, regCtx.getFsuIp(), deviceIds.size(), regCtx.getContextCompleteness());

        } else {
            // Manual probe mode — keep existing behavior
            deviceIds = req.getDeviceIds();
            if (deviceIds == null || deviceIds.isEmpty()) {
                deviceIds = List.of("51051241820004", "51051241830004", "51051241840004",
                        "51051240700002", "51051243812345");
            }
            fsuIpSource = "static_config";
            deviceListSource = "request_manual";
        }

        // 1. Resolve FSU endpoint (needed before registration context for URL template)
        FsuEndpointResult endpoint = fsuEndpointResolver.resolve(fsuCode);
        if (!endpoint.isSuccess()) {
            GetDataProbeResponse err = new GetDataProbeResponse();
            err.setSuccess(false);
            err.setFsuCode(fsuCode);
            err.setResultCode(endpoint.getResultCode());
            err.setResultDesc(endpoint.getResultDesc());
            return err;
        }

        // 1b. Override effectiveServiceUrl for registration context mode
        if (useRegCtx && regCtx != null && regCtx.getFsuIp() != null) {
            effectiveServiceUrl = replaceHostOnly(
                    endpoint.getServiceUrl(), regCtx.getFsuIp().trim());
        }

        // 2. Load signal dictionary
        List<CsvSignalRow> dictionary;
        try {
            dictionary = loadSignalDictionary();
        } catch (Exception e) {
            log.error("无法加载信号字典 CSV", e);
            GetDataProbeResponse err = new GetDataProbeResponse();
            err.setSuccess(false);
            err.setFsuCode(fsuCode);
            err.setResultCode("CSV_LOAD_FAILED");
            err.setResultDesc("无法加载标准信号字典: " + e.getMessage());
            return err;
        }

        // 3. For each DeviceID, infer device_type_code, select signals, track deviceCode
        List<DeviceProbeResult> deviceResults = new ArrayList<>();
        Map<String, List<String>> semaphores = new LinkedHashMap<>();
        Map<String, String> deviceCodes = new LinkedHashMap<>();  // deviceId → deviceCode
        Map<String, Boolean> deviceCodeFallbacks = new LinkedHashMap<>(); // deviceId → fallback flag

        // Build deviceCode map from registration context capabilities (if available)
        if (useRegCtx && regCtx != null && regCtx.getDeviceCapabilities() != null) {
            for (var cap : regCtx.getDeviceCapabilities()) {
                if (cap.isValid() && cap.getDeviceId() != null) {
                    String did = cap.getDeviceId();
                    if (cap.getDeviceCode() != null && !cap.getDeviceCode().trim().isEmpty()) {
                        deviceCodes.put(did, cap.getDeviceCode().trim());
                        deviceCodeFallbacks.put(did, false);
                    } else {
                        deviceCodes.put(did, did);  // fallback
                        deviceCodeFallbacks.put(did, true);
                    }
                }
            }
        }

        for (String deviceId : deviceIds) {
            String typeCode = inferDeviceTypeCode(deviceId);
            String effDeviceCode = deviceCodes.getOrDefault(deviceId, deviceId);
            boolean fallback = deviceCodeFallbacks.getOrDefault(deviceId, true);

            List<CsvSignalRow> filtered = filterByDeviceType(dictionary, typeCode, maxSignals);
            List<String> signalIds = new ArrayList<>();
            DeviceProbeResult dpr = new DeviceProbeResult();
            dpr.setDeviceId(deviceId);
            dpr.setDeviceTypeCode(typeCode);
            dpr.setSignalsRequested(filtered.size());
            // Device.Code fallback tracking
            dpr.setDeviceCodeFallback(fallback);
            if (fallback) {
                dpr.setDeviceCodeFallbackReason(
                    "Device.Code missing in LOGIN DeviceList; fallback to Device.Id");
            }

            for (CsvSignalRow row : filtered) {
                signalIds.add(row.signalId);
                SignalValueResult sv = new SignalValueResult();
                sv.setSignalId(row.signalId);
                sv.setSpid(row.signalId); // 2016: spid = signal_id
                sv.setSignalName(row.signalName);
                sv.setSignalType(row.signalType);
                sv.setUnit(row.unit);
                sv.setQuality("candidate-from-dictionary");
                dpr.getValues().add(sv);
            }

            deviceResults.add(dpr);
            if (!signalIds.isEmpty()) {
                semaphores.put(deviceId, signalIds);
            }
        }

        // 4. Call GET_DATA via FsuServiceClient (don't write DB)
        // Support multiple strategies per B接口2016 protocol analysis
        String strategy = req.getStrategy() != null ? req.getStrategy().trim() : "device-list";
        try {
            String infoXml;
            String xmlDataXml = null;
            String sampleOp = "GET_DATA-probe-" + strategy;

            switch (strategy) {
                case "simple-signalid":
                    // 2016 标准格式: SignalID 列表放在 xmlDataXml
                    infoXml = "<FSUCode>" + escape(fsuCode) + "</FSUCode>";
                    xmlDataXml = buildSimpleSignalIdXml(deviceIds, semaphores);
                    break;

                case "single-device":
                    // 策略A: 单设备 + 单 SignalID
                    {
                        String singleDev = deviceIds.isEmpty() ? "51051241820004" : deviceIds.get(0);
                        List<String> singleSig = new ArrayList<>();
                        if (!semaphores.isEmpty()) {
                            var first = semaphores.values().iterator().next();
                            if (!first.isEmpty()) singleSig.add(first.get(0));
                        }
                        if (singleSig.isEmpty()) singleSig.add("0118001001");
                        infoXml = buildGetDataInfoXml(fsuCode, List.of(singleDev),
                                Map.of(singleDev, singleSig));
                    }
                    break;

                case "signal-all9":
                    // 策略C: 单设备 + TSemaphore.Id = 9999999999 (监控点全9)
                    {
                        String singleDev = deviceIds.isEmpty() ? "51051241820004" : deviceIds.get(0);
                        infoXml = buildGetDataInfoXml(fsuCode, List.of(singleDev),
                                Map.of(singleDev, List.of("9999999999")));
                    }
                    break;

                case "device-code-all9":
                    // 策略D: Device.Code = 999999999999 (设备编码全9, B接口2016长度12)
                    {
                        String singleDev = deviceIds.isEmpty() ? "51051241820004" : deviceIds.get(0);
                        infoXml = buildAll9DeviceCodeXml(fsuCode, singleDev);
                    }
                    break;

                case "id-vs-code":
                    // 策略E: Device.Id 全9 + Device.Code 全9
                    infoXml = buildAll9IdAndCodeXml(fsuCode);
                    break;

                case "per-device-type":
                    // 策略F: 按设备类型单独查询，每个 device_type 一个请求
                    // 此处按 device_type 分组，每组只取第一个 DeviceID
                    infoXml = buildPerDeviceTypeXml(fsuCode, deviceIds, deviceResults);
                    break;

                default: // "device-list" — 策略B: Emerson 变体
                    infoXml = buildGetDataInfoXml(fsuCode, deviceIds, semaphores, deviceCodes);
                    break;
            }

            // 注册上下文模式下使用 effectiveServiceUrl (来自 registrationContext.fsuIp)
            // manual_probe 模式下使用 endpoint resolver
            String actualServiceUrl = effectiveServiceUrl != null
                    ? effectiveServiceUrl : endpoint.getServiceUrl();

            FsuServiceRequest request = FsuServiceRequest.builder()
                    .fsuCode(fsuCode)
                    .serviceUrl(actualServiceUrl)
                    .pkType(BInterfacePkType.GET_DATA)
                    .pkTypeFormat("legacy-2016")
                    .infoXml(infoXml)
                    .xmlDataXml(xmlDataXml)
                    .build();

            FsuServiceResponse response = fsuServiceClient.call(request);
            boolean realCall = response.isRealCall();
            String rawSoap = response.getRawSoap();

            // Save raw samples
            String samplePath = saveRawSample(sampleOp, fsuCode,
                    (infoXml != null ? infoXml : "") +
                    (xmlDataXml != null ? "\n<!-- xmlData -->\n" + xmlDataXml : ""),
                    rawSoap);

            // Parse FSU Result field from raw SOAP
            String fsuResultRaw = extractXmlValue(rawSoap, "Result");
            boolean deviceListPresent = rawSoap != null && rawSoap.contains("<DeviceList>")
                    && !rawSoap.contains("<DeviceList/>");

            // Build response
            GetDataProbeResponse resp = new GetDataProbeResponse();
            resp.setFsuCode(fsuCode);
            resp.setRealDeviceAccessed(realCall);
            resp.setProbeTime(probeTime);
            resp.setRawSamplePath(samplePath);
            resp.setStrategyName(strategy);
            resp.setHttpSuccess(true);
            resp.setFsuResultRaw(fsuResultRaw);

            // BIF2016-CONNECTION-003: registration context metadata
            resp.setTargetSource(useRegCtx ? "login_registration_context" : "manual_probe");
            resp.setRegistrationContextUsed(useRegCtx && regCtx != null);
            resp.setFsuIpSource(fsuIpSource);
            resp.setDeviceListSource(deviceListSource);
            resp.setDeviceCapabilityCount(deviceIds != null ? deviceIds.size() : 0);
            if (regCtx != null) {
                resp.setRegistrationContextCompleteness(regCtx.getContextCompleteness());
                resp.setRegistrationContextMissingFields(
                        regCtx.getMissingFields() != null ? regCtx.getMissingFields() : List.of());
            }

            // Result semantics per 2016 EnumResult: FAILURE=0, SUCCESS=1
            if (fsuResultRaw != null) {
                boolean isSuccess = "1".equals(fsuResultRaw);
                resp.setFsuResultMeaning(
                    isSuccess ? "Result=1: SUCCESS (per 2016 EnumResult)" :
                    "Result=0: FAILURE (per 2016 EnumResult)");
                resp.setProtocolResultMeaning(isSuccess ? "SUCCESS" : "FAILURE");
            } else {
                resp.setProtocolResultMeaning("UNKNOWN");
            }

            if (!response.isSuccess()) {
                resp.setSuccess(false);
                resp.setBusinessSuccess(false);
                resp.setStatusText("FSU client call failed");
                resp.setResultCode(response.getResultCode());
                resp.setResultDesc(response.getResultDesc());
                resp.setDevices(deviceResults);
                return resp;
            }

            boolean ackOk = rawSoap != null &&
                    (rawSoap.contains("<Code>402</Code>") || rawSoap.contains("<Code>402<"));
            resp.setAckReceived(ackOk);
            resp.setAckCode(ackOk ? 402 : 0);
            resp.setDeviceListPresent(deviceListPresent);

            // Compute businessSuccess: strictly ACK=402 + Result=1 (per 2016 EnumResult)
            boolean bizSuccess = ackOk && "1".equals(fsuResultRaw);

            // Parse response xmlData
            XmlDataModel xmlData = response.getXmlData();
            boolean empty = (xmlData == null || xmlData.isEmpty());

            if (empty) {
                resp.setSuccess(true);
                resp.setBusinessSuccess(bizSuccess);
                resp.setEmptyData(true);
                if (bizSuccess) {
                    resp.setStatusText("GET_DATA_ACK success, but no values returned");
                } else if (!ackOk) {
                    resp.setStatusText("GET_DATA_ACK not received or unexpected ACK code");
                } else if (fsuResultRaw == null) {
                    resp.setStatusText("GET_DATA_ACK result is unknown");
                } else {
                    resp.setStatusText("GET_DATA_ACK returned FAILURE (Result=0 per 2016 EnumResult)");
                }
                resp.setEmptyDataMessage(
                        "已基于 B接口2016 协议和标准信号字典完成策略探测。"
                        + (ackOk ? " FSU 返回 ACK=402" : " FSU 未返回预期 ACK=402")
                        + (fsuResultRaw != null ? " Result=" + fsuResultRaw : " Result missing")
                        + "。");
                resp.setDevices(deviceResults);
                return resp;
            }

            // Merge returned values into device results
            mergeXmlDataIntoResults(xmlData, deviceResults);

            resp.setSuccess(true);
            resp.setBusinessSuccess(bizSuccess);
            resp.setEmptyData(false);
            resp.setStatusText(bizSuccess
                    ? "GET_DATA_ACK success, values returned"
                    : "GET_DATA_ACK unexpected result");
            resp.setDevices(deviceResults);
            return resp;

        } catch (Exception e) {
            log.error("GET_DATA probe 异常: fsuCode={}", fsuCode, e);
            GetDataProbeResponse err = new GetDataProbeResponse();
            err.setSuccess(false);
            err.setFsuCode(fsuCode);
            err.setResultCode("PROBE_ERROR");
            err.setResultDesc("GET_DATA probe 异常: " + e.getMessage());
            err.setDevices(deviceResults);
            return err;
        }
    }

    // ==================== CSV loading ====================

    private List<CsvSignalRow> loadSignalDictionary() throws IOException {
        // Try multiple possible locations
        List<String> candidates = List.of(
                "docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv",
                "../docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv",
                "/home/tom/桌面/FSU/docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv"
        );

        Path found = null;
        for (String c : candidates) {
            Path p = Path.of(c);
            if (Files.exists(p)) { found = p; break; }
        }

        if (found == null) {
            throw new FileNotFoundException("找不到标准信号字典 CSV，尝试路径: " + candidates);
        }

        List<CsvSignalRow> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(found, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) return rows;
            String[] cols = header.split(",", -1);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < cols.length; i++) {
                idx.put(cols[i].trim().toLowerCase(), i);
            }

            Integer siCol = idx.get("signal_id");
            Integer snCol = idx.get("signal_name");
            Integer stCol = idx.get("signal_type");
            Integer unCol = idx.get("unit");
            Integer dtCol = idx.get("device_type_code");

            if (siCol == null) {
                // Try alternative column names
                siCol = idx.get("spid");
            }
            if (siCol == null) {
                throw new IOException("CSV 缺少 signal_id 列，可用列: " + idx.keySet());
            }

            String line;
            int rowNum = 1;
            while ((line = br.readLine()) != null) {
                rowNum++;
                if (line.trim().isEmpty()) continue;
                String[] vals = line.split(",", -1);
                String signalId = val(vals, siCol).trim();
                if (signalId.isEmpty()) continue;

                CsvSignalRow row = new CsvSignalRow();
                row.signalId = signalId;
                row.signalName = snCol != null ? val(vals, snCol).trim() : "";
                row.signalType = stCol != null ? val(vals, stCol).trim() : "";
                row.unit = unCol != null ? val(vals, unCol).trim() : "";
                row.deviceTypeCode = dtCol != null ? val(vals, dtCol).trim() : "";
                rows.add(row);
            }
        }
        log.info("加载信号字典: {} 行 from {}", rows.size(), found);
        return rows;
    }

    private static String val(String[] arr, int idx) {
        return idx < arr.length ? (arr[idx] != null ? arr[idx] : "") : "";
    }

    // ==================== DeviceType inference ====================

    /**
     * 从 DeviceID 推断 device_type_code。
     * B接口2016 DeviceID: 第8-9位(1-indexed) = substring(7,9)
     * 51051241820004 → [7:9]="18", 51051240700002 → [7:9]="07"
     */
    public static String inferDeviceTypeCode(String deviceId) {
        if (deviceId == null || deviceId.length() < 9) return "00";
        return deviceId.substring(7, 9);
    }

    /**
     * 从信号字典中筛选匹配 device_type_code 的 SignalID。
     * SignalID 10位: 第3-4位(1-indexed) = substring(2,4) = device_type_code
     */
    public static List<CsvSignalRow> filterByDeviceType(List<CsvSignalRow> dictionary,
                                                  String deviceTypeCode, int maxResults) {
        String dtc = deviceTypeCode != null ? deviceTypeCode : "00";
        if (dtc.length() == 1) dtc = "0" + dtc;

        List<CsvSignalRow> result = new ArrayList<>();
        for (CsvSignalRow row : dictionary) {
            if (row.signalId == null || row.signalId.length() < 4) continue;
            String rowType = row.signalId.substring(2, 4);
            if (dtc.equals(rowType)) {
                result.add(row);
                if (result.size() >= maxResults) break;
            }
        }
        return result;
    }

    // ==================== XML helpers ====================

    /**
     * 构建 2016 标准 GET_DATA xmlData: 简单 SignalID 列表。
     * 不使用 DeviceList/TSemaphore（那是 Emerson 变体格式）。
     */
    /**
     * 策略D: Device.Code = 999999999999 (全9, B接口2016 设备编码长度12)。
     * 不指定 TSemaphore, 查询该设备编码下所有信号。
     */
    private String buildAll9DeviceCodeXml(String fsuCode, String deviceId) {
        return "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList>"
                + "<Device Id=\"" + escape(deviceId) + "\" Code=\"999999999999\"/>"
                + "</DeviceList>";
    }

    /**
     * 策略E: Device.Id 全9 + Device.Code 全9。
     */
    private String buildAll9IdAndCodeXml(String fsuCode) {
        return "<FsuId>" + escape(fsuCode) + "</FsuId>"
                + "<FsuCode>" + escape(fsuCode) + "</FsuCode>"
                + "<DeviceList>"
                + "<Device Id=\"999999999999\" Code=\"999999999999\"/>"
                + "</DeviceList>";
    }

    /**
     * 策略F: 按设备类型分组，每组取第一个 DeviceID + 第一个 SignalID。
     */
    private String buildPerDeviceTypeXml(String fsuCode, List<String> allDeviceIds,
                                          List<DeviceProbeResult> deviceResults) {
        // Group by device type, take first device per type
        Map<String, String> typeToDevice = new LinkedHashMap<>();
        for (DeviceProbeResult dpr : deviceResults) {
            String dt = dpr.getDeviceTypeCode();
            if (!typeToDevice.containsKey(dt)) {
                typeToDevice.put(dt, dpr.getDeviceId());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<FsuId>").append(escape(fsuCode)).append("</FsuId>");
        sb.append("<FsuCode>").append(escape(fsuCode)).append("</FsuCode>");
        sb.append("<DeviceList>");
        for (var entry : typeToDevice.entrySet()) {
            String dt = entry.getKey();
            String devId = entry.getValue();
            // Find first SignalID for this device type
            String firstSig = null;
            for (DeviceProbeResult dpr : deviceResults) {
                if (dt.equals(dpr.getDeviceTypeCode()) && dpr.getValues() != null
                        && !dpr.getValues().isEmpty()) {
                    firstSig = dpr.getValues().get(0).getSignalId();
                    break;
                }
            }
            if (firstSig == null) firstSig = "01" + dt + "001001";

            sb.append("<Device Id=\"").append(escape(devId))
              .append("\" Code=\"").append(escape(devId)).append("\">\n");
            sb.append("<Id>").append(escape(firstSig)).append("</Id>\n");
            sb.append("</Device>\n");
        }
        sb.append("</DeviceList>");
        return sb.toString();
    }

    private String buildSimpleSignalIdXml(List<String> deviceIds,
                                           Map<String, List<String>> semaphores) {
        StringBuilder sb = new StringBuilder();
        for (var entry : semaphores.entrySet()) {
            for (String sigId : entry.getValue()) {
                sb.append("<SignalID>").append(escape(sigId)).append("</SignalID>");
            }
        }
        return sb.toString();
    }

    private String buildGetDataInfoXml(String fsuCode, List<String> deviceIds,
                                        Map<String, List<String>> semaphores) {
        return buildGetDataInfoXml(fsuCode, deviceIds, semaphores, Map.of());
    }

    private String buildGetDataInfoXml(String fsuCode, List<String> deviceIds,
                                        Map<String, List<String>> semaphores,
                                        Map<String, String> deviceCodes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<FsuId>").append(escape(fsuCode)).append("</FsuId>");
        sb.append("<FsuCode>").append(escape(fsuCode)).append("</FsuCode>");
        sb.append("<DeviceList>");
        for (String devId : deviceIds) {
            String code = deviceCodes.getOrDefault(devId, devId);
            sb.append("<Device Id=\"").append(escape(devId))
              .append("\" Code=\"").append(escape(code)).append("\"");
            List<String> semList = semaphores.getOrDefault(devId, List.of());
            if (!semList.isEmpty()) {
                sb.append(">\n");
                boolean hasValid = false;
                for (String semId : semList) {
                    if (semId == null || semId.isBlank()) continue;
                    sb.append("<Id>").append(escape(semId)).append("</Id>\n");
                    hasValid = true;
                }
                if (!hasValid) {
                    sb.setLength(sb.length() - 2);
                    sb.append("/>\n");
                } else {
                    sb.append("</Device>\n");
                }
            } else {
                sb.append("/>\n");
            }
        }
        sb.append("</DeviceList>");
        return sb.toString();
    }

    private void mergeXmlDataIntoResults(XmlDataModel xmlData, List<DeviceProbeResult> deviceResults) {
        if (xmlData == null) return;

        Map<String, DeviceProbeResult> devMap = new LinkedHashMap<>();
        for (DeviceProbeResult dpr : deviceResults) {
            devMap.put(dpr.getDeviceId(), dpr);
        }

        List<Map<String, String>> items = xmlData.getItems();
        if (items != null) {
            for (Map<String, String> item : items) {
                String devId = getField(item, "DeviceID");
                String id = getField(item, "Id");
                String measuredVal = getField(item, "MeasuredVal");
                String status = getField(item, "Status");

                if (devId == null) devId = "unknown";
                DeviceProbeResult dpr = devMap.get(devId);
                if (dpr == null && "unknown".equals(devId) && !deviceResults.isEmpty()) {
                    dpr = deviceResults.get(0);
                }
                if (dpr == null) continue;

                // Try to find matching signal in values
                boolean updated = false;
                for (SignalValueResult sv : dpr.getValues()) {
                    if (id != null && (id.equals(sv.getSignalId()) || id.equals(sv.getSpid()))) {
                        sv.setValue(measuredVal);
                        sv.setQuality("real-fsu");
                        updated = true;
                        break;
                    }
                }
                if (!updated && id != null) {
                    SignalValueResult sv = new SignalValueResult();
                    sv.setSignalId(id);
                    sv.setSpid(id);
                    sv.setValue(measuredVal);
                    sv.setQuality("real-fsu-unmatched");
                    dpr.getValues().add(sv);
                }
            }

            // Update counts
            for (DeviceProbeResult dpr : deviceResults) {
                int count = 0;
                for (SignalValueResult sv : dpr.getValues()) {
                    if (sv.getValue() != null && !sv.getValue().isEmpty()) count++;
                }
                dpr.setValuesReturned(count);
            }
        }
    }

    // ==================== Raw sample saving ====================

    private String saveRawSample(String operation, String fsuCode, String requestXml, String responseXml) {
        try {
            String dir = "docs/landing/raw-samples";
            Path dirPath = Path.of(dir);
            if (!Files.exists(dirPath)) {
                // Try relative to project root
                dirPath = Path.of("../" + dir);
            }
            Files.createDirectories(dirPath);

            String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            String filename = String.format("fast-realdata-001-%s-%s-%s.xml", operation, fsuCode, ts);
            Path filePath = dirPath.resolve(filename);

            StringBuilder content = new StringBuilder();
            content.append("<!-- FAST-REALDATA-001 raw sample -->\n");
            content.append("<!-- operation: ").append(operation).append(" -->\n");
            content.append("<!-- fsuCode: ").append(fsuCode).append(" -->\n");
            content.append("<!-- timestamp: ").append(LocalDateTime.now()).append(" -->\n\n");

            content.append("=== REQUEST ===\n");
            content.append(requestXml != null ? requestXml : "(null)").append("\n\n");
            content.append("=== RESPONSE ===\n");
            content.append(responseXml != null ? responseXml : "(null)").append("\n");

            Files.writeString(filePath, content.toString(), StandardCharsets.UTF_8);

            log.info("Raw sample saved: {}", filePath);
            return filePath.toString();
        } catch (Exception e) {
            log.warn("保存 raw sample 失败", e);
            return null;
        }
    }

    // ==================== Registration context helpers ====================

    private GetDataProbeResponse buildRegCtxError(String fsuCode, String probeTime,
                                                    BInterfaceFsuRegistrationContextDto regCtx,
                                                    String errorCode, String message) {
        GetDataProbeResponse resp = new GetDataProbeResponse();
        resp.setSuccess(false);
        resp.setFsuCode(fsuCode);
        resp.setTargetSource("login_registration_context");
        resp.setRegistrationContextUsed(false);
        resp.setManualFallbackUsed(false);
        resp.setErrorCode(errorCode);
        resp.setResultDesc(message);
        resp.setProbeTime(probeTime);
        if (regCtx != null) {
            resp.setRegistrationContextCompleteness(regCtx.getContextCompleteness());
            resp.setRegistrationContextMissingFields(
                    regCtx.getMissingFields() != null ? regCtx.getMissingFields() : List.of());
        }
        return resp;
    }

    // ==================== Validation ====================

    private void validateReadOnly(String operator, String reason, boolean confirmReadOnly) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new IllegalArgumentException("operator 不能为空");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("reason 不能为空");
        }
        if (!confirmReadOnly) {
            throw new IllegalArgumentException("confirmReadOnly 必须为 true");
        }
        if (!realCallEnabled) {
            throw new IllegalStateException(
                    "真实 FSU 调用未启用 (b-interface.fsu-client.real-call-enabled=false)。" +
                    "请设置 real-call-enabled=true 或在 application-dev.yml 中启用。");
        }
    }

    // ==================== Utility ====================

    private String getField(Map<String, String> map, String key) {
        if (map == null || key == null) return null;
        String v = map.get(key);
        if (v != null) return v.trim();
        for (var e : map.entrySet()) {
            if (e.getKey().equalsIgnoreCase(key))
                return e.getValue() != null ? e.getValue().trim() : null;
        }
        return null;
    }

    /** 从 XML 字符串中用简单正则提取标签值。 */
    private String extractXmlValue(String xml, String tagName) {
        if (xml == null || tagName == null) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "<" + tagName + ">([^<]*)</" + tagName + ">",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(xml);
        if (m.find()) {
            String v = m.group(1);
            return v != null ? v.trim() : null;
        }
        return null;
    }

    /** 替换 URL 中的 host 部分，保留 scheme/port/path/query/fragment。 */
    static String replaceHostOnly(String templateUrl, String newHost) {
        try {
            java.net.URI uri = java.net.URI.create(templateUrl);
            java.net.URI replaced = new java.net.URI(
                    uri.getScheme(), uri.getUserInfo(), newHost,
                    uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            return replaced.toString();
        } catch (Exception e) {
            log.warn("URL host 替换失败: url={}, host={}", templateUrl, newHost, e);
            return templateUrl;
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // ==================== Inner types ====================

    public static class CsvSignalRow {
        public String signalId;
        public String signalName;
        public String signalType;
        public String unit;
        public String deviceTypeCode;
    }
}
