package com.dcim.platform.module.binterface.soap;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.model.PkTypeDescriptor;
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
 * SOAP 报文处理器。
 *
 * <p>负责 B接口协议 payload 的构造和解析。
 *
 * <h3>职责边界（BIF-P4-005-B 明确）</h3>
 * <ul>
 *   <li>{@link #buildRequest} — 仅构造内层 {@code <Request>} payload（不含 SOAP Envelope）</li>
 *   <li>{@link #buildResponse} — 构造完整 document-style SOAP 响应（SCService 入站用）</li>
 *   <li>{@link #parse} — 双向兼容解析（RPC 和 document-style）</li>
 *   <li>RPC 封装/解包由 {@code FsuServiceRpcAdapter} 负责，不在本类</li>
 * </ul>
 */
@Component
public class SoapMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(SoapMessageHandler.class);

    static final String SOAP_ENVELOPE_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    static final String SOAP_ENC_NS = "http://schemas.xmlsoap.org/soap/encoding/";
    static final String XSI_NS = "http://www.w3.org/2001/XMLSchema-instance";
    static final String FSU_SERVICE_NS = "http://FSUService.chinatowercom.com";
    static final String SC_SERVICE_NS = "http://SCService.chinatowercom.com";

    static final String SOAP_PREFIX = "soap";
    static final String SOAP_ENC_PREFIX = "SOAP-ENC";
    static final String XSI_PREFIX = "xsi";

    // RPC element local names (for parse)
    static final String EL_INVOKE = "invoke";
    static final String EL_INVOKE_RESPONSE = "invokeResponse";
    static final String EL_XML_DATA_PARAM = "xmlData";
    static final String EL_INVOKE_RETURN = "invokeReturn";
    static final String EL_REQUEST = "Request";
    static final String EL_RESPONSE = "Response";

    private final DocumentBuilderFactory docFactory;
    private final TransformerFactory transformerFactory;

    public SoapMessageHandler() {
        this.docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        this.transformerFactory = TransformerFactory.newInstance();
    }

    // ==================== Payload 构造（无 SOAP Envelope） ====================

    /**
     * 构造内层 {@code <Request>} payload（不含 SOAP Envelope）。
     *
     * <p>产出格式：</p>
     * <pre>{@code
     * <Request>
     *   <PK_Type>GET_LOGININFO</PK_Type>
     *   <Info><FSUCode>...</FSUCode></Info>
     *   <xmlData>...</xmlData>
     * </Request>
     * }</pre>
     *
     * <p>用于：FsuServiceRpcAdapter 包装为 WSDL RPC 格式后发送给真实 FSU。</p>
     */
    public String buildRequest(String pkType, String info, String xmlData) {
        return buildPayload(EL_REQUEST, pkType, null, info, xmlData);
    }

    /**
     * 构造内层 {@code <Request>} payload（含 2024 Name+Code 格式 PK_Type）。
     *
     * <p>当 {@code commandCode != null} 时，PK_Type 输出 Name+Code 双标识：</p>
     * <pre>{@code
     * <PK_Type>
     *   <Name>GET_ACTIVEALARM</Name>
     *   <Code>603</Code>
     * </PK_Type>
     * }</pre>
     *
     * <p>当 {@code commandCode == null} 时，退化为旧格式纯文本。</p>
     */
    public String buildRequest(String pkType, Integer commandCode, String info, String xmlData) {
        return buildPayload(EL_REQUEST, pkType, commandCode, info, xmlData);
    }

    /**
     * 构造内层 {@code <Response>} payload（不含 SOAP Envelope）。
     *
     * <p>格式与 buildRequest 对称，根元素为 {@code <Response>}。</p>
     */
    public String buildPayloadResponse(String pkType, String info, String xmlData) {
        return buildPayload(EL_RESPONSE, pkType, null, info, xmlData);
    }

    private String buildPayload(String rootTag, String pkType, Integer commandCode, String info, String xmlData) {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement(rootTag);
            doc.appendChild(root);

            appendPkTypeElement(doc, root, pkType, commandCode);

            if (info != null && !info.isEmpty()) {
                Element infoEl = doc.createElement("Info");
                appendXmlFragment(doc, infoEl, info);
                root.appendChild(infoEl);
            }

            if (xmlData != null && !xmlData.isEmpty()) {
                Element xmlDataEl = doc.createElement("xmlData");
                appendXmlFragment(doc, xmlDataEl, xmlData);
                root.appendChild(xmlDataEl);
            }

            return serialize(doc);
        } catch (Exception e) {
            throw new RuntimeException("Payload 构造失败: rootTag=" + rootTag, e);
        }
    }

    private void appendPkTypeElement(Document doc, Element root, String pkType, Integer commandCode) {
        Element pkTypeEl = doc.createElement("PK_Type");
        if (commandCode != null) {
            Element nameEl = doc.createElement("Name");
            nameEl.setTextContent(pkType);
            pkTypeEl.appendChild(nameEl);
            Element codeEl = doc.createElement("Code");
            codeEl.setTextContent(String.valueOf(commandCode));
            pkTypeEl.appendChild(codeEl);
        } else {
            pkTypeEl.setTextContent(pkType);
        }
        root.appendChild(pkTypeEl);
    }

    // ==================== document-style SOAP Envelope（SCService 入站） ====================

    /**
     * 构造完整 document-style SOAP 响应信封（SCService 入站用），使用 Name+Code 格式 PK_Type。
     *
     * <p>不受 FSUService 出站 RPC adapter 影响。</p>
     */
    public String buildResponseWithCode(String pkType, int commandCode, String info, String xmlData) {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element envelope = doc.createElementNS(SOAP_ENVELOPE_NS, SOAP_PREFIX + ":Envelope");
            envelope.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + SOAP_PREFIX, SOAP_ENVELOPE_NS);
            doc.appendChild(envelope);

            Element body = doc.createElementNS(SOAP_ENVELOPE_NS, SOAP_PREFIX + ":Body");
            envelope.appendChild(body);

            Document innerDoc = buildPayloadDocument(EL_RESPONSE, pkType, info, xmlData);
            // Overwrite PK_Type with Name+Code format
            Element innerRoot = innerDoc.getDocumentElement();
            Element oldPk = (Element) innerRoot.getElementsByTagName("PK_Type").item(0);
            if (oldPk != null) {
                innerRoot.removeChild(oldPk);
                appendPkTypeElement(innerDoc, innerRoot, pkType, commandCode);
            }
            Node imported = doc.importNode(innerRoot, true);
            body.appendChild(imported);

            return serialize(doc);
        } catch (Exception e) {
            throw new RuntimeException("SOAP 响应信封构造失败", e);
        }
    }

    /**
     * 构造完整 document-style SOAP 响应信封（SCService 入站用）。
     *
     * <p>不受 FSUService 出站 RPC adapter 影响。</p>
     */
    public String buildResponse(String pkType, String info, String xmlData) {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element envelope = doc.createElementNS(SOAP_ENVELOPE_NS, SOAP_PREFIX + ":Envelope");
            envelope.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + SOAP_PREFIX, SOAP_ENVELOPE_NS);
            doc.appendChild(envelope);

            Element body = doc.createElementNS(SOAP_ENVELOPE_NS, SOAP_PREFIX + ":Body");
            envelope.appendChild(body);

            // 导入内层 Response payload
            Document innerDoc = buildPayloadDocument(EL_RESPONSE, pkType, info, xmlData);
            Node imported = doc.importNode(innerDoc.getDocumentElement(), true);
            body.appendChild(imported);

            return serialize(doc);
        } catch (Exception e) {
            throw new RuntimeException("SOAP 响应信封构造失败", e);
        }
    }

    private Document buildPayloadDocument(String rootTag, String pkType, String info, String xmlData) {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element root = doc.createElement(rootTag);
            doc.appendChild(root);

            appendPkTypeElement(doc, root, pkType, null);

            if (info != null && !info.isEmpty()) {
                Element infoEl = doc.createElement("Info");
                appendXmlFragment(doc, infoEl, info);
                root.appendChild(infoEl);
            }

            if (xmlData != null && !xmlData.isEmpty()) {
                Element xmlDataEl = doc.createElement("xmlData");
                appendXmlFragment(doc, xmlDataEl, xmlData);
                root.appendChild(xmlDataEl);
            }

            return doc;
        } catch (Exception e) {
            throw new RuntimeException("内层报文构造失败: rootTag=" + rootTag, e);
        }
    }

    /**
     * 构造 SOAP Fault 报文。
     */
    public String buildFault(String faultCode, String faultString, String detailXml) {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.newDocument();

            Element envelope = doc.createElementNS(SOAP_ENVELOPE_NS, SOAP_PREFIX + ":Envelope");
            envelope.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:" + SOAP_PREFIX, SOAP_ENVELOPE_NS);
            doc.appendChild(envelope);

            Element body = doc.createElementNS(SOAP_ENVELOPE_NS, SOAP_PREFIX + ":Body");
            envelope.appendChild(body);

            Element fault = doc.createElementNS(SOAP_ENVELOPE_NS, SOAP_PREFIX + ":Fault");
            body.appendChild(fault);

            Element codeEl = doc.createElement("faultcode");
            codeEl.setTextContent(faultCode);
            fault.appendChild(codeEl);

            Element stringEl = doc.createElement("faultstring");
            stringEl.setTextContent(faultString);
            fault.appendChild(stringEl);

            if (detailXml != null && !detailXml.isEmpty()) {
                Element detail = doc.createElement("detail");
                appendXmlFragment(doc, detail, detailXml);
                fault.appendChild(detail);
            }

            return serialize(doc);
        } catch (Exception e) {
            throw new RuntimeException("SOAP Fault 报文构造失败", e);
        }
    }

    // ==================== 解析（双向兼容） ====================

    /**
     * 解析 SOAP 报文，提取 PK_Type / Info / xmlData。
     *
     * <p>支持 RPC 格式和 document-style 格式。</p>
     *
     * @param soapMessage SOAP Envelope XML 或内层 Request/Response payload XML
     * @return BInterfaceMessage
     */
    public BInterfaceMessage parse(String soapMessage) {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(soapMessage)));

            BInterfaceMessage message = new BInterfaceMessage();

            // 检测是否有 SOAP Envelope
            Element rootEl = doc.getDocumentElement();
            String rootLocal = getLocalNameSafe(rootEl);

            Element bodyEl = null;

            if ("Envelope".equals(rootLocal)) {
                // 完整 SOAP envelope
                bodyEl = findFirstChildByNS(rootEl, SOAP_ENVELOPE_NS, "Body");
                if (bodyEl == null) {
                    throw new RuntimeException("SOAP 报文缺少 Body 元素");
                }

                // SOAP Fault
                Element faultEl = findFirstChildByNS(bodyEl, SOAP_ENVELOPE_NS, "Fault");
                if (faultEl != null) {
                    message.setPkType(BInterfacePkType.UNKNOWN);
                    message.setInfo(parseSoapFault(faultEl));
                    return message;
                }

                // 提取内层根元素
                Element innerRoot = extractInnerRoot(bodyEl);
                if (innerRoot == null) {
                    throw new RuntimeException("SOAP Body 为空或无有效内层报文");
                }
                rootEl = innerRoot;
            } else if (EL_REQUEST.equals(rootLocal) || EL_RESPONSE.equals(rootLocal)) {
                // 直接是内层 payload
                // rootEl already set
            } else {
                // 未知格式，尝试作为内层解析
            }

            String tag = getLocalNameSafe(rootEl);
            message.setRequest(tag);

            // PK_Type — 支持旧格式和 2024 Name+Code 双标识（BIF-P4-011）
            PkTypeDescriptor pkDesc = parsePkType(rootEl);
            message.setPkTypeDescriptor(pkDesc);
            message.setPkType(pkDesc.getLegacyPkType() != null
                    ? pkDesc.getLegacyPkType() : BInterfacePkType.UNKNOWN);

            // Info
            Element infoEl = findFirstChildByNS(rootEl, null, "Info");
            if (infoEl != null) {
                message.setInfo(serializeElementContent(infoEl));
            }

            // xmlData
            Element xmlDataEl = findFirstChildByNS(rootEl, null, "xmlData");
            if (xmlDataEl != null) {
                String content = serializeElementContent(xmlDataEl);
                if (!content.isEmpty()) {
                    message.setXmlData(content);
                }
            }

            return message;
        } catch (Exception e) {
            throw new RuntimeException("SOAP 报文解析失败", e);
        }
    }

    /**
     * 从 SOAP Body 提取内层业务根元素。
     */
    private Element extractInnerRoot(Element bodyEl) {
        Element firstChild = getFirstChildElement(bodyEl);
        if (firstChild == null) return null;

        String localName = getLocalNameSafe(firstChild);

        // RPC invoke → xmlData → Request
        if (EL_INVOKE.equals(localName)) {
            Element paramEl = findFirstChildByNS(firstChild, null, EL_XML_DATA_PARAM);
            if (paramEl != null) {
                Element inner = getFirstChildElement(paramEl);
                if (inner != null) return inner;
                // CDATA fallback
                String text = paramEl.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return parseTextAsElement(text.trim());
                }
            }
        }

        // RPC invokeResponse → invokeReturn → Response
        if (EL_INVOKE_RESPONSE.equals(localName)) {
            Element returnEl = findFirstChildByNS(firstChild, null, EL_INVOKE_RETURN);
            if (returnEl != null) {
                Element inner = getFirstChildElement(returnEl);
                if (inner != null) return inner;
                String text = returnEl.getTextContent();
                if (text != null && !text.trim().isEmpty()) {
                    return parseTextAsElement(text.trim());
                }
            }
        }

        // Document-style: Body 直接包含 Request/Response
        String tag = getLocalNameSafe(firstChild);
        if (EL_REQUEST.equals(tag) || EL_RESPONSE.equals(tag)) {
            return firstChild;
        }

        return firstChild;
    }

    private Element parseTextAsElement(String xmlText) {
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            Document d = builder.parse(new InputSource(new StringReader(xmlText)));
            return d.getDocumentElement();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== PK_Type 双格式解析 (BIF-P4-011) ====================

    /**
     * 解析 PK_Type 元素，支持旧格式和 2024 Name+Code 双标识。
     */
    private PkTypeDescriptor parsePkType(Element rootEl) {
        Element pkTypeEl = findFirstChildByNS(rootEl, null, "PK_Type");
        if (pkTypeEl == null) {
            return PkTypeDescriptor.fromLegacyText(null);
        }

        // 检查是否有子元素（2024 Name+Code 格式）还是纯文本（旧格式）
        Element nameEl = findFirstChildByNS(pkTypeEl, null, "Name");
        Element codeEl = findFirstChildByNS(pkTypeEl, null, "Code");

        if (nameEl != null || codeEl != null) {
            // 2024 格式
            String name = nameEl != null ? nameEl.getTextContent().trim() : null;
            String codeStr = codeEl != null ? codeEl.getTextContent().trim() : null;
            Integer code = null;
            if (codeStr != null && !codeStr.isEmpty()) {
                try {
                    code = Integer.parseInt(codeStr);
                } catch (NumberFormatException e) {
                    log.debug("PK_Type Code 非数字: {}", codeStr);
                }
            }

            if (name != null && !name.isEmpty() && code != null) {
                return PkTypeDescriptor.fromNameCode(name, code);
            } else if (name != null && !name.isEmpty()) {
                return PkTypeDescriptor.fromNameOnly(name);
            } else if (code != null) {
                return PkTypeDescriptor.fromCodeOnly(code);
            }
            return PkTypeDescriptor.fromLegacyText(null);
        }

        // 旧格式：纯文本
        String text = pkTypeEl.getTextContent();
        return PkTypeDescriptor.fromLegacyText(text != null ? text.trim() : null);
    }

    // ==================== 内部工具方法 ====================

    private String parseSoapFault(Element faultEl) {
        StringBuilder sb = new StringBuilder();
        String code = getChildText(faultEl, "faultcode");
        String string = getChildText(faultEl, "faultstring");
        sb.append("<Fault>");
        if (code != null) sb.append("<faultcode>").append(escapeXml(code)).append("</faultcode>");
        if (string != null) sb.append("<faultstring>").append(escapeXml(string)).append("</faultstring>");
        sb.append("</Fault>");
        return sb.toString();
    }

    private void appendXmlFragment(Document doc, Element parent, String xmlFragment) {
        if (xmlFragment == null || xmlFragment.isEmpty()) return;
        try {
            DocumentBuilder builder = docFactory.newDocumentBuilder();
            String wrapped = "<root>" + xmlFragment + "</root>";
            Document fragmentDoc = builder.parse(new InputSource(new StringReader(wrapped)));
            NodeList children = fragmentDoc.getDocumentElement().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node imported = doc.importNode(children.item(i), true);
                parent.appendChild(imported);
            }
        } catch (Exception e) {
            parent.setTextContent(xmlFragment);
        }
    }

    private String serialize(Document doc) {
        try {
            Transformer transformer = transformerFactory.newTransformer();
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

    private String serializeElementContent(Element element) {
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE || child.getNodeType() == Node.TEXT_NODE) {
                    transformer.transform(new DOMSource(child), new StreamResult(writer));
                }
            }
            return writer.toString().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private Element findFirstChildByNS(Element parent, String ns, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element el = (Element) children.item(i);
                if (matchesElement(el, ns, localName)) return el;
            }
        }
        return null;
    }

    private boolean matchesElement(Element el, String ns, String localName) {
        if (localName == null) return false;
        String elLocal = el.getLocalName();
        if (elLocal != null && elLocal.equals(localName)) {
            return ns == null || ns.equals(el.getNamespaceURI());
        }
        String tag = el.getTagName();
        return tag.equals(localName) || tag.endsWith(":" + localName);
    }

    private Element getFirstChildElement(Element parent) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) return (Element) children.item(i);
        }
        return null;
    }

    private String getLocalNameSafe(Element el) {
        String ln = el.getLocalName();
        if (ln != null) return ln;
        String tag = el.getTagName();
        int colon = tag.indexOf(':');
        return colon >= 0 ? tag.substring(colon + 1) : tag;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element) {
                Element el = (Element) children.item(i);
                if (el.getTagName().equals(tagName) || el.getTagName().endsWith(":" + tagName)) {
                    String text = el.getTextContent();
                    return (text != null) ? text.trim() : null;
                }
            }
        }
        return null;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
