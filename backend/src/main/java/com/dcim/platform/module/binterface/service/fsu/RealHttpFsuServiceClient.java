package com.dcim.platform.module.binterface.service.fsu;

import com.dcim.platform.module.binterface.compat.BInterfaceCommand2016;
import com.dcim.platform.module.binterface.compat.BInterfaceCommandAliasMapper;
import com.dcim.platform.module.binterface.model.BInterfaceCommand2024;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * 真实 HTTP FSU 服务客户端（WSDL RPC 适配版）。
 *
 * <p>BIF-P4-005-B: 通过 {@link FsuServiceRpcAdapter} 将 B接口 payload 包装为
 * WSDL RPC/encoded 格式后发送给真实 FSU。</p>
 *
 * <h3>调用链</h3>
 * <pre>
 * SoapMessageHandler.buildRequest() → 内层 Request payload
 *   → FsuServiceRpcAdapter.wrapRequestPayload() → RPC SOAP Envelope
 *     → HTTP POST → FSU
 *       → FsuServiceRpcAdapter.unwrapResponsePayload() → 内层 Response payload
 *         → SoapMessageHandler.parse() → XmlDataParser
 * </pre>
 *
 * <h3>安全约束</h3>
 * <ul>
 *   <li>默认 disabled ({@code real-call-enabled=false})，需显式配置启用</li>
 *   <li>不支持控制命令（SET_POINT, SET_FSUREBOOT 等）</li>
 *   <li>所有异常转结构化响应，不抛出网络异常</li>
 *   <li>SOAPAction 按 WSDL 设为空字符串</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "b-interface.fsu-client.real-call-enabled", havingValue = "true")
public class RealHttpFsuServiceClient implements FsuServiceClient {

    private static final Logger log = LoggerFactory.getLogger(RealHttpFsuServiceClient.class);

    private static final String SOAP_CONTENT_TYPE = "text/xml; charset=utf-8";
    private static final String SOAP_ACTION_HEADER = "";

    private final SoapMessageHandler soapMessageHandler;
    private final XmlDataParser xmlDataParser;
    private final FsuServiceRpcAdapter rpcAdapter;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public RealHttpFsuServiceClient(SoapMessageHandler soapMessageHandler,
                                     XmlDataParser xmlDataParser,
                                     FsuServiceRpcAdapter rpcAdapter) {
        this.soapMessageHandler = soapMessageHandler;
        this.xmlDataParser = xmlDataParser;
        this.rpcAdapter = rpcAdapter;
        this.connectTimeoutMs = 3000;
        this.readTimeoutMs = 5000;
        log.warn("RealHttpFsuServiceClient 已启用（真实 FSU 调用已开启，RPC 适配版）");
    }

    public RealHttpFsuServiceClient(SoapMessageHandler soapMessageHandler,
                                     XmlDataParser xmlDataParser,
                                     FsuServiceRpcAdapter rpcAdapter,
                                     int connectTimeoutMs, int readTimeoutMs) {
        this.soapMessageHandler = soapMessageHandler;
        this.xmlDataParser = xmlDataParser;
        this.rpcAdapter = rpcAdapter;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        log.warn("RealHttpFsuServiceClient 已启用（真实 FSU 调用已开启，RPC 适配版）");
    }

    @Override
    public FsuServiceResponse call(FsuServiceRequest request) {
        if (request == null) {
            return FsuServiceResponse.error("请求参数为空");
        }

        if (request.getServiceUrl() == null || request.getServiceUrl().trim().isEmpty()) {
            return FsuServiceResponse.fail("2001", "FSU 服务地址为空: " + request.getFsuCode());
        }

        log.info("FSU HTTP 调用: pkType={}, fsuCode={}, url={}",
                request.getPkType(), request.getFsuCode(), request.getServiceUrl());

        try {
            // 1. 构造内层 <Request> payload
            //    structured (default): 2024 Name+Code 格式
            //    legacy-text: 旧版纯文本 PK_Type（无 Code）
            //    legacy-2016: Name+Code 格式但使用 B接口2016 码表
            String pkTypeName = request.getPkType().name();
            boolean forceLegacyText = "legacy-text".equals(request.getPkTypeFormat());
            boolean forceLegacy2016 = "legacy-2016".equals(request.getPkTypeFormat());
            String requestPayload;
            if (forceLegacyText) {
                requestPayload = soapMessageHandler.buildRequest(pkTypeName,
                        request.getInfoXml(), request.getXmlDataXml());
            } else if (forceLegacy2016) {
                Integer code2016 = BInterfaceCommand2016.codeFor(pkTypeName).orElse(null);
                requestPayload = soapMessageHandler.buildRequest(pkTypeName, code2016,
                        request.getInfoXml(), request.getXmlDataXml());
            } else {
                BInterfaceCommand2024 cmd2024 = BInterfaceCommandAliasMapper.to2024(request.getPkType()).orElse(null);
                if (cmd2024 != null) {
                    requestPayload = soapMessageHandler.buildRequest(cmd2024.getName(), cmd2024.getCode(),
                            request.getInfoXml(), request.getXmlDataXml());
                } else {
                    requestPayload = soapMessageHandler.buildRequest(pkTypeName,
                            request.getInfoXml(), request.getXmlDataXml());
                }
            }

            // 2. RPC 封装
            String rpcSoapRequest = rpcAdapter.wrapRequestPayload(requestPayload);

            // 3. HTTP POST
            String rpcSoapResponse = doHttpPost(request.getServiceUrl(), rpcSoapRequest);

            // 4. RPC 解包 → document-style SOAP
            String docSoapResponse = rpcAdapter.unwrapResponsePayload(rpcSoapResponse);

            // 5. 解析
            BInterfaceMessage message = soapMessageHandler.parse(docSoapResponse);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            // 6. 提取 ResultCode
            String resultCode = "0";
            if (infoXml != null && xmlDataParser.parseFields(infoXml).containsKey("ResultCode")) {
                resultCode = xmlDataParser.parseFields(infoXml).get("ResultCode");
            }

            if ("0".equals(resultCode)) {
                return FsuServiceResponse.successReal(docSoapResponse, infoXml, rawXmlData, xmlData);
            } else {
                return FsuServiceResponse.fail(resultCode, "FSU 返回错误: " + resultCode);
            }

        } catch (java.net.ConnectException e) {
            log.error("FSU 连接失败: url={}", request.getServiceUrl(), e);
            return FsuServiceResponse.fail("5001", "FSU 连接失败: " + e.getMessage());
        } catch (java.net.SocketTimeoutException e) {
            log.error("FSU 请求超时: url={}", request.getServiceUrl(), e);
            return FsuServiceResponse.fail("5001", "FSU 请求超时: " + e.getMessage());
        } catch (Exception e) {
            log.error("FSU HTTP 调用异常: pkType={}, fsuCode={}",
                    request.getPkType(), request.getFsuCode(), e);
            return FsuServiceResponse.error("FSU HTTP 调用失败: " + e.getMessage());
        }
    }

    /**
     * 执行 HTTP POST 请求（RPC SOAP + SOAPAction header）。
     */
    private String doHttpPost(String urlStr, String soapXml) throws Exception {
        URL url = URI.create(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", SOAP_CONTENT_TYPE);
            conn.setRequestProperty("SOAPAction", SOAP_ACTION_HEADER);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = soapXml.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                return new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                // HTTP 500 可能包含 SOAP Fault XML，通过 error stream 读取
                String errorBody = new String(
                        conn.getErrorStream() != null
                                ? conn.getErrorStream().readAllBytes()
                                : new byte[0],
                        StandardCharsets.UTF_8);
                throw new RuntimeException("HTTP " + responseCode + ": " + errorBody);
            }
        } finally {
            conn.disconnect();
        }
    }
}
