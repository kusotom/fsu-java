package com.dcim.platform.binterface;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

final class BInterface2016StandardTestSupport {

    static final String FSU_CODE = "51051243812345";

    private BInterface2016StandardTestSupport() {
    }

    enum Direction {
        FSU_TO_SC,
        SC_TO_FSU
    }

    record CommandSpec(String name, int code, String ackName, int ackCode,
                       Direction direction, boolean setCommand) {
    }

    static final List<CommandSpec> PRIMARY_COMMANDS = List.of(
            new CommandSpec("LOGIN", 101, "LOGIN_ACK", 102, Direction.FSU_TO_SC, false),
            new CommandSpec("LOGOUT", 103, "LOGOUT_ACK", 104, Direction.FSU_TO_SC, false),
            new CommandSpec("GET_DATA", 401, "GET_DATA_ACK", 402, Direction.SC_TO_FSU, false),
            new CommandSpec("SEND_ALARM", 501, "SEND_ALARM_ACK", 502, Direction.FSU_TO_SC, false),
            new CommandSpec("GET_LOGININFO", 1501, "GET_LOGININFO_ACK", 1502, Direction.SC_TO_FSU, false),
            new CommandSpec("GET_FTP", 1601, "GET_FTP_ACK", 1602, Direction.SC_TO_FSU, false),
            new CommandSpec("GET_FSUINFO", 1701, "GET_FSUINFO_ACK", 1702, Direction.SC_TO_FSU, false),
            new CommandSpec("GET_THRESHOLD", 1901, "GET_THRESHOLD_ACK", 1902, Direction.SC_TO_FSU, false),
            new CommandSpec("TIME_CHECK", 1301, "TIME_CHECK_ACK", 1302, Direction.SC_TO_FSU, false)
    );

    static final List<CommandSpec> SET_COMMANDS = List.of(
            new CommandSpec("SET_POINT", 1001, "SET_POINT_ACK", 1002, Direction.SC_TO_FSU, true),
            new CommandSpec("SET_THRESHOLD", 2001, "SET_THRESHOLD_ACK", 2002, Direction.SC_TO_FSU, true),
            new CommandSpec("SET_LOGININFO", 1503, "SET_LOGININFO_ACK", 1504, Direction.SC_TO_FSU, true),
            new CommandSpec("SET_FTP", 1603, "SET_FTP_ACK", 1604, Direction.SC_TO_FSU, true),
            new CommandSpec("SET_FSUREBOOT", 1801, "SET_FSUREBOOT_ACK", 1802, Direction.SC_TO_FSU, true)
    );

    static CommandSpec spec(String name) {
        return allCommands().stream()
                .filter(s -> s.name().equals(name) || s.ackName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown BInterface2016 command: " + name));
    }

    static List<CommandSpec> allCommands() {
        return java.util.stream.Stream.concat(PRIMARY_COMMANDS.stream(), SET_COMMANDS.stream()).toList();
    }

    static String readResource(String resourcePath) {
        try (InputStream in = BInterface2016StandardTestSupport.class
                .getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                fail("Missing test resource: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError("Cannot read test resource: " + resourcePath, e);
        }
    }

    static Document parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new AssertionError("Invalid XML: " + e.getMessage() + "\n" + xml, e);
        }
    }

    static Element first(Document doc, String localName) {
        return first(doc.getDocumentElement(), localName);
    }

    static Element first(Element element, String localName) {
        NodeList nodes = element.getElementsByTagName("*");
        if (matches(element, localName)) {
            return element;
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element child && matches(child, localName)) {
                return child;
            }
        }
        return null;
    }

    static String text(Document doc, String localName) {
        Element element = first(doc, localName);
        return element == null ? null : element.getTextContent().trim();
    }

    static int intText(Document doc, String localName) {
        String value = text(doc, localName);
        if (value == null || value.isBlank()) {
            throw new AssertionError("Missing numeric XML field: " + localName);
        }
        return Integer.parseInt(value);
    }

    static boolean hasTag(String xml, String localName) {
        return first(parse(xml), localName) != null;
    }

    static boolean containsResultCode(String xml) {
        return hasTag(xml, "ResultCode");
    }

    static String soapEnvelope(String bodyPayload) {
        return "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soap:Body>" + bodyPayload + "</soap:Body></soap:Envelope>";
    }

    static String requestPayload(String commandName, int code, String infoXml) {
        return "<Request><PK_Type><Name>" + commandName + "</Name><Code>" + code
                + "</Code></PK_Type><Info>" + infoXml + "</Info></Request>";
    }

    static String responsePayload(String ackName, int ackCode, String result, String tailInfoXml) {
        String tail = tailInfoXml == null ? "" : tailInfoXml;
        return "<Response><PK_Type><Name>" + ackName + "</Name><Code>" + ackCode
                + "</Code></PK_Type><Info><Result>" + result + "</Result>"
                + tail + "</Info></Response>";
    }

    static String resultOf(String xml) {
        return text(parse(xml), "Result");
    }

    static boolean is2016Success(String xml) {
        return "1".equals(resultOf(xml));
    }

    private static boolean matches(Element element, String localName) {
        String actual = element.getLocalName();
        if (actual == null) {
            actual = element.getTagName();
            int colon = actual.indexOf(':');
            if (colon >= 0) {
                actual = actual.substring(colon + 1);
            }
        }
        return localName.equals(actual);
    }
}
