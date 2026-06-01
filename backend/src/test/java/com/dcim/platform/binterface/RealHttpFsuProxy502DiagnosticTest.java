package com.dcim.platform.binterface;

import org.junit.jupiter.api.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BIF2016-NETWORK-002: Java HttpURLConnection proxy 502 diagnosis.
 * Default @Disabled, requires -DrealFsuTest.enabled=true to run.
 * Tests compare Java vs curl proxy POST behavior against Emerson FSU.
 */
@Tag("real-fsu")
@Disabled("Requires -DrealFsuTest.enabled=true")
class RealHttpFsuProxy502DiagnosticTest {

    private static final String FSU_URL = "http://192.168.100.100:8080/services/FSUService";
    private static final String PROXY_HOST = "127.0.0.1";
    private static final int PROXY_PORT = 7890;
    private static final String REQUEST_FILE =
            "/home/tom/桌面/FSU/docs/landing/raw-samples/data-mapping-008-retry-get-data-request.xml";

    private static byte[] requestBody;
    private static Proxy proxy;

    @BeforeAll
    static void setUp() throws Exception {
        requestBody = Files.readAllBytes(Path.of(REQUEST_FILE));
        proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(PROXY_HOST, PROXY_PORT));
        System.out.println("Request: " + new String(requestBody, 0, Math.min(200, requestBody.length)));
    }

    // ── Baseline: curl-matching headers ──

    @Test
    void curlCompatibleHeadersShouldReturn200() throws Exception {
        HttpURLConnection c = connect();
        setCurlHeaders(c);
        c.setFixedLengthStreamingMode((long) requestBody.length);
        try (OutputStream os = c.getOutputStream()) { os.write(requestBody); }
        int code = c.getResponseCode();
        String body = readBody(c);
        boolean hasAck = body.contains("GET_DATA_ACK") || body.contains("<Code>402</Code>");
        System.out.println("curl-matching: HTTP " + code + " ack=" + hasAck + " len=" + body.length());
        if (!hasAck && code >= 400) {
            System.out.println("  error: " + body.substring(0, Math.min(500, body.length())));
        }
    }

    // ── Variable experiments ──

    @Test
    void experimentConnectionClose() throws Exception {
        HttpURLConnection c = connect();
        setCurlHeaders(c);
        c.setRequestProperty("Connection", "close");
        c.setFixedLengthStreamingMode((long) requestBody.length);
        try (OutputStream os = c.getOutputStream()) { os.write(requestBody); }
        int code = c.getResponseCode();
        System.out.println("Connection:close => HTTP " + code);
    }

    @Test
    void experimentNoConnectionHeader() throws Exception {
        HttpURLConnection c = connect();
        c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        c.setRequestProperty("SOAPAction", "");
        c.setFixedLengthStreamingMode((long) requestBody.length);
        try (OutputStream os = c.getOutputStream()) { os.write(requestBody); }
        int code = c.getResponseCode();
        System.out.println("no Connection header => HTTP " + code);
    }

    @Test
    void experimentNoSOAPAction() throws Exception {
        HttpURLConnection c = connect();
        c.setRequestMethod("POST"); c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        c.setRequestProperty("Connection", "close");
        c.setFixedLengthStreamingMode((long) requestBody.length);
        try (OutputStream os = c.getOutputStream()) { os.write(requestBody); }
        int code = c.getResponseCode();
        System.out.println("no SOAPAction => HTTP " + code);
    }

    @Test
    void experimentChunkedStreamingMode() throws Exception {
        HttpURLConnection c = connect();
        setCurlHeaders(c);
        c.setChunkedStreamingMode(0);
        try (OutputStream os = c.getOutputStream()) { os.write(requestBody); }
        int code = c.getResponseCode();
        System.out.println("chunked => HTTP " + code);
    }

    @Test
    void experimentNoFixedLength() throws Exception {
        HttpURLConnection c = connect();
        setCurlHeaders(c);
        // Don't set fixedLengthStreamingMode — let Java decide
        try (OutputStream os = c.getOutputStream()) { os.write(requestBody); }
        int code = c.getResponseCode();
        System.out.println("no fixedLength => HTTP " + code);
    }

    @Test
    void experimentJDKHttpClientMinimal() throws Exception {
        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .proxy(java.net.http.HttpClient.Builder.NO_PROXY).build();
        // Connect directly to proxy, sending absolute URI
        var jdkClient = java.net.http.HttpClient.newBuilder()
                .proxy(java.net.ProxySelector.of(new InetSocketAddress(PROXY_HOST, PROXY_PORT)))
                .build();
        var req = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(FSU_URL))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "")
                .header("Connection", "close")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(requestBody))
                .build();
        var resp = jdkClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        boolean hasAck = resp.body().contains("GET_DATA_ACK") || resp.body().contains("<Code>402</Code>");
        System.out.println("JDK HttpClient: HTTP " + resp.statusCode() + " ack=" + hasAck + " len=" + resp.body().length());
    }

    @Test
    void experimentDirectPOSTProxy() throws Exception {
        // Send raw HTTP to proxy directly (bypass HttpURLConnection)
        try (Socket s = new Socket(PROXY_HOST, PROXY_PORT)) {
            s.setSoTimeout(10000);
            var w = new PrintWriter(s.getOutputStream(), false, StandardCharsets.UTF_8);
            w.print("POST " + FSU_URL + " HTTP/1.1\r\n");
            w.print("Host: 192.168.100.100:8080\r\n");
            w.print("Content-Type: text/xml; charset=utf-8\r\n");
            w.print("SOAPAction: \"\"\r\n");
            w.print("Connection: close\r\n");
            w.print("Content-Length: " + requestBody.length + "\r\n");
            w.print("\r\n");
            w.flush();
            s.getOutputStream().write(requestBody);
            s.getOutputStream().flush();
            byte[] resp = s.getInputStream().readAllBytes();
            String respStr = new String(resp, StandardCharsets.UTF_8);
            boolean hasAck = respStr.contains("GET_DATA_ACK");
            System.out.println("raw socket proxy POST: hasAck=" + hasAck + " len=" + respStr.length());
            s.close();
        }
    }

    // ── Helpers ──

    private HttpURLConnection connect() throws Exception {
        URL url = new URL(FSU_URL);
        HttpURLConnection c = (HttpURLConnection) url.openConnection(proxy);
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        return c;
    }

    private void setCurlHeaders(HttpURLConnection c) {
        c.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        c.setRequestProperty("SOAPAction", "\"\"");
        c.setRequestProperty("Connection", "close");
        c.setRequestProperty("User-Agent", "curl/7.81.0");
    }

    private String readBody(HttpURLConnection c) throws Exception {
        try (var is = c.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
