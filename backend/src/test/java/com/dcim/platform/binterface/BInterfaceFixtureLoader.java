package com.dcim.platform.binterface;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * B接口测试夹具加载工具。
 *
 * 从 backend/fixtures/b_interface/ 目录加载 XML/JSON 测试文件。
 * 路径约定：
 *   soap/            — 完整 SOAP 报文
 *   xmldata/         — 纯 xmlData 载荷
 *   invalid/         — 异常/错误报文
 *   expected/        — 解析期望结果 (JSON)
 *
 * 使用方法：
 *   String xml = BInterfaceFixtureLoader.loadSoap("sc_service/login.request.xml");
 */
public class BInterfaceFixtureLoader {

    private static final String FIXTURES_BASE = "b_interface/";

    /**
     * 加载 SOAP 报文 fixture
     * @param path 相对路径，如 "sc_service/login.request.xml"
     */
    public static String loadSoap(String path) {
        return load("soap/" + path);
    }

    /**
     * 加载 xmlData 载荷 fixture
     * @param path 相对路径，如 "login.request.xml"
     */
    public static String loadXmlData(String path) {
        return load("xmldata/" + path);
    }

    /**
     * 加载无效报文 fixture
     * @param path 相对路径，如 "malformed_xml.request.xml"
     */
    public static String loadInvalid(String path) {
        return load("invalid/" + path);
    }

    /**
     * 加载期望解析结果 fixture
     * @param path 相对路径，如 "parsed_login.request.json"
     */
    public static String loadExpected(String path) {
        return load("expected/" + path);
    }

    /**
     * 从 classpath fixtures 目录加载文件内容
     */
    public static String load(String relativePath) {
        try {
            ClassPathResource resource = new ClassPathResource(FIXTURES_BASE + relativePath);
            return FileCopyUtils.copyToString(new InputStreamReader(
                    resource.getInputStream(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("无法加载 fixture: " + FIXTURES_BASE + relativePath, e);
        }
    }

    /**
     * 检查 fixture 文件是否存在
     */
    public static boolean exists(String relativePath) {
        try {
            ClassPathResource resource = new ClassPathResource(FIXTURES_BASE + relativePath);
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }
}
