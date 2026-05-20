package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResolver;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResult;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GET_ACTIVEALARM 活动告警查询服务 (2024 标准, Code=603)。
 *
 * <p>BIF-P4-016: SC 主动查询 FSU 当前活动告警快照。只读，不修改本地告警状态。</p>
 */
@Service
public class GetActiveAlarmService {

    private static final Logger log = LoggerFactory.getLogger(GetActiveAlarmService.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;

    public GetActiveAlarmService(FsuServiceClient fsuServiceClient, FsuEndpointResolver fsuEndpointResolver) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
    }

    public GetActiveAlarmResult execute(String fsuCode, String fsuServiceUrl) {
        if (fsuCode == null || fsuCode.trim().isEmpty())
            return GetActiveAlarmResult.fail("2001", "缺少 SUID");
        String suid = fsuCode.trim();

        String url = fsuServiceUrl;
        if (url == null && fsuEndpointResolver != null) {
            FsuEndpointResult ep = fsuEndpointResolver.resolve(suid);
            if (!ep.isSuccess()) return GetActiveAlarmResult.fail(ep.getResultCode(), ep.getResultDesc(), suid);
            url = ep.getServiceUrl();
        }

        try {
            FsuServiceRequest req = FsuServiceRequest.builder()
                    .fsuCode(suid).serviceUrl(url)
                    .pkType(BInterfacePkType.GET_ACTIVEALARM)
                    .infoXml("<SUID>" + suid + "</SUID>").build();

            FsuServiceResponse resp = fsuServiceClient.call(req);
            if (!resp.isSuccess())
                return GetActiveAlarmResult.fail(resp.getResultCode(),
                        resp.getResultDesc() != null ? resp.getResultDesc() : "FSU 查询失败", suid);

            XmlDataModel xd = resp.getXmlData();
            if (xd == null || xd.isEmpty())
                return GetActiveAlarmResult.success(suid, List.of(), 0, 0, 0, resp.isRealCall());

            List<Map<String, String>> items = xd.getItems();
            List<GetActiveAlarmResult.ActiveAlarmItem> alarms = new ArrayList<>();
            int parsed = 0, invalid = 0;

            if (items != null) {
                for (Map<String, String> item : items) {
                    try {
                        alarms.add(parseAlarmItem(item, suid));
                        parsed++;
                    } catch (Exception e) {
                        invalid++;
                        log.debug("TAlarm 解析失败: {}", e.getMessage());
                    }
                }
            }

            return GetActiveAlarmResult.success(suid, alarms, items != null ? items.size() : 0, parsed, invalid, resp.isRealCall());
        } catch (Exception e) {
            log.error("GET_ACTIVEALARM 异常: suid={}", suid, e);
            return GetActiveAlarmResult.fail("5001", "查询异常", suid);
        }
    }

    private GetActiveAlarmResult.ActiveAlarmItem parseAlarmItem(Map<String, String> item, String suid) {
        return new GetActiveAlarmResult.ActiveAlarmItem(
                getField(item, "SerialNo"), suid,
                getField(item, "DeviceID"), getField(item, "SPID"),
                parseTime(getField(item, "StartTime")),
                parseTime(getField(item, "EndTime")),
                getField(item, "TriggerVal"),
                getField(item, "AlarmLevel"),
                getField(item, "AlarmFlag"),
                getField(item, "AlarmDesc"),
                getField(item, "AlarmFriDesc"));
    }

    private String getField(Map<String, String> m, String k) {
        if (m == null || k == null) return null;
        String v = m.get(k);
        if (v != null) return v.trim();
        for (Map.Entry<String, String> e : m.entrySet())
            if (e.getKey().equalsIgnoreCase(k)) return e.getValue() != null ? e.getValue().trim() : null;
        return null;
    }

    private LocalDateTime parseTime(String v) {
        if (v == null || v.trim().isEmpty()) return null;
        try { return LocalDateTime.parse(v.trim(), TIME_FMT); } catch (DateTimeParseException e) { return null; }
    }
}
