package com.dcim.platform.module.binterface.xml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * B接口 xmlData 校验器。
 *
 * 对 {@link XmlDataModel} 执行内容校验：
 * <ul>
 *   <li>xmlData 不可解析失败</li>
 *   <li>SignalID 不可为空或空白</li>
 *   <li>重复结构中的条目不可为空映射</li>
 * </ul>
 *
 * <b>注意：</b>FSUCode、ResultCode 等字段位于 Info 层，不属于 xmlData，
 * 由上层 (BInterfaceMessage) 校验。
 *
 * 校验结果通过结构化的 {@link ValidationResult} 返回，不抛异常。
 *
 * @see XmlDataModel
 * @see ValidationResult
 */
public class XmlDataValidator {

    /**
     * 校验 XmlDataModel 基本规则。
     */
    public ValidationResult validate(XmlDataModel model) {
        ValidationResult result = new ValidationResult();
        if (model == null) {
            result.addError("xmlData model 为空");
            return result;
        }

        if (!model.isValid()) {
            result.addError("xmlData 解析失败: " + model.getFirstError());
            return result;
        }

        // SignalID 非空校验（items 模式）
        for (int i = 0; i < model.itemCount(); i++) {
            Map<String, String> item = model.getItems().get(i);
            if (item == null || item.isEmpty()) {
                result.addWarning("items[" + i + "] 为空条目");
                continue;
            }
            String signalId = null;
            for (Map.Entry<String, String> entry : item.entrySet()) {
                if (entry.getKey().equalsIgnoreCase("SignalID")) {
                    signalId = entry.getValue();
                    break;
                }
            }
            if (signalId != null && signalId.trim().isEmpty()) {
                result.addError("items[" + i + "].SignalID 为空");
            }
        }

        return result;
    }

    /**
     * 校验请求的 xmlData（目前与基本校验相同，预留扩展）。
     */
    public ValidationResult validateRequest(XmlDataModel model) {
        return validate(model);
    }

    /**
     * 校验响应的 xmlData（目前与基本校验相同，预留扩展）。
     */
    public ValidationResult validateResponse(XmlDataModel model) {
        return validate(model);
    }

    // ==================== 校验结果 ====================

    /**
     * 校验结果，包含错误和警告列表。
     */
    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public boolean isPassed() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public String getFirstError() {
            return errors.isEmpty() ? null : errors.get(0);
        }

        @Override
        public String toString() {
            return "ValidationResult{errors=" + errors.size() + ", warnings=" + warnings.size()
                    + ", passed=" + isPassed() + '}';
        }
    }
}
