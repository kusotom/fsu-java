package com.dcim.platform.module.binterface.service.slow;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.service.GetDataResult;
import com.dcim.platform.module.binterface.service.GetDataService;
import com.dcim.platform.module.binterface.service.GetThresholdResult;
import com.dcim.platform.module.binterface.service.GetThresholdService;
import com.dcim.platform.module.binterface.service.LoginService;
import com.dcim.platform.module.resource.entity.FsuDeviceEntity;
import com.dcim.platform.module.resource.repository.FsuDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 慢数据通道轮询服务。
 *
 * <p>编排慢数据轮询任务：查询候选 FSU → 跳过 OFFLINE/未登录 → 查询轮询目标信号 →
 * 执行 GET_DATA → 可配置执行 GET_THRESHOLD。只读查询，不执行任何 SET 类命令。</p>
 *
 * <p>与 BIF-P3-004 的改进：不再传空 signalId 列表，通过 {@link SignalPollingTargetService}
 * 获取每个 FSU 的信号配置。无配置时跳过并计入 skippedNoSignalCount。</p>
 */
@Service
public class SlowDataPollingService {

    private static final Logger log = LoggerFactory.getLogger(SlowDataPollingService.class);

    private final SlowDataPollingProperties properties;
    private final FsuDeviceRepository fsuDeviceRepository;
    private final LoginService loginService;
    private final GetDataService getDataService;
    private final GetThresholdService getThresholdService;
    private final SignalPollingTargetService signalPollingTargetService;

    public SlowDataPollingService(SlowDataPollingProperties properties,
                                  FsuDeviceRepository fsuDeviceRepository,
                                  LoginService loginService,
                                  GetDataService getDataService,
                                  GetThresholdService getThresholdService,
                                  SignalPollingTargetService signalPollingTargetService) {
        this.properties = properties;
        this.fsuDeviceRepository = fsuDeviceRepository;
        this.loginService = loginService;
        this.getDataService = getDataService;
        this.getThresholdService = getThresholdService;
        this.signalPollingTargetService = signalPollingTargetService;
    }

    /**
     * 执行一次轮询。
     *
     * @return 轮询结果，永不返回 null
     */
    public SlowDataPollingResult runOnce() {
        return runOnce(LocalDateTime.now());
    }

    /**
     * 执行一次轮询（可注入时间，便于测试）。
     *
     * @param now 当前时间
     * @return 轮询结果，永不返回 null
     */
    public SlowDataPollingResult runOnce(LocalDateTime now) {
        List<String> errors = new ArrayList<>();
        List<String> processedFsuCodes = new ArrayList<>();
        int skippedOffline = 0;
        int skippedNotLoggedIn = 0;
        int skippedNoSignal = 0;
        int getDataSuccess = 0;
        int getDataFailure = 0;
        int getThresholdSuccess = 0;
        int getThresholdFailure = 0;

        try {
            List<FsuDeviceEntity> allFsuDevices = fsuDeviceRepository.findAll();
            if (allFsuDevices.isEmpty()) {
                log.debug("慢数据轮询: 无 FSU 设备");
                return buildResult(0, 0, 0, 0, 0, 0, 0, 0,
                        List.of(), List.of());
            }

            int maxFsu = Math.max(1, properties.getMaxFsuPerRun());
            int processed = 0;

            for (FsuDeviceEntity fsu : allFsuDevices) {
                if (processed >= maxFsu) {
                    log.debug("慢数据轮询: 达到单次上限 {}", maxFsu);
                    break;
                }

                String fsuCode = fsu.getFsuCode();
                try {
                    // 1. 检查在线状态
                    if (!isOnline(fsuCode)) {
                        skippedOffline++;
                        continue;
                    }

                    // 2. 检查登录状态
                    if (!loginService.isLoggedIn(fsuCode)) {
                        skippedNotLoggedIn++;
                        continue;
                    }

                    // 3. 查询 GET_DATA 轮询目标
                    List<SignalPollingTarget> dataTargets =
                            signalPollingTargetService.findTargets(fsuCode, BInterfacePkType.GET_DATA);
                    if (dataTargets.isEmpty()) {
                        skippedNoSignal++;
                    } else {
                        List<String> dataSignalIds = extractSignalIds(dataTargets);
                        boolean getDataOk = executeGetData(fsuCode, dataSignalIds, errors);
                        if (getDataOk) {
                            getDataSuccess++;
                        } else {
                            getDataFailure++;
                        }
                    }

                    // 4. 如果配置允许，查询 GET_THRESHOLD 轮询目标
                    if (properties.isPollGetThreshold()) {
                        List<SignalPollingTarget> thresholdTargets =
                                signalPollingTargetService.findTargets(fsuCode, BInterfacePkType.GET_THRESHOLD);
                        if (!thresholdTargets.isEmpty()) {
                            List<String> thresholdSignalIds = extractSignalIds(thresholdTargets);
                            boolean getThresholdOk = executeGetThreshold(fsuCode, thresholdSignalIds, errors);
                            if (getThresholdOk) {
                                getThresholdSuccess++;
                            } else {
                                getThresholdFailure++;
                            }
                        }
                    }

                    processedFsuCodes.add(fsuCode);
                    processed++;
                } catch (Exception e) {
                    log.error("慢数据轮询: FSU={} 处理异常", fsuCode, e);
                    errors.add(fsuCode + ": " + e.getMessage());
                    getDataFailure++;
                }
            }

            int scanned = Math.min(allFsuDevices.size(), maxFsu);
            return buildResult(scanned, skippedOffline, skippedNotLoggedIn, skippedNoSignal,
                    getDataSuccess, getDataFailure,
                    getThresholdSuccess, getThresholdFailure,
                    processedFsuCodes, errors);

        } catch (Exception e) {
            log.error("慢数据轮询: 整体异常", e);
            errors.add("全局异常: " + e.getMessage());
            return buildResult(0, 0, 0, 0, 0, 0, 0, 0,
                    List.of(), errors);
        }
    }

    // ==================== 内部方法 ====================

    private boolean isOnline(String fsuCode) {
        return loginService.getStatus(fsuCode)
                .map(s -> "ONLINE".equals(s.getOnlineStatus()))
                .orElse(false);
    }

    private List<String> extractSignalIds(List<SignalPollingTarget> targets) {
        return targets.stream()
                .filter(SignalPollingTarget::isEnabled)
                .map(SignalPollingTarget::getSignalId)
                .toList();
    }

    private boolean executeGetData(String fsuCode, List<String> signalIds, List<String> errors) {
        if (signalIds == null || signalIds.isEmpty()) {
            return false;
        }
        GetDataResult result = getDataService.execute(fsuCode, null, signalIds);
        if (!result.isSuccess()) {
            log.warn("慢数据轮询 GET_DATA 失败: fsuCode={}, code={}",
                    fsuCode, result.getResultCode());
            errors.add(fsuCode + " GET_DATA: " + result.getResultCode() + " " + result.getResultDesc());
            return false;
        }
        return true;
    }

    private boolean executeGetThreshold(String fsuCode, List<String> signalIds, List<String> errors) {
        if (signalIds == null || signalIds.isEmpty()) {
            return false;
        }
        GetThresholdResult result = getThresholdService.execute(fsuCode, null, signalIds);
        if (!result.isSuccess()) {
            log.warn("慢数据轮询 GET_THRESHOLD 失败: fsuCode={}, code={}",
                    fsuCode, result.getResultCode());
            errors.add(fsuCode + " GET_THRESHOLD: " + result.getResultCode() + " " + result.getResultDesc());
            return false;
        }
        return true;
    }

    private SlowDataPollingResult buildResult(int scanned, int skippedOffline, int skippedNotLoggedIn,
                                               int skippedNoSignal,
                                               int getDataOk, int getDataFail,
                                               int getThresholdOk, int getThresholdFail,
                                               List<String> processed, List<String> errors) {
        boolean realCall = properties.isAllowRealCall();
        return SlowDataPollingResult.completed(scanned, skippedOffline, skippedNotLoggedIn,
                skippedNoSignal,
                getDataOk, getDataFail, getThresholdOk, getThresholdFail,
                processed, errors, realCall);
    }
}
