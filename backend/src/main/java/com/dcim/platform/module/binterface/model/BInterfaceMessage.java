package com.dcim.platform.module.binterface.model;

/**
 * B接口SOAP/XML报文封装模型
 *
 * 承载B接口标准报文结构:
 * - Request / Response (SOAP Body 根元素)
 * - PK_Type (命令码)
 * - Info (元数据，XML 字符串)
 * - xmlData (业务数据，XML 字符串)
 *
 * Info 和 xmlData 以原始 XML 字符串形式存储。
 * 子字段解析由 XmlDataModel (BIF-P0-003) 负责。
 */
public class BInterfaceMessage {

    private String request;
    private String response;
    private BInterfacePkType pkType;
    private String info;
    private String xmlData;

    /** BIF-P4-011: PK_Type 解析描述符（双格式兼容）。 */
    private PkTypeDescriptor pkTypeDescriptor;

    public BInterfaceMessage() {
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public BInterfacePkType getPkType() {
        return pkType;
    }

    public void setPkType(BInterfacePkType pkType) {
        this.pkType = pkType;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getXmlData() {
        return xmlData;
    }

    public void setXmlData(String xmlData) {
        this.xmlData = xmlData;
    }

    public PkTypeDescriptor getPkTypeDescriptor() {
        return pkTypeDescriptor;
    }

    public void setPkTypeDescriptor(PkTypeDescriptor pkTypeDescriptor) {
        this.pkTypeDescriptor = pkTypeDescriptor;
    }
}
