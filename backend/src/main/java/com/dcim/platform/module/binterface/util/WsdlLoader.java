package com.dcim.platform.module.binterface.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * WSDL 文件加载工具。
 *
 * 从 classpath:wsdl/ 目录加载 WSDL XML 文件。
 */
public class WsdlLoader {

    private WsdlLoader() {
    }

    /**
     * 从 classpath 加载 WSDL 文件内容
     *
     * @param path classpath 路径，如 "wsdl/sc-service.wsdl"
     * @return WSDL XML 字符串
     * @throws RuntimeException 如果文件不存在或加载失败
     */
    public static String load(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            if (!resource.exists()) {
                throw new RuntimeException("WSDL 文件不存在: " + path);
            }
            return FileCopyUtils.copyToString(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("WSDL 文件加载失败: " + path, e);
        }
    }
}
