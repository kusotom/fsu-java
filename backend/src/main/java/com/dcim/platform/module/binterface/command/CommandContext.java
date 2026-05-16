package com.dcim.platform.module.binterface.command;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.xml.XmlDataModel;

import java.util.HashMap;
import java.util.Map;

/**
 * B接口命令上下文。
 *
 * 承载一次命令处理所需的所有上下文信息：
 * <ul>
 *   <li>pkType — 命令码</li>
 *   <li>soapMessage — 解析后的 SOAP 消息（含 Info 原始 XML）</li>
 *   <li>xmlData — 由 XmlDataParser 解析的结构化数据模型</li>
 *   <li>rawSoap — 原始 SOAP XML 字符串</li>
 *   <li>rawXmlData — 原始 xmlData XML 字符串</li>
 *   <li>attributes — 扩展属性映射</li>
 * </ul>
 *
 * 由 CommandDispatcher 在分发前构造，传递给 CommandHandler。
 */
public class CommandContext {

    private final BInterfacePkType pkType;

    private final BInterfaceMessage soapMessage;

    private final XmlDataModel xmlData;

    private final String rawSoap;

    private final String rawXmlData;

    private final Map<String, Object> attributes = new HashMap<>();

    public CommandContext(BInterfacePkType pkType, BInterfaceMessage soapMessage,
                          XmlDataModel xmlData, String rawSoap, String rawXmlData) {
        this.pkType = pkType;
        this.soapMessage = soapMessage;
        this.xmlData = xmlData;
        this.rawSoap = rawSoap;
        this.rawXmlData = rawXmlData;
    }

    public BInterfacePkType getPkType() {
        return pkType;
    }

    public BInterfaceMessage getSoapMessage() {
        return soapMessage;
    }

    public XmlDataModel getXmlData() {
        return xmlData;
    }

    public String getRawSoap() {
        return rawSoap;
    }

    public String getRawXmlData() {
        return rawXmlData;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        return (T) attributes.get(key);
    }

    @Override
    public String toString() {
        return "CommandContext{pkType=" + pkType
                + ", xmlData=" + (xmlData != null ? xmlData.getRootName() : "null")
                + "}";
    }
}
