package com.dcim.platform.module.binterface.service;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.safety.ConfirmationTokenService;
import com.dcim.platform.module.binterface.safety.SetCommandAuditService;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyDecision;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyGate;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResolver;
import com.dcim.platform.module.binterface.service.fsu.FsuEndpointResult;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceClient;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceRequest;
import com.dcim.platform.module.binterface.service.fsu.FsuServiceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * SET_TIME 时间同步服务 (2024 标准, Code=901)。
 *
 * <p>BIF-P4-SAFE-002: 接入 SetCommandSafetyGate 统一安全门禁。
 * 会修改 FSU 设备时间，需安全门禁和人工确认。</p>
 */
@Service
public class SetTimeService {

    private static final Logger log = LoggerFactory.getLogger(SetTimeService.class);
    static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FsuServiceClient fsuServiceClient;
    private final FsuEndpointResolver fsuEndpointResolver;
    private final SetCommandSafetyGate safetyGate;
    private final ConfirmationTokenService tokenService;
    private final SetCommandAuditService auditService;

    @Autowired
    public SetTimeService(FsuServiceClient fsuServiceClient, FsuEndpointResolver fsuEndpointResolver) {
        this(fsuServiceClient, fsuEndpointResolver, null, null, null);
    }

    public SetTimeService(FsuServiceClient fsuServiceClient,
                          FsuEndpointResolver fsuEndpointResolver,
                          SetCommandSafetyGate safetyGate) {
        this(fsuServiceClient, fsuEndpointResolver, safetyGate, null, null);
    }

    public SetTimeService(FsuServiceClient fsuServiceClient,
                          FsuEndpointResolver fsuEndpointResolver,
                          SetCommandSafetyGate safetyGate,
                          ConfirmationTokenService tokenService,
                          SetCommandAuditService auditService) {
        this.fsuServiceClient = fsuServiceClient;
        this.fsuEndpointResolver = fsuEndpointResolver;
        this.safetyGate = safetyGate;
        this.tokenService = tokenService;
        this.auditService = auditService;
    }

    /** 完整安全流程（token验证→门禁→审计→执行）。 */
    public SetTimeResult executeWithSafety(SetTimeExecutionRequest req) {
        String suid = req.getSuid();
        if (suid == null || suid.trim().isEmpty()) return SetTimeResult.fail("2001", "缺少 SUID");
        suid = suid.trim();

        // 1. Token 验证
        String tokenHash = null;
        boolean tokenValid = false;
        String tokenReason = null;
        if (tokenService != null && req.getConfirmationToken() != null) {
            var vr = tokenService.validate(req.getConfirmationToken(), "SET_TIME", suid);
            tokenValid = vr.isValid();
            tokenHash = vr.getTokenHash();
            if (!tokenValid) {
                tokenReason = vr.getReasonCode();
                if (auditService != null) auditService.recordRejected(
                        SetCommandSafetyDecision.builder().commandName("SET_TIME").suid(suid)
                                .reasonCode(tokenReason).reasonMessage(vr.getReasonMessage())
                                .confirmationRequired(true).confirmationProvided(true).build(),
                        suid, req.getSource(), tokenHash);
                return SetTimeResult.rejected(tokenReason, vr.getReasonMessage(), suid);
            }
        }

        // 2. Gate 判定
        if (safetyGate == null) return executeDirect(suid, req.getServiceUrl(), req.getTargetTime());
        SetCommandSafetyDecision d = safetyGate.evaluate(
                "SET_TIME", suid, tokenValid ? "valid" : req.getConfirmationToken(),
                req.isSchedulerTriggered(), req.getTargetCount(), req.isRealCallRequested());
        if (!d.isAllowed()) {
            if (auditService != null) auditService.recordRejected(d, suid, req.getSource(), tokenHash);
            return SetTimeResult.rejected(d.getReasonCode(), d.getReasonMessage(), suid);
        }

        // 3. Dry-run
        if (d.isDryRun()) {
            if (auditService != null) auditService.recordDryRun(d, suid, req.getSource(), tokenHash);
            if (req.getTargetTime() == null || req.getTargetTime().trim().isEmpty())
                return SetTimeResult.fail("2003", "缺少 StandardTime");
            LocalDateTime dt;
            try { dt = LocalDateTime.parse(req.getTargetTime().trim(), TIME_FORMAT); }
            catch (DateTimeParseException e) { return SetTimeResult.fail("2003", "时间格式非法", suid); }
            return SetTimeResult.dryRunSuccess(suid, dt);
        }

        // 4. Allowed — execute
        if (auditService != null) auditService.recordAllowed(d, suid, req.getSource(), tokenHash);
        SetTimeResult result = executeDirect(suid, req.getServiceUrl(), req.getTargetTime());

        // 5. Consume token on success
        if (result.isSuccess() && tokenService != null && tokenHash != null)
            tokenService.markUsed(tokenHash);

        return result;
    }

    /** 绕过门禁直接执行（仅 executeWithSafety allowed 路径调用）。 */
    private SetTimeResult executeDirect(String suid, String serviceUrl, String standardTime) {
        LocalDateTime sentTime;
        try { sentTime = LocalDateTime.parse(standardTime.trim(), TIME_FORMAT); }
        catch (DateTimeParseException e) { return SetTimeResult.fail("2003", "时间格式非法", suid); }

        String url = serviceUrl;
        if (url == null && fsuEndpointResolver != null) {
            FsuEndpointResult ep = fsuEndpointResolver.resolve(suid);
            if (!ep.isSuccess()) return SetTimeResult.fail(ep.getResultCode(), ep.getResultDesc(), suid);
            url = ep.getServiceUrl();
        }
        try {
            FsuServiceRequest r = FsuServiceRequest.builder()
                    .fsuCode(suid).serviceUrl(url).pkType(BInterfacePkType.SET_TIME)
                    .infoXml("<SUID>" + suid + "</SUID><TTime>" + standardTime.trim() + "</TTime>").build();
            FsuServiceResponse resp = fsuServiceClient.call(r);
            if (!resp.isSuccess()) return SetTimeResult.fail(resp.getResultCode(),
                    resp.getResultDesc() != null ? resp.getResultDesc() : "FSU 调用失败", suid);
            return SetTimeResult.success(suid, sentTime);
        } catch (Exception e) {
            log.error("SET_TIME 异常: suid={}", suid, e);
            return SetTimeResult.fail("5001", "时间同步异常: " + e.getMessage(), suid);
        }
    }

    public SetTimeResult execute(String fsuCode, String fsuServiceUrl, String standardTime,
                                  String confirmationToken, boolean schedulerTriggered,
                                  int targetCount, boolean realCallRequested) {
        if (fsuCode == null || fsuCode.trim().isEmpty())
            return SetTimeResult.fail("2001", "缺少 SUID");
        String suid = fsuCode.trim();

        if (safetyGate != null) {
            SetCommandSafetyDecision d = safetyGate.evaluate(
                    "SET_TIME", suid, confirmationToken, schedulerTriggered, targetCount, realCallRequested);
            if (!d.isAllowed()) {
                log.warn("SET_TIME 门禁拒绝: suid={}, reason={}", suid, d.getReasonCode());
                return SetTimeResult.rejected(d.getReasonCode(), d.getReasonMessage(), suid);
            }
            if (d.isDryRun()) {
                if (standardTime == null || standardTime.trim().isEmpty())
                    return SetTimeResult.fail("2003", "缺少 StandardTime");
                LocalDateTime dt;
                try { dt = LocalDateTime.parse(standardTime.trim(), TIME_FORMAT); }
                catch (DateTimeParseException e) { return SetTimeResult.fail("2003", "时间格式非法", suid); }
                return SetTimeResult.dryRunSuccess(suid, dt);
            }
        }

        if (standardTime == null || standardTime.trim().isEmpty())
            return SetTimeResult.fail("2003", "缺少 StandardTime");
        LocalDateTime sentTime;
        try {
            sentTime = LocalDateTime.parse(standardTime.trim(), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return SetTimeResult.fail("2003", "时间格式非法: " + standardTime, suid);
        }

        String url = fsuServiceUrl;
        if (url == null && fsuEndpointResolver != null) {
            FsuEndpointResult ep = fsuEndpointResolver.resolve(suid);
            if (!ep.isSuccess()) return SetTimeResult.fail(ep.getResultCode(), ep.getResultDesc(), suid);
            url = ep.getServiceUrl();
        }

        try {
            FsuServiceRequest req = FsuServiceRequest.builder()
                    .fsuCode(suid).serviceUrl(url).pkType(BInterfacePkType.SET_TIME)
                    .infoXml("<SUID>" + suid + "</SUID><TTime>" + standardTime.trim() + "</TTime>").build();
            FsuServiceResponse resp = fsuServiceClient.call(req);
            if (!resp.isSuccess())
                return SetTimeResult.fail(resp.getResultCode(),
                        resp.getResultDesc() != null ? resp.getResultDesc() : "FSU 调用失败", suid);
            return SetTimeResult.success(suid, sentTime);
        } catch (Exception e) {
            log.error("SET_TIME 异常: suid={}", suid, e);
            return SetTimeResult.fail("5001", "时间同步异常: " + e.getMessage(), suid);
        }
    }

    /** 兼容旧调用（无门禁参数，Stub测试用）。 */
    public SetTimeResult execute(String fsuCode, String fsuServiceUrl, String standardTime) {
        return execute(fsuCode, fsuServiceUrl, standardTime, null, false, 1, false);
    }
}
