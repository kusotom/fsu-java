package com.dcim.platform.module.binterface.wsdl;

import com.dcim.platform.module.binterface.util.WsdlLoader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SCService WSDL 提供接口
 *
 * 向 FSU 提供平台侧 SCService 的 WSDL 定义。
 * WSDL 内容从 classpath:wsdl/sc-service.wsdl 加载。
 *
 * 对应 B接口 2016 WSDL 规范：
 * - RPC style, soapenc:encoded
 * - 单一 invoke(xmlData) 操作
 * - 命名空间：http://SCService.chinatowercom.com
 */
@RestController
@RequestMapping("/api/b-interface/wsdl")
public class ScServiceWsdlController {

    @GetMapping(value = "/sc-service", produces = MediaType.TEXT_XML_VALUE)
    public String getScServiceWsdl() {
        return WsdlLoader.load("wsdl/sc-service.wsdl");
    }
}
