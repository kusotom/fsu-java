package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.xml.XmlDataBuilder;
import com.dcim.platform.module.binterface.xml.XmlDataModel;

import java.util.ArrayList;
import java.util.List;

/**
 * B接口命令处理结果。
 *
 * 由 CommandHandler.handle() 返回，经 CommandDispatcher 封装后供
 * ScServiceController 构造 SOAP 响应。包含：
 * <ul>
 *   <li>success — 处理是否成功</li>
 *   <li>resultCode — B接口 ResultCode 值</li>
 *   <li>resultDesc — 可读描述</li>
 *   <li>pkType — 响应命令码</li>
 *   <li>responseInfoXml — Info 字段 XML 内容（不含根元素）</li>
 *   <li>responseXmlData — 响应的结构化 xmlData 模型</li>
 *   <li>implemented — 该命令是否已实现业务逻辑</li>
 *   <li>errors — 错误信息列表</li>
 * </ul>
 *
 * 可通过 {@link #toInfoXml()} 和 {@link #toXmlDataXml()} 转换为
 * SoapMessageHandler.buildResponse() 所需的字符串。
 */
public class CommandResult {

    private boolean success;

    private String resultCode;

    private String resultDesc;

    private BInterfacePkType pkType;

    private String responseInfoXml;

    private XmlDataModel responseXmlData;

    private boolean implemented;

    private final List<String> errors = new ArrayList<>();

    private static final XmlDataBuilder xmlDataBuilder = new XmlDataBuilder();

    public CommandResult() {
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建未实现占位结果。
     */
    public static CommandResult notImplemented(BInterfacePkType pkType) {
        CommandResult result = new CommandResult();
        result.pkType = pkType;
        result.success = false;
        result.resultCode = "0";
        result.resultDesc = "Handler not implemented (BIF-P1 pending)";
        result.implemented = false;
        result.responseInfoXml = "<Result>0</Result>";
        return result;
    }

    /**
     * 创建成功结果。
     */
    public static CommandResult success(BInterfacePkType pkType) {
        CommandResult result = new CommandResult();
        result.pkType = pkType;
        result.success = true;
        result.resultCode = "1";
        result.resultDesc = "OK";
        result.implemented = true;
        result.responseInfoXml = "<Result>1</Result>";
        return result;
    }

    /**
     * 创建错误结果。
     */
    public static CommandResult error(BInterfacePkType pkType, String resultCode, String message) {
        CommandResult result = new CommandResult();
        result.pkType = pkType;
        result.success = false;
        result.resultCode = resultCode;
        result.resultDesc = message;
        result.implemented = false;
        result.responseInfoXml = "<Result>" + resultCode + "</Result>";
        result.addError(message);
        return result;
    }

    // ==================== 转换为 XML ====================

    /**
     * 获取 Info 字段的 XML 字符串（用于 SoapMessageHandler.buildResponse）。
     */
    public String toInfoXml() {
        return responseInfoXml;
    }

    /**
     * 获取 xmlData 字段的 XML 字符串（用于 SoapMessageHandler.buildResponse）。
     * 自动调用 XmlDataBuilder 将模型转为 XML。
     */
    public String toXmlDataXml() {
        if (responseXmlData == null || responseXmlData.isEmpty()) {
            return null;
        }
        String built = xmlDataBuilder.build(responseXmlData);
        return built.isEmpty() ? null : built;
    }

    // ==================== 便捷判断 ====================

    public boolean isSuccess() {
        return success;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    // ==================== Getter / Setter ====================

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultDesc() {
        return resultDesc;
    }

    public void setResultDesc(String resultDesc) {
        this.resultDesc = resultDesc;
    }

    public BInterfacePkType getPkType() {
        return pkType;
    }

    public void setPkType(BInterfacePkType pkType) {
        this.pkType = pkType;
    }

    public String getResponseInfoXml() {
        return responseInfoXml;
    }

    public void setResponseInfoXml(String responseInfoXml) {
        this.responseInfoXml = responseInfoXml;
    }

    public XmlDataModel getResponseXmlData() {
        return responseXmlData;
    }

    public void setResponseXmlData(XmlDataModel responseXmlData) {
        this.responseXmlData = responseXmlData;
    }

    public boolean isImplemented() {
        return implemented;
    }

    public void setImplemented(boolean implemented) {
        this.implemented = implemented;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }

    @Override
    public String toString() {
        return "CommandResult{success=" + success + ", resultCode=" + resultCode
                + ", implemented=" + implemented + ", pkType=" + pkType + "}";
    }
}
