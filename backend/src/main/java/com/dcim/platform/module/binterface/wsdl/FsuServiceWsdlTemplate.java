package com.dcim.platform.module.binterface.wsdl;

import com.dcim.platform.module.binterface.util.WsdlLoader;

/**
 * FSUService WSDL 模板
 *
 * 平台作为 SC 客户端调用 FSUService 时所需的 WSDL 模板。
 * WSDL 内容从 classpath:wsdl/fsu-service.wsdl 加载。
 *
 * 对应 B接口 2016 WSDL 规范：
 * - RPC style, soapenc:encoded
 * - 单一 invoke(xmlData) 操作
 * - 命名空间：http://FSUService.chinatowercom.com
 */
public class FsuServiceWsdlTemplate {

    /**
     * 获取 FSUService 的 WSDL 模板字符串
     */
    public String getTemplate() {
        return WsdlLoader.load("wsdl/fsu-service.wsdl");
    }
}
