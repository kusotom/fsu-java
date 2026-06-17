package com.dcim.platform.module.binterface.service.fsu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * FSUService WSDL RPC 适配器。
 *
 * <p>BIF-P4-005-B: 将 B接口协议内层 {@code <Request>} payload 包装为 WSDL RPC/encoded
 * 线格式，并从 RPC 响应中提取内层 {@code <Response>} payload。</p>
 *
 * <h3>WSDL 约定（FSUService.wsdl）</h3>
 * <ul>
 *   <li>operation: invoke</li>
 *   <li>input: invokeRequest(xmlData: soapenc:string)</li>
 *   <li>output: invokeResponse(invokeReturn: soapenc:string)</li>
 *   <li>style: rpc</li>
 *   <li>use: encoded</li>
 *   <li>SOAPAction: ""</li>
 *   <li>targetNamespace: http://FSUService.chinatowercom.com</li>
 * </ul>
 *
 * <h3>RPC 封装格式 (BIF2016-RPCXML-001)</h3>
 * <pre>{@code
 * SOAP-ENV:Envelope
 *   SOAP-ENV:Body
 *     ns1:invoke (xmlns:ns1="http://FSUService.chinatowercom.com")
 *       xmlData (xsi:type="xsd:string")
 *         &lt;Request&gt;   ← XML-escaped text content (not DOM child)
 *           PK_Type
 *           Info
 *           xmlData
 *         &lt;/Request&gt;
 *       </xmlData>
 *     </ns1:invoke>
 * }</pre>
 *
 * <h3>边界</h3>
 * <ul>
 *   <li>仅处理 FSUService 出站（SC→FSU）RPC 封装</li>
 *   <li>不影响 SCService 入站链路</li>
 *   <li>不影响 StubFsuServiceClient</li>
 *   <li>不修改 payload 内部结构</li>
 * </ul>
 */
@Component
public class FsuServiceRpcAdapter {

    private static final Logger log = LoggerFactory.getLogger(FsuServiceRpcAdapter.class);

    // ==================== 命名空间 ====================
    static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String SOAP_ENC_NS = "http://schemas.xmlsoap.org/soap/encoding/";
    static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    static final String FSU_SERVICE_NS = "http://FSUService.chinatowercom.com";

    static final String SOAP_PREFIX = "SOAP-ENV";
    static final String SOAP_ENC_PREFIX = "SOAP-ENC";
    static final String XSI_PREFIX = "xsi";
    static final String NS1_PREFIX = "ns1";

    static final String SOAP_ACTION_HEADER = "";

    private final DocumentBuilderFactory docFactory;

    public FsuServiceRpcAdapter() {
        this.docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
    }

    // ==================== RPC 包装 ====================

    /**
     * 将内层 {@code <Request>} payload 包装为完整 RPC SOAP Envelope。
     *
     * @param requestPayloadXml 内层 payload 字符串，如 {@code <Request><PK_Type>...</PK_Type>...</Request>}
     * @return 完整 RPC SOAP Envelope XML
     * @throws RuntimeException 如果 payload 为空或 XML 解析失败
     */
    public String wrapRequestPayload(String requestPayloadXml) {
        if (requestPayloadXml == null || requestPayloadXml.trim().isEmpty()) {
            throw new IllegalArgumentException("requestPayloadXml 不能为空");
        }

        // BIF2016-RPCXML-001: Emerson FSU 验证可用格式:
        // <soap:Envelope xmlns:soap="..." xmlns:xsd="..." xmlns:xsi="...">
        //   <soap:Body>
        //     <ns1:invoke xmlns:ns1="http://FSUService.chinatowercom.com">
        //       <xmlData xsi:type="xsd:string">&lt;Request&gt;...&lt;/Request&gt;</xmlData>
        //     </ns1:invoke>
        //   </soap:Body>
        // </soap:Envelope>
        //
        // Use explicit XML string construction to avoid DOM serializer producing
        // incompatible prefixes (SOAP-ENV) not recognized by Emerson FSU.

        // Escape the payload as XML text (the DOM serializer would do this,
        // but explicit construction avoids prefix mismatches).
        String escapedPayload = xmlEscape(requestPayloadXml);

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soap:Envelope"
                + " xmlns:soap=\"" + SOAP_ENVELOPE_NS + "\""
                + " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\""
                + " xmlns:xsi=\"" + XSI_NS + "\""
                + ">"
                + "<soap:Body>"
                + "<ns1:invoke xmlns:ns1=\"" + FSU_SERVICE_NS + "\">"
                + "<xmlData xsi:type=\"xsd:string\">" + escapedPayload + "</xmlData>"
                + "</ns1:invoke>"
                + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /**
     * XML-escape a string for use as xmlData text content.
     * Only escapes the 5 XML special characters. Does NOT double-escape.
     */
    static String xmlEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 64);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;"); break;
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                case '"':  sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    // ==================== RPC 解包 ====================

    /**
     * 从 RPC SOAP 响应中解包，返回 document-style SOAP Envelope 以便
     * {@link com.dcim.platform.module.binterface.soap.SoapMessageHandler#parse(String)} 处理。
     *
     * @param rpcResponseSoap 完整 RPC SOAP 响应报文
     * @return document-style SOAP Envelope，包含内层 {@code <Response>} payload，
     *         或 SOAP Fault 的包装信封
     * @throws RuntimeException 如果响应为空或无法解析
     */
    public String unwrapResponsePayload(String rpcResponseSoap) {
        if (rpcResponseSoap == null || rpcResponseSoap.trim().isEmpty()) {
            throw new IllegalArgumentException("rpcResponseSoap 不能为空");
        }

        try {
            Document doc = parseXml(rpcResponseSoap);
            Element envelope = doc.getDocumentElement();

            // 查找 Body
            Element bodyEl = findFirstChildByNS(envelope, SOAP_ENVELOPE_NS, "Body");
            if (bodyEl == null) {
                throw new RuntimeException("RPC 响应缺少 SOAP Body");
            }

            // 检测 SOAP Fault
            Element faultEl = findFirstChildByNS(bodyEl, SOAP_ENVELOPE_NS, "Fault");
            if (faultEl != null) {
                log.debug("RPC 响应包含 SOAP Fault");
                return wrapFaultInEnvelope(faultEl);
            }

            // 查找 invokeResponse → invokeReturn → Response
            Element invokeResp = findFirstChildByNS(bodyEl, null, "invokeResponse");
            if (invokeResp == null) {
                // 向后兼容：Body 内直接包含 Response（document-style 响应）
                Element directResponse = findFirstChildByNS(bodyEl, null, "Response");
                if (directResponse != null) {
                    log.debug("RPC 解包: document-style 响应，直接传递");
                    return rpcResponseSoap;
                }
                throw new RuntimeException("RPC 响应缺少 invokeResponse 元素");
            }

            Element returnEl = findFirstChildByNS(invokeResp, null, "invokeReturn");
            if (returnEl == null) {
                throw new RuntimeException("RPC 响应缺少 invokeReturn 元素");
            }

            // 提取内层内容
            Element innerRoot = getFirstChildElement(returnEl);
            String innerXml;
            if (innerRoot != null) {
                innerXml = serializeElement(innerRoot);
            } else {
                // 可能是 CDATA 或转义文本
                String text = returnEl.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    innerXml = stripXmlDeclaration(text.trim());
                } else {
                    // 空响应
                    innerXml = "<" + "Response" + "/>";
                }
            }

            log.debug("RPC 解包完成: innerXml 长度={}", innerXml.length());
            return wrapInMinimalEnvelope(innerXml);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("RPC 响应解包失败: " + e.getMessage(), e);
        }
    }

    // ==================== SOAP Fault 检测 ====================

    /**
     * 检测 SOAP 报文是否包含 Fault。
     */
    public boolean isRpcFault(String soap) {
        if (soap == null || soap.trim().isEmpty()) return false;
        try {
            Document doc = parseXml(soap);
            Element bodyEl = findFirstChildByNS(doc.getDocumentElement(), SOAP_ENVELOPE_NS, "Body");
            if (bodyEl == null) return false;
            return findFirstChildByNS(bodyEl, SOAP_ENVELOPE_NS, "Fault") != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 SOAP Fault 中提取 faultcode。
     */
    public String extractFaultCode(String soap) {
        if (soap == null) return null;
        try {
            Document doc = parseXml(soap);
            Element bodyEl = findFirstChildByNS(doc.getDocumentElement(), SOAP_ENVELOPE_NS, "Body");
            if (bodyEl == null) return null;
            Element faultEl = findFirstChildByNS(bodyEl, SOAP_ENVELOPE_NS, "Fault");
            if (faultEl == null) return null;
            Element codeEl = findFirstChildByNS(faultEl, null, "faultcode");
            return codeEl != null ? codeEl.getTextContent().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 SOAP Fault 中提取 faultstring。
     */
    public String extractFaultString(String soap) {
        if (soap == null) return null;
        try {
            Document doc = parseXml(soap);
            Element bodyEl = findFirstChildByNS(doc.getDocumentElement(), SOAP_ENVELOPE_NS, "Body");
            if (bodyEl == null) return null;
            Element faultEl = findFirstChildByNS(bodyEl, SOAP_ENVELOPE_NS, "Fault");
            if (faultEl == null) return null;
            Element stringEl = findFirstChildByNS(faultEl, null, "faultstring");
            return stringEl != null ? stringEl.getTextContent().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 从 XML 文本中去除嵌套的 {@code <?xml ...?>} 声明。
     *
     * <p>真实 FSU 的 RPC 响应中，{@code <invokeReturn>} 文本内容可能包含完整的
     * XML 文档（含声明），需要剥离声明后才能嵌入其他 XML 文档中。</p>
     *
     * <p>保留原始报文不变，仅在解析副本上操作。</p>
     */
    public static String stripXmlDeclaration(String xmlText) {
        if (xmlText == null || xmlText.isEmpty()) return xmlText;
        String trimmed = xmlText.trim();
        if (trimmed.startsWith("<?xml ")) {
            int end = trimmed.indexOf("?>");
            if (end >= 0) {
                return trimmed.substring(end + 2).trim();
            }
        }
        return xmlText;
    }

    /**
     * 将内层 XML 包装在最小 SOAP Envelope 中，供 SoapMessageHandler.parse() 处理。
     */
    private String wrapInMinimalEnvelope(String innerXml) {
        return "<soap:Envelope xmlns:soap=\"" + SOAP_ENVELOPE_NS + "\">"
                + "<soap:Body>" + innerXml + "</soap:Body>"
                + "</soap:Envelope>";
    }

    /**
     * 将 SOAP Fault 元素包装在最小 SOAP Envelope 中。
     */
    private String wrapFaultInEnvelope(Element faultEl) {
        String faultXml = serializeElement(faultEl);
        return "<soap:Envelope xmlns:soap=\"" + SOAP_ENVELOPE_NS + "\">"
                + "<soap:Body>" + faultXml + "</soap:Body>"
                + "</soap:Envelope>";
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilder builder = docFactory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private Element findFirstChildByNS(Element parent, String ns, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element el = (Element) children.item(i);
                String elLocal = el.getLocalName();
                if (elLocal != null && elLocal.equals(localName)) {
                    if (ns == null || ns.equals(el.getNamespaceURI())) return el;
                }
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

    private String serialize(Document doc) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("XML 序列化失败", e);
        }
    }

    private String serializeElement(Element el) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(el), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
