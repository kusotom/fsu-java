package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.util.WsdlLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WSDL 端点与加载测试。
 *
 * 验证：
 * 1. WSDL 文件可从 classpath 加载
 * 2. WSDL 内容符合 B接口 2016 规范
 */
class WsdlEndpointTest {

    @Test
    void shouldLoadScServiceWsdl() {
        String wsdl = WsdlLoader.load("wsdl/sc-service.wsdl");
        assertNotNull(wsdl);
        assertFalse(wsdl.isEmpty());
        assertTrue(wsdl.contains("SCService"));
        assertTrue(wsdl.contains("http://SCService.chinatowercom.com"));
        assertTrue(wsdl.contains("invoke"));
        assertTrue(wsdl.contains("soapenc:"));
        assertTrue(wsdl.contains("use=\"encoded\""));
    }

    @Test
    void shouldLoadFsuServiceWsdl() {
        String wsdl = WsdlLoader.load("wsdl/fsu-service.wsdl");
        assertNotNull(wsdl);
        assertFalse(wsdl.isEmpty());
        assertTrue(wsdl.contains("FSUService"));
        assertTrue(wsdl.contains("http://FSUService.chinatowercom.com"));
        assertTrue(wsdl.contains("invoke"));
    }

    @Test
    void shouldHaveUniqueWsdlNamespaces() {
        String scWsdl = WsdlLoader.load("wsdl/sc-service.wsdl");
        String fsuWsdl = WsdlLoader.load("wsdl/fsu-service.wsdl");
        assertTrue(scWsdl.contains("SCService.chinatowercom"));
        assertTrue(fsuWsdl.contains("FSUService.chinatowercom"));
    }

    @Test
    void shouldHaveRpcStyleBinding() {
        String scWsdl = WsdlLoader.load("wsdl/sc-service.wsdl");
        assertTrue(scWsdl.contains("style=\"rpc\""));
        assertTrue(scWsdl.contains("use=\"encoded\""));
    }

    @Test
    void shouldHaveInvokeOperation() {
        String scWsdl = WsdlLoader.load("wsdl/sc-service.wsdl");
        assertTrue(scWsdl.contains("name=\"invoke\""));
        assertTrue(scWsdl.contains("name=\"xmlData\""));
        assertTrue(scWsdl.contains("name=\"invokeReturn\""));
    }

    @Test
    void shouldThrowForMissingWsdl() {
        assertThrows(RuntimeException.class, () ->
                WsdlLoader.load("wsdl/non-existent.wsdl"));
    }
}
