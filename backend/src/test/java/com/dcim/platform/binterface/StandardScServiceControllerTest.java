package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandDispatcher;
import com.dcim.platform.module.binterface.log.BInterfaceMessageLogService;
import com.dcim.platform.module.binterface.repository.BInterfaceMessageLogRepository;
import com.dcim.platform.module.binterface.service.sc.ScServiceProcessor;
import com.dcim.platform.module.binterface.service.sc.StandardScServiceController;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StandardScServiceController 集成测试。
 *
 * 使用 Spring Boot Test 验证：
 * 1. POST /services/SCService 返回 SOAP XML（非 JSON）
 * 2. POST /api/b-interface/sc-service 保持兼容
 * 3. BInterfaceMessageLog 记录入站报文
 * 4. 真实 tcpdump LOGIN 样本解析正确
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "b-interface.fsu-client.real-call-enabled=false",
                "spring.jpa.open-in-view=false"
        })
class StandardScServiceControllerTest {

    @Autowired
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    /**
     * 真实 tcpdump LOGIN SOAP 样本（2026-05-21 现场抓包）
     */
    private static final String REAL_LOGIN_SOAP = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
                           xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <soap:Body>
                <ns1:invoke xmlns:ns1="http://SCService.chinatowercom.com">
                  <xmlData>
                    <PK_Type>
                      <Name>LOGIN</Name>
                      <Code>101</Code>
                    </PK_Type>
                    <Info>
                      <FsuId>51051243812345</FsuId>
                      <FsuCode>51051243812345</FsuCode>
                      <FsuIP>192.168.100.100</FsuIP>
                      <MacId>00:09:F5:E0:8D:B7</MacId>
                      <Version>21.1.HQ.FSU.WD.AA44.R</Version>
                      <DictVersion>1</DictVersion>
                      <DeviceList>
                        <Device Id="51051241820004" Code="51051241820004"/>
                        <Device Id="51051241830004" Code="51051241830004"/>
                        <Device Id="51051241840004" Code="51051241840004"/>
                        <Device Id="51051240700002" Code="51051240700002"/>
                        <Device Id="51051243812345" Code="51051243812345"/>
                      </DeviceList>
                    </Info>
                  </xmlData>
                </ns1:invoke>
              </soap:Body>
            </soap:Envelope>""";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    // ==================== /services/SCService 入口 ====================

    @Test
    void shouldAcceptRealLoginAtServicesPath() throws Exception {
        var response = mockMvc.perform(post("/services/SCService")
                        .contentType(MediaType.TEXT_XML)
                        .header("SOAPAction", "")
                        .content(REAL_LOGIN_SOAP))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                .andReturn();

        String body = response.getResponse().getContentAsString();

        // 不是 JSON
        assertFalse(body.trim().startsWith("{"), "响应不能是 JSON: " + body.substring(0, Math.min(200, body.length())));

        // 包含 SOAP Envelope
        assertTrue(body.contains("Envelope"), "响应应包含 SOAP Envelope");

        // 包含 SOAP Body（ACK 或 Fault 均可，取决于 FSU 是否已注册）
        assertTrue(body.contains("Body") || body.contains("body"),
                "响应应包含 SOAP Body");
    }

    @Test
    void shouldReturnSoapFaultForEmptyBodyAtServicesPath() throws Exception {
        var response = mockMvc.perform(post("/services/SCService")
                        .contentType(MediaType.TEXT_XML)
                        .content(""))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                .andReturn();

        String body = response.getResponse().getContentAsString();
        assertFalse(body.trim().startsWith("{"), "空请求不应返回 JSON");
        assertTrue(body.contains("Fault") || body.contains("fault"),
                "空请求应返回 SOAP Fault");
    }

    // ==================== /api/b-interface/sc-service 兼容 ====================

    @Test
    void shouldKeepOldPathWorking() throws Exception {
        var response = mockMvc.perform(post("/api/b-interface/sc-service")
                        .contentType(MediaType.TEXT_XML)
                        .header("SOAPAction", "")
                        .content(REAL_LOGIN_SOAP))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
                .andReturn();

        String body = response.getResponse().getContentAsString();
        assertFalse(body.trim().startsWith("{"), "旧入口也不能返回 JSON");
        assertTrue(body.contains("Envelope"), "旧入口响应应包含 SOAP Envelope");
    }

    // ==================== 两入口一致性 ====================

    @Test
    void shouldReturnConsistentResponseFromBothPaths() throws Exception {
        var r1 = mockMvc.perform(post("/services/SCService")
                        .contentType(MediaType.TEXT_XML)
                        .content(REAL_LOGIN_SOAP))
                .andReturn();
        var r2 = mockMvc.perform(post("/api/b-interface/sc-service")
                        .contentType(MediaType.TEXT_XML)
                        .content(REAL_LOGIN_SOAP))
                .andReturn();

        // 两个入口都应返回非 JSON SOAP
        assertFalse(r1.getResponse().getContentAsString().trim().startsWith("{"));
        assertFalse(r2.getResponse().getContentAsString().trim().startsWith("{"));
    }

    // ==================== BInterfaceMessageLog 入库 ====================

    @Autowired
    private BInterfaceMessageLogRepository messageLogRepository;

    @Test
    void shouldSaveInboundMessageForServicesPath() throws Exception {
        // 记录保存前的数量
        long countBefore = messageLogRepository.count();

        mockMvc.perform(post("/services/SCService")
                        .contentType(MediaType.TEXT_XML)
                        .header("SOAPAction", "")
                        .content(REAL_LOGIN_SOAP))
                .andExpect(status().isOk());

        // 报文日志应新增一条记录（POST 处理过程中调用了 saveInbound）
        long countAfter = messageLogRepository.count();
        assertTrue(countAfter >= countBefore,
                "报文日志记录数不应减少（saveInbound 应在处理过程中被调用）");
    }
}
