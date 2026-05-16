package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.service.fsu.FsuServiceRpcAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FsuServiceRpcAdapter 测试。
 *
 * <p>BIF-P4-005-B: 验证 WSDL RPC 封装/解包正确性。</p>
 */
class FsuServiceRpcAdapterTest {

    private FsuServiceRpcAdapter adapter;

    private static final String REQUEST_PAYLOAD =
            "<Request>\n"
            + "  <PK_Type>GET_LOGININFO</PK_Type>\n"
            + "  <Info>\n"
            + "    <FSUCode>51051243812345</FSUCode>\n"
            + "  </Info>\n"
            + "  <xmlData/>\n"
            + "</Request>";

    private static final String RESPONSE_PAYLOAD =
            "<Response>\n"
            + "  <PK_Type>GET_LOGININFO</PK_Type>\n"
            + "  <Info>\n"
            + "    <ResultCode>0</ResultCode>\n"
            + "  </Info>\n"
            + "  <xmlData>\n"
            + "    <LoginInfo>\n"
            + "      <FSUCode>51051243812345</FSUCode>\n"
            + "      <LoginStatus>LOGIN</LoginStatus>\n"
            + "    </LoginInfo>\n"
            + "  </xmlData>\n"
            + "</Response>";

    @BeforeEach
    void setUp() {
        adapter = new FsuServiceRpcAdapter();
    }

    // ==================== wrapRequestPayload 测试 ====================

    @Test
    void shouldWrapRequestPayloadAsValidXml() {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        assertNotNull(result);
        // 验证可被 DOM 解析
        assertDoesNotThrow(() -> parseXml(result));
    }

    @Test
    void shouldWrapWithSoapEnvelope() {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        assertTrue(result.contains("Envelope"), "应包含 Envelope");
        assertTrue(result.contains("http://schemas.xmlsoap.org/soap/envelope/"),
                "应包含 SOAP 命名空间");
    }

    @Test
    void shouldWrapWithSoapBody() {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        assertTrue(result.contains("Body"), "应包含 Body");
    }

    @Test
    void shouldWrapWithNs1InvokeAsBodyFirstChild() throws Exception {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        Document doc = parseXml(result);

        Element bodyEl = findBody(doc);
        assertNotNull(bodyEl, "应有 Body 元素");

        Element firstChild = getFirstChildElement(bodyEl);
        assertNotNull(firstChild, "Body 应有子元素");

        String localName = firstChild.getLocalName() != null
                ? firstChild.getLocalName()
                : firstChild.getTagName();
        assertEquals("invoke", localName, "Body 第一子元素应为 invoke");
    }

    @Test
    void shouldWrapWithFsuServiceNamespace() throws Exception {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        Document doc = parseXml(result);

        Element bodyEl = findBody(doc);
        Element invoke = getFirstChildElement(bodyEl);

        assertEquals("http://FSUService.chinatowercom.com", invoke.getNamespaceURI(),
                "invoke 应有 FSUService namespace");
    }

    @Test
    void shouldWrapWithXmlDataParameter() throws Exception {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        Document doc = parseXml(result);

        Element bodyEl = findBody(doc);
        Element invoke = getFirstChildElement(bodyEl);
        Element xmlDataEl = findFirstChildByLocalName(invoke, "xmlData");

        assertNotNull(xmlDataEl, "invoke 下应有 xmlData 元素");
    }

    @Test
    void shouldWrapWithXsiTypeOnXmlData() throws Exception {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        Document doc = parseXml(result);

        Element bodyEl = findBody(doc);
        Element invoke = getFirstChildElement(bodyEl);
        Element xmlDataEl = findFirstChildByLocalName(invoke, "xmlData");

        String typeAttr = xmlDataEl.getAttributeNS(
                "http://www.w3.org/2001/XMLSchema-instance", "type");
        if (typeAttr == null || typeAttr.isEmpty()) {
            typeAttr = xmlDataEl.getAttribute("xsi:type");
        }
        assertNotNull(typeAttr, "xmlData 应有 xsi:type");
        assertTrue(typeAttr.contains("string"), "xsi:type 应为 string 类型");
    }

    @Test
    void shouldPreserveInnerRequestPayload() {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        assertTrue(result.contains("Request"), "应保留 Request 元素");
        assertTrue(result.contains("GET_LOGININFO"), "应保留 PK_Type");
        assertTrue(result.contains("51051243812345"), "应保留 FSUCode");
    }

    @Test
    void shouldPreservePkTypeInPayload() {
        String payloadWithGetData = REQUEST_PAYLOAD.replace("GET_LOGININFO", "GET_DATA");
        String result = adapter.wrapRequestPayload(payloadWithGetData);
        assertTrue(result.contains("GET_DATA"), "应保留 GET_DATA");
    }

    @Test
    void shouldPreserveInfoFields() {
        String payload = REQUEST_PAYLOAD.replace("51051243812345", "TEST-FSU")
                .replace("<xmlData/>", "<xmlData><SignalID>TEMP-001</SignalID></xmlData>");
        String result = adapter.wrapRequestPayload(payload);
        assertTrue(result.contains("TEST-FSU"), "应保留 FSUCode");
        assertTrue(result.contains("TEMP-001"), "应保留 SignalID");
    }

    @Test
    void shouldRejectNullPayload() {
        assertThrows(IllegalArgumentException.class, () -> adapter.wrapRequestPayload(null));
    }

    @Test
    void shouldRejectEmptyPayload() {
        assertThrows(IllegalArgumentException.class, () -> adapter.wrapRequestPayload(""));
    }

    @Test
    void shouldContainSoapEncodingNamespace() {
        String result = adapter.wrapRequestPayload(REQUEST_PAYLOAD);
        assertTrue(result.contains("http://schemas.xmlsoap.org/soap/encoding/"),
                "应包含 SOAP ENC 命名空间");
    }

    // ==================== unwrapResponsePayload 测试 ====================

    @Test
    void shouldUnwrapInvokeReturnResponse() {
        String rpcResponse = buildRpcResponse(RESPONSE_PAYLOAD);
        String result = adapter.unwrapResponsePayload(rpcResponse);

        assertNotNull(result);
        assertTrue(result.contains("Response"), "解包后应包含 Response");
        assertTrue(result.contains("Envelope"), "解包后应为有效 SOAP Envelope");
    }

    @Test
    void shouldUnwrapAndPreserveResultCode() {
        String rpcResponse = buildRpcResponse(RESPONSE_PAYLOAD);
        String result = adapter.unwrapResponsePayload(rpcResponse);
        assertTrue(result.contains("ResultCode"), "应保留 ResultCode");
        assertTrue(result.contains("<ResultCode>0</ResultCode>"), "ResultCode 应为 0");
    }

    @Test
    void shouldUnwrapAndPreserveLoginInfo() {
        String rpcResponse = buildRpcResponse(RESPONSE_PAYLOAD);
        String result = adapter.unwrapResponsePayload(rpcResponse);
        assertTrue(result.contains("LoginInfo"), "应保留 LoginInfo");
        assertTrue(result.contains("LOGIN"), "应保留 LOGIN 状态");
    }

    @Test
    void shouldHandleSoapFaultInResponse() {
        String faultXml = BInterfaceFixtureLoader.load(
                "real_fsu/readonly/fault_method_request_not_implemented.xml");
        String result = adapter.unwrapResponsePayload(faultXml);

        assertNotNull(result);
        assertTrue(result.contains("Envelope"), "Fault 应被包装为 Envelope");
    }

    @Test
    void shouldDetectRpcFault() {
        String faultXml = BInterfaceFixtureLoader.load(
                "real_fsu/readonly/fault_method_request_not_implemented.xml");
        assertTrue(adapter.isRpcFault(faultXml), "应检测到 RPC Fault");
    }

    @Test
    void shouldNotDetectFaultInNormalResponse() {
        String rpcResponse = buildRpcResponse(RESPONSE_PAYLOAD);
        assertFalse(adapter.isRpcFault(rpcResponse), "正常响应不应检测为 Fault");
    }

    @Test
    void shouldExtractFaultCode() {
        String faultXml = BInterfaceFixtureLoader.load(
                "real_fsu/readonly/fault_method_request_not_implemented.xml");
        String code = adapter.extractFaultCode(faultXml);
        assertEquals("SOAP-ENV:Client", code, "应提取正确的 faultcode");
    }

    @Test
    void shouldExtractFaultString() {
        String faultXml = BInterfaceFixtureLoader.load(
                "real_fsu/readonly/fault_method_request_not_implemented.xml");
        String msg = adapter.extractFaultString(faultXml);
        assertNotNull(msg);
        assertTrue(msg.contains("Request"), "Fault 消息应提到 Request");
        assertTrue(msg.contains("not implemented"), "应包含 not implemented");
    }

    @Test
    void shouldHandleEmptyResponse() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.unwrapResponsePayload(null));
    }

    @Test
    void shouldHandleDocumentStyleResponse() {
        // 向后兼容：直接传 document-style SOAP
        String docResponse = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>"
                + "<Response><PK_Type>HEARTBEAT</PK_Type><Info><ResultCode>0</ResultCode></Info></Response>"
                + "</soap:Body></soap:Envelope>";
        // 应该能处理（无 invokeResponse 时传递原响应）
        String result = adapter.unwrapResponsePayload(docResponse);
        assertNotNull(result);
    }

    @Test
    void shouldRejectNullUnwrap() {
        assertThrows(IllegalArgumentException.class,
                () -> adapter.unwrapResponsePayload(null));
    }

    // ==================== 真实 Fault fixture 测试 ====================

    @Test
    void shouldLoadRealFsuFaultFixture() {
        String xml = BInterfaceFixtureLoader.load(
                "real_fsu/readonly/fault_method_request_not_implemented.xml");
        assertNotNull(xml);
        assertTrue(xml.contains("SOAP-ENV:Client"));
        assertTrue(xml.contains("Method 'Request' not implemented"));
    }

    @Test
    void shouldParseRealFsuFaultCode() {
        String xml = BInterfaceFixtureLoader.load(
                "real_fsu/readonly/fault_method_request_not_implemented.xml");
        assertEquals("SOAP-ENV:Client", adapter.extractFaultCode(xml));
    }

    @Test
    void realFsuFaultExplainsDocumentStyleMismatch() {
        String xml = BInterfaceFixtureLoader.load(
                "real_fsu/readonly/fault_method_request_not_implemented.xml");
        String msg = adapter.extractFaultString(xml);
        assertTrue(msg.contains("Request"), "Fault 应提及被拒绝的方法名 Request");
        assertTrue(msg.contains("not implemented") || msg.contains("not recognized"),
                "Fault 应说明方法未实现/未识别");
    }

    // ==================== 辅助方法 ====================

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private Element findBody(Document doc) throws Exception {
        return findFirstChildByLocalName(doc.getDocumentElement(), "Body");
    }

    private Element findFirstChildByLocalName(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element el = (Element) children.item(i);
                String ln = el.getLocalName();
                if (ln != null && ln.equals(localName)) return el;
                if (el.getTagName().equals(localName) || el.getTagName().endsWith(":" + localName)) {
                    return el;
                }
            }
        }
        return null;
    }

    private Element getFirstChildElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) return (Element) children.item(i);
        }
        return null;
    }

    /**
     * 构造模拟 RPC 响应。
     */
    private String buildRpcResponse(String innerResponseXml) {
        return "<SOAP-ENV:Envelope"
                + " xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\""
                + " xmlns:SOAP-ENC=\"http://schemas.xmlsoap.org/soap/encoding/\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns:ns1=\"http://FSUService.chinatowercom.com\">"
                + "<SOAP-ENV:Body>"
                + "<ns1:invokeResponse>"
                + "<invokeReturn xsi:type=\"SOAP-ENC:string\">"
                + innerResponseXml
                + "</invokeReturn>"
                + "</ns1:invokeResponse>"
                + "</SOAP-ENV:Body>"
                + "</SOAP-ENV:Envelope>";
    }
}
