package com.dcim.platform.module.binterface.controller;

import com.dcim.platform.common.response.ApiResponse;
import com.dcim.platform.common.security.RequirePermission;
import com.dcim.platform.common.security.Permissions;
import com.dcim.platform.common.security.DataScopeService;
import com.dcim.platform.module.alarm.entity.AlarmRecordEntity;
import com.dcim.platform.module.alarm.repository.AlarmRecordRepository;
import com.dcim.platform.module.binterface.dto.BInterfaceFrontendDtos.*;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.repository.BInterfaceFsuStatusRepository;
import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import com.dcim.platform.module.mapping.entity.DeviceSignalCandidateEntity;
import com.dcim.platform.module.mapping.entity.UnmappedSignalObservationEntity;
import com.dcim.platform.module.mapping.repository.DeviceSignalCandidateRepository;
import com.dcim.platform.module.mapping.repository.UnmappedSignalObservationRepository;
import com.dcim.platform.module.mapping.service.EStoneIIDictionaryImportService;
import com.dcim.platform.module.mapping.service.EStoneIIMappingResult;
import com.dcim.platform.module.mapping.service.EStoneIIMappingService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.entity.MonitoringPointEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import com.dcim.platform.module.resource.repository.MonitoringPointRepository;
import com.dcim.platform.module.telemetry.entity.RealtimeDataEntity;
import com.dcim.platform.module.telemetry.repository.RealtimeDataRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * B接口前端只读 REST API (BACKEND-FE-API-002).
 * 8 个端点 — 全部只读, 不访问真实 FSU, 不执行 SET.
 */
@RestController
@RequestMapping("/api/b-interface")
@RequirePermission(Permissions.FSU_VIEW)
public class BInterfaceFrontendReadController {

    private final BInterfaceFsuStatusRepository fsuStatusRepo;
    private final FsuDeviceRepository fsuDeviceRepo;
    private final BInterfaceMessageLogRepository msgLogRepo;
    private final BInterfaceMessageLogService msgLogService;
    private final AlarmRecordRepository alarmRepo;
    private final RealtimeDataRepository rtRepo;
    private final MonitoringPointRepository pointRepo;
    private final DataScopeService dataScopeService;
    private final EStoneIIDictionaryImportService dictionaryImportService;
    private final EStoneIIMappingService mappingService;
    private final DeviceSignalCandidateRepository candidateRepo;
    private final UnmappedSignalObservationRepository unmappedRepo;

    public BInterfaceFrontendReadController(BInterfaceFsuStatusRepository fsr, FsuDeviceRepository fdr,
                                             BInterfaceMessageLogRepository mlr, BInterfaceMessageLogService mls,
                                             AlarmRecordRepository ar, RealtimeDataRepository rr,
                                             MonitoringPointRepository pr, DataScopeService dss,
                                             EStoneIIDictionaryImportService dis, EStoneIIMappingService ms,
                                             DeviceSignalCandidateRepository cr,
                                             UnmappedSignalObservationRepository ur) {
        this.fsuStatusRepo = fsr; this.fsuDeviceRepo = fdr; this.msgLogRepo = mlr; this.msgLogService = mls;
        this.alarmRepo = ar; this.rtRepo = rr; this.pointRepo = pr;
        this.dataScopeService = dss;
        this.dictionaryImportService = dis;
        this.mappingService = ms;
        this.candidateRepo = cr;
        this.unmappedRepo = ur;
    }

    // ===== 1. FSU 列表 =====
    @GetMapping("/fsus")
    public ApiResponse<List<FsuSummary>> listFsus(@RequestParam(required = false) String fsuCode) {
        List<FsuSummary> list = new ArrayList<>();
        for (var status : dataScopeService.filterByFsuScope(fsuStatusRepo.findAll(), e -> e.getFsuCode())) {
            if (fsuCode != null && !fsuCode.equals(status.getFsuCode())) continue;
            FsuSummary s = buildFsuSummary(status);
            var fsu = fsuDeviceRepo.findByFsuCode(status.getFsuCode());
            if (fsu.isPresent()) {
                s.fsuIp = fsu.get().getIpAddr(); s.fsuId = status.getFsuCode();
                s.stationName = "1"; s.macId = fsu.get().getMacAddr();
            }
            s.deviceCount = knownDeviceCount(status.getFsuCode());
            s.mappedPointCount = (int) pointRepo.findByFsuId(status.getFsuId()).stream().filter(p -> p.getPointCode() != null).count();
            s.unmappedDeviceCount = 5 - s.mappedPointCount;
            list.add(s);
        }
        return ApiResponse.success(list);
    }

    // ===== 2. FSU 详情 =====
    @GetMapping("/fsus/{fsuCode}")
    public ApiResponse<FsuDetail> getFsuDetail(@PathVariable String fsuCode) {
        var status = dataScopeService.filterByFsuScope(fsuStatusRepo.findAll(), e -> e.getFsuCode()).stream().filter(e -> e.getFsuCode() != null && e.getFsuCode().equals(fsuCode)).findFirst().orElse(null);
        if (status == null) return ApiResponse.fail("FSU not found");
        FsuDetail d = new FsuDetail();
        copyFsuFields(buildFsuSummary(status), d);
        var fsu = fsuDeviceRepo.findByFsuCode(fsuCode);
        fsu.ifPresent(f -> { d.fsuIp = f.getIpAddr(); d.macId = f.getMacAddr(); d.fsuId = fsuCode; d.stationName = "1"; });
        d.devices = buildKnownDevices(fsuCode);
        d.recentMessages = buildRecentMessages(fsuCode);
        return ApiResponse.success(d);
    }

    // ===== 3. DeviceList =====
    @GetMapping("/fsus/{fsuCode}/devices")
    public ApiResponse<List<DeviceInfo>> getDevices(@PathVariable String fsuCode) {
        if (!canAccessFsu(fsuCode)) return ApiResponse.fail("FSU not found");
        return ApiResponse.success(buildKnownDevices(fsuCode));
    }

    // ===== 4. Message Logs — already at /api/b-interface/message-logs/query =====

    // ===== A. FTP / LoginInfo =====
    @GetMapping({"/ftp", "/fsus/{fsuCode}/ftp"})
    public ApiResponse<List<Map<String,Object>>> getFtpConfig(@PathVariable(required = false) String fsuCode) {
        return ApiResponse.success(List.of());
    }
    @GetMapping({"/login-info", "/fsus/{fsuCode}/login-info"})
    public ApiResponse<Map<String,Object>> getLoginInfo(@PathVariable(required = false) String fsuCode) {
        List<BInterfaceFsuStatusEntity> visible = dataScopeService.filterByFsuScope(
                fsuStatusRepo.findAll(), BInterfaceFsuStatusEntity::getFsuCode);
        BInterfaceFsuStatusEntity status = visible.stream()
                .filter(e -> fsuCode == null || fsuCode.equals(e.getFsuCode()))
                .findFirst()
                .orElse(null);
        if (status == null) return ApiResponse.fail("FSU not found");
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("fsuCode", status.getFsuCode());
        m.put("stationName", "1");
        fsuDeviceRepo.findByFsuCode(status.getFsuCode()).ifPresent(f -> {
            m.put("macId", f.getMacAddr());
            m.put("version", f.getFirmwareVersion());
        });
        m.put("source", "LOGIN");
        return ApiResponse.success(m);
    }

    // ===== B. FTP Images =====
    @GetMapping("/ftp/images")
    public ApiResponse<List<Map<String,Object>>> getFtpImages() { return ApiResponse.success(List.of()); }
    @GetMapping("/ftp/images/pull-runs")
    public ApiResponse<List<Map<String,Object>>> getFtpImagePullRuns() { return ApiResponse.success(List.of()); }

    // ===== C. Scheduler =====
    @GetMapping("/schedulers/configs")
    public ApiResponse<List<Map<String,Object>>> getSchedulerConfigs() {
        List<Map<String,Object>> list = new ArrayList<>();
        for (String type : List.of("GET_DATA_POLL","GET_FSUINFO_HEARTBEAT","FTP_IMAGE_PULL","ACTIVE_ALARM_AUDIT")) {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("schedulerType", type); m.put("enabled", false);
            m.put("schedulerEnabled", false); m.put("realCallEnabled", false); m.put("allowedSuids", List.of()); m.put("source", "config"); list.add(m);
        }
        return ApiResponse.success(list);
    }
    @GetMapping("/schedulers/runs")
    public ApiResponse<List<Map<String,Object>>> getSchedulerRuns() { return ApiResponse.success(List.of()); }

    // ===== D. Protocol / Audit =====
    @GetMapping("/protocol/matrix")
    public ApiResponse<List<Map<String,Object>>> getProtocolMatrix() {
        List<Map<String,Object>> list = new ArrayList<>();
        String[][] cmds = {{"LOGIN","101"},{"LOGOUT","103"},{"SEND_ALARM","501"},{"GET_DATA","401"},{"GET_HISDATA","403"},
                {"GET_THRESHOLD","1901"},{"SET_POINT","1001"},{"SET_THRESHOLD","2001"},{"GET_LOGININFO","1501"},
                {"SET_LOGININFO","1503"},{"GET_FTP","1601"},{"SET_FTP","1603"},{"GET_FSUINFO","1701"},
                {"SET_FSUREBOOT","1801"},{"TIME_CHECK","1301"},{"GET_ACTIVEALARM","603"}};
        for (String[] c : cmds) { Map<String,Object> m = new LinkedHashMap<>(); m.put("commandName", c[0]); m.put("requestCode", c[1]); m.put("protocolVersion", "2016"); m.put("source", "backend-planning"); list.add(m); }
        return ApiResponse.success(list);
    }
    @GetMapping("/error-codes")
    public ApiResponse<List<Map<String,String>>> getErrorCodes() {
        return ApiResponse.success(List.of(
                Map.of("code","safety_blocked","category","safety","severity","warning"),
                Map.of("code","real_call_disabled","category","safety","severity","warning"),
                Map.of("code","not_whitelisted","category","safety","severity","warning"),
                Map.of("code","scheduler_disabled","category","safety","severity","info"),
                Map.of("code","business_failure","category","business","severity","warning"),
                Map.of("code","soap_timeout","category","transport","severity","danger"),
                Map.of("code","xml_parse_error","category","parse","severity","danger"),
                Map.of("code","circuit_open","category","circuit_breaker","severity","info"),
                Map.of("code","not_mapped","category","mapping","severity","info")));
    }
    @GetMapping("/audits")
    public ApiResponse<List<Map<String,Object>>> getAudits() { return ApiResponse.success(List.of()); }
    @GetMapping("/protocol/profiles")
    public ApiResponse<List<Map<String,String>>> getProfiles() {
        return ApiResponse.success(List.of(Map.of("profile","standard-2016","role","标准协议"), Map.of("profile","emerson-2016","role","当前真实设备"), Map.of("profile","future-2024","role","兼容层")));
    }
    @GetMapping("/set/safety-policies")
    public ApiResponse<List<Map<String,Object>>> getSetPolicies() {
        List<Map<String,Object>> list = new ArrayList<>();
        for (String cmd : List.of("SET_POINT","SET_THRESHOLD","SET_FTP","SET_LOGININFO","SET_FSUREBOOT","AUTO_UPGRADE")) {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("commandName", cmd); m.put("enabled", false); m.put("realCallEnabled", false);
            m.put("requiresConfirmationToken", true); m.put("requiresWhitelist", true); m.put("status", "blocked_by_default"); list.add(m);
        }
        return ApiResponse.success(list);
    }

    // ===== E. ActiveAlarmDiff =====
    @GetMapping("/active-alarms/diff")
    public ApiResponse<List<Map<String,Object>>> getActiveAlarmDiff() { return ApiResponse.success(List.of()); }
    @GetMapping("/active-alarms/audit-runs")
    public ApiResponse<List<Map<String,Object>>> getActiveAlarmAuditRuns() { return ApiResponse.success(List.of()); }

    // ===== F. Run-once capabilities =====
    @GetMapping("/run-once/capabilities")
    public ApiResponse<List<Map<String,Object>>> getRunOnceCapabilities() {
        List<Map<String,Object>> list = new ArrayList<>();
        for (String cmd : List.of("GET_FSUINFO","GET_DATA","GET_THRESHOLD","GET_ACTIVEALARM","FTP_IMAGE_PULL")) {
            Map<String,Object> m = new LinkedHashMap<>(); m.put("commandName", cmd); m.put("serviceImplemented", true);
            m.put("restEndpointImplemented", false); m.put("dryRunSupported", true); m.put("realCallSupported", false);
            m.put("realCallEnabled", false); m.put("whitelistRequired", true); m.put("status", "planning"); list.add(m);
        }
        return ApiResponse.success(list);
    }

    // ===== 5. Alarms =====
    @GetMapping("/alarms")
    public ApiResponse<List<AlarmDto>> getAlarms(@RequestParam(required = false) String fsuCode) {
        dictionaryImportService.importIfNeeded();
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        List<AlarmRecordEntity> alarms = dataScopeService.filterByFsuScope(alarmRepo.findAll(),
                a -> fsuCodeById.getOrDefault(a.getFsuId(), String.valueOf(a.getFsuId())));
        if (fsuCode != null) {
            Long targetFsuId = fsuDeviceRepo.findByFsuCode(fsuCode).map(FsuDeviceEntity::getId).orElse(-1L);
            alarms = alarms.stream().filter(a -> a.getFsuId() != null && a.getFsuId().equals(targetFsuId)).collect(java.util.stream.Collectors.toList());
        }
        List<AlarmDto> list = new ArrayList<>();
        for (var a : alarms) {
            AlarmDto dto = new AlarmDto();
            dto.id = a.getId(); dto.serialNo = a.getSerialNo(); dto.deviceId = a.getDeviceId();
            dto.spid = a.getSpid(); dto.signalId = a.getPointCode(); dto.alarmLevel = a.getAlarmLevel();
            dto.alarmStatus = a.getAlarmStatus(); dto.alarmDesc = a.getAlarmDesc();
            dto.alarmTime = a.getOccurTime() != null ? a.getOccurTime().toString() : null;
            dto.recoveryTime = a.getClearTime() != null ? a.getClearTime().toString() : null;
            dto.createdAt = a.getCreatedAt() != null ? a.getCreatedAt().toString() : null;
            dto.updatedAt = a.getUpdatedAt() != null ? a.getUpdatedAt().toString() : null;
            dto.fsuCode = fsuCodeById.get(a.getFsuId());
            EStoneIIMappingResult mapped = mappingService.resolveAlarm(dto.fsuCode, a.getDeviceId(), null,
                    a.getSpid(), a.getPointCode(), a.getAlarmCode(), a.getAlarmValue());
            applyAlarmMapping(dto, mapped);
            list.add(dto);
        }
        return ApiResponse.success(list);
    }

    // ===== 6. Realtime Points =====
    @GetMapping("/realtime-points")
    public ApiResponse<List<RealtimePointDto>> getRealtimePoints(@RequestParam(required = false) String fsuCode) {
        dictionaryImportService.importIfNeeded();
        Map<Long, String> fsuCodeById = fsuCodeByDbId();
        List<RealtimeDataEntity> data = dataScopeService.filterByFsuScope(rtRepo.findAll(),
                r -> fsuCodeById.getOrDefault(r.getFsuId(), String.valueOf(r.getFsuId())));
        if (fsuCode != null) {
            Long targetFsuId = fsuDeviceRepo.findByFsuCode(fsuCode).map(FsuDeviceEntity::getId).orElse(-1L);
            data = data.stream().filter(r -> r.getFsuId() != null && r.getFsuId().equals(targetFsuId)).collect(java.util.stream.Collectors.toList());
        }
        List<RealtimePointDto> list = new ArrayList<>();
        for (var d : data) {
            RealtimePointDto dto = new RealtimePointDto();
            dto.fsuCode = fsuCodeById.get(d.getFsuId());
            dto.signalId = d.getPointCode();
            dto.valueText = d.getValueText(); dto.valueNumber = d.getValueNumber();
            dto.value = d.getValueText() != null ? d.getValueText()
                    : (d.getValueNumber() != null ? d.getValueNumber().toPlainString() : null);
            dto.collectTime = d.getCollectTime() != null ? d.getCollectTime().toString() : null;
            dto.updatedAt = d.getUpdatedAt() != null ? d.getUpdatedAt().toString() : null;
            EStoneIIMappingResult mapped = mappingService.resolveRealtime(dto.fsuCode, null, null,
                    d.getPointCode(), d.getPointCode(), dto.value);
            applyRealtimeMapping(dto, mapped);
            dto.hasSignalIdentity = hasSignalIdentity(d.getPointCode());
            dto.isDeviceOnly = false;
            dto.legacyData = false;
            dto.observationType = "REALTIME_SIGNAL";
            if (!mapped.mapped() && isLegacyRealtimePointCode(d.getPointCode())) {
                dto.mappingStatus = "HISTORICAL_PENDING_BACKFILL";
                dto.mappingConfidence = "UNKNOWN";
                dto.source = "legacy_realtime_data";
                dto.needRealDataConfirm = true;
                dto.legacyData = true;
                dto.observationType = "HISTORICAL_REALTIME";
            }
            list.add(dto);
        }
        return ApiResponse.success(list);
    }

    // ===== 7. Unmapped Signals =====
    @GetMapping("/unmapped-signals")
    public ApiResponse<List<UnmappedSignalDto>> getUnmappedSignals(@RequestParam(required = false) String fsuCode) {
        dictionaryImportService.importIfNeeded();
        List<UnmappedSignalDto> list = new ArrayList<>();
        List<DeviceSignalCandidateEntity> candidates = dataScopeService.filterByFsuScope(
                candidateRepo.findAllByOrderByDeviceIdAscSignalIdAsc(), DeviceSignalCandidateEntity::getFsuId);
        if (fsuCode != null) {
            candidates = candidates.stream()
                    .filter(c -> fsuCode.equals(c.getFsuId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        for (DeviceSignalCandidateEntity c : candidates) {
            UnmappedSignalDto dto = new UnmappedSignalDto();
            dto.fsuCode = c.getFsuId(); dto.deviceId = c.getDeviceId(); dto.deviceCode = c.getDeviceCode();
            dto.spid = c.getSignalId(); dto.signalId = c.getSignalId(); dto.signalName = c.getSignalName();
            dto.rawId = c.getSignalId(); dto.rawName = c.getSignalName();
            dto.mappingStatus = c.getMappingStatus(); dto.mappingConfidence = c.getConfidence();
            dto.templateVariant = c.getTemplateVariant(); dto.needRealDataConfirm = c.getNeedRealDataConfirm();
            dto.verifiedByRealData = c.getVerifiedByRealData(); dto.source = c.getMappingSource();
            dto.reason = "D_CLASS_ASSUMED_TEMPLATE_CANDIDATE";
            dto.hasSignalIdentity = hasSignalIdentity(dto.spid, dto.signalId, dto.rawId);
            dto.isDeviceOnly = false;
            dto.legacyData = false;
            dto.observationType = "TEMPLATE_CANDIDATE";
            dto.seenCount = 0;
            EStoneIIMappingResult mapped = mappingService.resolveRealtime(c.getFsuId(), c.getDeviceId(), c.getDeviceCode(),
                    c.getSignalId(), c.getSignalId(), null);
            dto.signalType = mapped.signalType(); dto.unit = mapped.unit();
            list.add(dto);
        }
        List<UnmappedSignalObservationEntity> observations = dataScopeService.filterByFsuScope(
                unmappedRepo.findAllByOrderByLastSeenAtDesc(), UnmappedSignalObservationEntity::getFsuId);
        if (fsuCode != null) {
            observations = observations.stream()
                    .filter(u -> fsuCode.equals(u.getFsuId()))
                    .collect(java.util.stream.Collectors.toList());
        }
        for (UnmappedSignalObservationEntity u : observations) {
            UnmappedSignalDto dto = new UnmappedSignalDto();
            dto.fsuCode = u.getFsuId(); dto.deviceId = u.getDeviceId(); dto.deviceCode = u.getDeviceCode();
            dto.spid = u.getSpid(); dto.signalId = u.getSignalId(); dto.rawValue = u.getRawValue();
            dto.rawId = u.getRawId(); dto.rawName = u.getRawName();
            dto.rawCommand = u.getSourceCommand(); dto.reason = u.getReason(); dto.unit = u.getUnit();
            dto.signalName = u.getRawName(); dto.mappingConfidence = "UNKNOWN";
            dto.hasSignalIdentity = hasSignalIdentity(u.getSpid(), u.getSignalId(), u.getRawId());
            dto.isDeviceOnly = !Boolean.TRUE.equals(dto.hasSignalIdentity) && isGetLoginInfo(u.getSourceCommand());
            dto.legacyData = false;
            dto.source = u.getSourceCommand() != null ? u.getSourceCommand() : "unmapped_observation";
            if (Boolean.TRUE.equals(dto.isDeviceOnly)) {
                dto.mappingStatus = "DEVICE_ONLY";
                dto.reason = "DEVICE_DISCOVERED_WAIT_GET_DATA";
                dto.observationType = "DEVICE_ONLY";
            } else if (!Boolean.TRUE.equals(dto.hasSignalIdentity)) {
                dto.mappingStatus = "SIGNAL_PENDING";
                dto.reason = u.getReason() != null ? u.getReason() : "SIGNAL_PENDING";
                dto.observationType = "SIGNAL_PENDING";
            } else {
                String identity = firstNonBlank(u.getSignalId(), u.getSpid(), u.getRawId());
                EStoneIIMappingResult mapped = mappingService.resolveRealtime(u.getFsuId(), u.getDeviceId(), u.getDeviceCode(),
                        firstNonBlank(u.getSpid(), identity), identity, u.getRawValue());
                if (mapped.mapped() && mapped.reason() == null) {
                    dto.signalName = mapped.signalName();
                    dto.signalType = mapped.signalType();
                    dto.unit = mapped.unit();
                    dto.valueMeaning = mapped.valueMeaning();
                    dto.mappingStatus = mapped.mappingStatus();
                    dto.mappingConfidence = mapped.mappingConfidence();
                    dto.templateVariant = mapped.templateVariant();
                    dto.needRealDataConfirm = mapped.needRealDataConfirm();
                    dto.verifiedByRealData = mapped.verifiedByRealData();
                    dto.source = mapped.source();
                    dto.observationType = "MAPPED_FROM_OBSERVATION";
                } else {
                    dto.mappingStatus = "UNKNOWN_EVENT_ID".equalsIgnoreCase(u.getReason()) ? "UNKNOWN_EVENT_ID" : "UNMAPPED";
                    dto.source = "unmapped_observation";
                    dto.observationType = "UNMAPPED_SIGNAL";
                }
            }
            dto.firstSeenAt = u.getFirstSeenAt() != null ? u.getFirstSeenAt().toString() : null;
            dto.lastSeenAt = u.getLastSeenAt() != null ? u.getLastSeenAt().toString() : null;
            dto.seenCount = u.getSeenCount() != null ? u.getSeenCount() : 0;
            list.add(dto);
        }
        return ApiResponse.success(list);
    }

    // ===== 8. Thresholds =====
    @GetMapping("/thresholds")
    public ApiResponse<List<ThresholdDto>> getThresholds() {
        return ApiResponse.success(List.of());
    }

    // ===== helpers =====

    private static final List<String> KNOWN_DEVICES = List.of(
            "51051241900002",
            "51051241820004",
            "51051241830004",
            "51051241840004",
            "51051240600004",
            "51051240700002",
            "51051243812345");

    private static final Map<String, String> DEVICE_NAMES = Map.of(
            "51051241900002", "CommState",
            "51051241820004", "Smoke",
            "51051241830004", "TempHumidity",
            "51051241840004", "WaterLeak",
            "51051240600004", "Power",
            "51051240700002", "Battery1",
            "51051243812345", "FSU");

    private static final Map<String, String> DEVICE_TYPE_NAMES = Map.of(
            "06", "D类开关电源",
            "07", "D类蓄电池组",
            "18", "D类机房/基站环境",
            "19", "D类监控设备",
            "38", "D类智能动环监控设备");

    private FsuSummary buildFsuSummary(BInterfaceFsuStatusEntity s) {
        FsuSummary fs = new FsuSummary();
        fs.id = s.getId(); fs.fsuCode = s.getFsuCode(); fs.fsuId = s.getFsuCode();
        fs.onlineStatus = s.getOnlineStatus(); fs.loginStatus = s.getLoginStatus();
        fs.sessionId = s.getSessionId(); fs.statusDetail = s.getStatusDetail();
        fs.lastLoginTime = s.getLastLoginTime() != null ? s.getLastLoginTime().toString() : null;
        fs.lastHeartbeatTime = s.getLastHeartbeat() != null ? s.getLastHeartbeat().toString() : null;
        fs.updatedAt = s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null;
        return fs;
    }

    private void copyFsuFields(FsuSummary src, FsuDetail dst) {
        dst.id = src.id; dst.fsuCode = src.fsuCode; dst.fsuId = src.fsuId; dst.stationName = src.stationName;
        dst.onlineStatus = src.onlineStatus; dst.loginStatus = src.loginStatus; dst.sessionId = src.sessionId;
        dst.fsuIp = src.fsuIp; dst.scIp = src.scIp; dst.macId = src.macId; dst.version = src.version;
        dst.lastLoginTime = src.lastLoginTime; dst.lastHeartbeatTime = src.lastHeartbeatTime;
        dst.updatedAt = src.updatedAt; dst.deviceCount = src.deviceCount;
        dst.mappedPointCount = src.mappedPointCount; dst.unmappedDeviceCount = src.unmappedDeviceCount;
        dst.statusDetail = src.statusDetail;
    }

    private List<DeviceInfo> buildKnownDevices(String fsuCode) {
        dictionaryImportService.importIfNeeded();
        List<DeviceInfo> list = new ArrayList<>();
        Map<String, DeviceInfo> byDevice = new LinkedHashMap<>();
        List<DeviceSignalCandidateEntity> candidates = candidateRepo.findByFsuIdOrderByDeviceIdAscSignalIdAsc(fsuCode);
        for (DeviceSignalCandidateEntity candidate : candidates) {
            String devId = candidate.getDeviceId();
            if (devId == null || devId.isBlank() || byDevice.containsKey(devId)) continue;
            DeviceInfo di = new DeviceInfo(); di.fsuCode = fsuCode; di.deviceId = devId;
            di.deviceCode = candidate.getDeviceCode();
            di.deviceName = candidate.getDeviceName();
            di.deviceType = candidate.getTowerDeviceType();
            di.source = "eStoneII_device_signal_candidate";
            di.mappingStatus = candidate.getMappingStatus();
            di.signalCount = (int) candidates.stream().filter(c -> devId.equals(c.getDeviceId())).count();
            byDevice.put(devId, di);
        }
        list.addAll(byDevice.values());
        return list;
    }

    private List<UnmappedSignalDto> buildAssumedDClassSignals(String fsuCode) {
        List<UnmappedSignalDto> list = new ArrayList<>();
        for (String devId : KNOWN_DEVICES) {
            String typeCode = inferDeviceTypeCode(devId);
            String deviceCode = "4" + typeCode;
            for (SignalDefinition signal : loadDClassSignalsForType(typeCode)) {
                UnmappedSignalDto dto = new UnmappedSignalDto();
                dto.fsuCode = fsuCode;
                dto.deviceId = devId;
                dto.deviceCode = deviceCode;
                dto.spid = signal.signalId;
                dto.signalId = signal.signalId;
                dto.signalName = signal.signalName;
                dto.signalType = signal.signalType;
                dto.unit = signal.unit;
                dto.reason = "assumed_enabled_by_d_class_dictionary";
                dto.source = "d-class-dictionary-assumption";
                dto.mappingStatus = "assumed_enabled";
                dto.seenCount = 1;
                list.add(dto);
            }
        }
        return list;
    }

    private static String inferDeviceTypeCode(String deviceId) {
        if (deviceId == null || deviceId.length() < 9) return "00";
        return deviceId.substring(7, 9);
    }

    private static Optional<SignalDefinition> findDClassSignal(String signalId) {
        if (signalId == null || signalId.length() < 4 || !signalId.startsWith("04")) {
            return Optional.empty();
        }
        return loadDClassSignalsForType(signalId.substring(2, 4)).stream()
                .filter(s -> signalId.equals(s.signalId))
                .findFirst();
    }

    private static List<SignalDefinition> loadDClassSignalsForType(String deviceTypeCode) {
        String typeCode = deviceTypeCode != null && deviceTypeCode.length() == 1 ? "0" + deviceTypeCode : deviceTypeCode;
        if (typeCode == null || typeCode.isBlank()) return List.of();
        List<SignalDefinition> result = new ArrayList<>();
        for (SignalDefinition signal : loadDClassSignalDictionary()) {
            if (signal.signalId != null
                    && signal.signalId.length() >= 4
                    && signal.signalId.startsWith("04")
                    && typeCode.equals(signal.signalId.substring(2, 4))) {
                result.add(signal);
            }
        }
        return result;
    }

    private static List<SignalDefinition> loadDClassSignalDictionary() {
        List<Path> candidates = List.of(
                Path.of("docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv"),
                Path.of("../docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv"),
                Path.of("/home/tom/桌面/FSU/docs/mapping/dry-run/standard-signal-dictionary-dry-run.csv")
        );
        Path found = candidates.stream().filter(Files::exists).findFirst().orElse(null);
        if (found == null) return List.of();

        List<SignalDefinition> result = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(found, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (header == null) return List.of();
            String[] columns = parseCsvLine(header);
            Map<String, Integer> index = new HashMap<>();
            for (int i = 0; i < columns.length; i++) {
                index.put(columns[i].replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT), i);
            }
            Integer signalIdIdx = index.get("signal_id");
            Integer signalNameIdx = index.get("signal_name");
            Integer signalTypeIdx = index.get("signal_type");
            Integer unitIdx = index.get("unit");
            Integer stationTypeIdx = index.get("station_type");
            if (signalIdIdx == null) return List.of();

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] values = parseCsvLine(line);
                String stationType = value(values, stationTypeIdx);
                String signalId = value(values, signalIdIdx);
                if (!"D".equals(stationType) || signalId == null || !signalId.startsWith("04")) continue;
                result.add(new SignalDefinition(
                        signalId,
                        value(values, signalNameIdx),
                        normalizeSignalType(value(values, signalTypeIdx)),
                        value(values, unitIdx)
                ));
            }
        } catch (IOException ignored) {
            return List.of();
        }
        return result;
    }

    private static String normalizeSignalType(String raw) {
        if ("遥测".equals(raw)) return "AI";
        if ("遥信".equals(raw)) return "DI";
        if ("遥控".equals(raw)) return "DO";
        if ("遥调".equals(raw)) return "AO";
        return raw;
    }

    private static String value(String[] values, Integer index) {
        if (index == null || index < 0 || index >= values.length) return "";
        return values[index] != null ? values[index].trim() : "";
    }

    private static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (ch == ',' && !inQuote) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values.toArray(String[]::new);
    }

    private static class SignalDefinition {
        final String signalId;
        final String signalName;
        final String signalType;
        final String unit;

        SignalDefinition(String signalId, String signalName, String signalType, String unit) {
            this.signalId = signalId;
            this.signalName = signalName;
            this.signalType = signalType;
            this.unit = unit;
        }
    }

    private List<MessageLogSummary> buildRecentMessages(String fsuCode) {
        var page = msgLogService.query(null, null, fsuCode, null, PageRequest.of(0, 5));
        return page.getContent().stream().map(m -> {
            MessageLogSummary s = new MessageLogSummary();
            s.id = m.getId(); s.direction = m.getDirection(); s.command = m.getCommand();
            s.fsuCode = m.getFsuCode(); s.messageType = m.getMessageType();
            s.rawMessageLength = m.getRawMessage() != null ? m.getRawMessage().length() : 0;
            s.rawMessagePreview = m.getRawMessage() != null && m.getRawMessage().length() > 200
                    ? m.getRawMessage().substring(0, 200) + "..." : m.getRawMessage();
            s.createdAt = m.getCreatedAt() != null ? m.getCreatedAt().toString() : null;
            return s;
        }).toList();
    }

    private int knownDeviceCount(String fsuCode) {
        dictionaryImportService.importIfNeeded();
        List<DeviceSignalCandidateEntity> candidates = candidateRepo.findByFsuIdOrderByDeviceIdAscSignalIdAsc(fsuCode);
        return (int) candidates.stream().map(DeviceSignalCandidateEntity::getDeviceId).filter(Objects::nonNull).distinct().count();
    }

    private boolean canAccessFsu(String fsuCode) {
        if (fsuCode == null || fsuCode.isBlank()) return false;
        return dataScopeService.filterByFsuScope(fsuStatusRepo.findAll(), BInterfaceFsuStatusEntity::getFsuCode)
                .stream()
                .anyMatch(e -> fsuCode.equals(e.getFsuCode()));
    }

    private static boolean hasSignalIdentity(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return true;
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private static boolean isGetLoginInfo(String source) {
        return source != null && "GET_LOGININFO".equalsIgnoreCase(source.trim());
    }

    private static boolean isLegacyRealtimePointCode(String pointCode) {
        return pointCode == null || !pointCode.trim().matches("\\d+");
    }

    private Map<Long, String> fsuCodeByDbId() {
        Map<Long, String> map = new HashMap<>();
        for (FsuDeviceEntity fsu : fsuDeviceRepo.findAll()) {
            if (fsu.getId() != null && fsu.getFsuCode() != null) map.put(fsu.getId(), fsu.getFsuCode());
        }
        return map;
    }

    private void applyRealtimeMapping(RealtimePointDto dto, EStoneIIMappingResult mapped) {
        dto.deviceId = mapped.deviceId();
        dto.deviceCode = mapped.deviceCode();
        dto.deviceName = mapped.deviceName();
        dto.spid = mapped.spid();
        dto.signalId = mapped.signalId();
        dto.signalName = mapped.signalName();
        dto.signalType = mapped.signalType();
        dto.signalCategory = mapped.signalCategory();
        dto.unit = mapped.unit();
        dto.valueMeaning = mapped.valueMeaning();
        dto.mappingStatus = mapped.mappingStatus();
        dto.mappingConfidence = mapped.mappingConfidence();
        dto.templateVariant = mapped.templateVariant();
        dto.needRealDataConfirm = mapped.needRealDataConfirm();
        dto.verifiedByRealData = mapped.verifiedByRealData();
        dto.derived = mapped.derived();
        dto.source = mapped.source();
    }

    private void applyAlarmMapping(AlarmDto dto, EStoneIIMappingResult mapped) {
        dto.deviceId = mapped.deviceId() != null ? mapped.deviceId() : dto.deviceId;
        dto.deviceCode = mapped.deviceCode();
        dto.deviceName = mapped.deviceName();
        dto.signalId = mapped.signalId();
        dto.spid = mapped.spid();
        dto.signalName = mapped.signalName();
        dto.eventId = mapped.eventId();
        dto.eventName = mapped.eventName();
        dto.alarmMeaning = mapped.alarmMeaning();
        dto.eventSeverity = mapped.eventSeverity();
        dto.mappingStatus = mapped.mappingStatus();
        dto.mappingConfidence = mapped.mappingConfidence();
        dto.templateVariant = mapped.templateVariant();
        dto.needRealDataConfirm = mapped.needRealDataConfirm();
        dto.verifiedByRealData = mapped.verifiedByRealData();
        dto.source = mapped.source();
    }
}
