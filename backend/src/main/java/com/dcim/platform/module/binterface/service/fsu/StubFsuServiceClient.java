package com.dcim.platform.module.binterface.service.fsu;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import com.dcim.platform.module.binterface.xml.XmlDataParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stub FSU 服务客户端（默认实现）。
 *
 * <p>使用内置预设响应返回 GET_DATA 结果，不访问真实设备，不访问网络。
 * 仅用于开发和测试阶段。</p>
 *
 * <p>当 {@code b-interface.fsu-client.real-call-enabled=true} 时此 Bean 不激活。</p>
 */
@Component
@ConditionalOnProperty(name = "b-interface.fsu-client.real-call-enabled", havingValue = "false", matchIfMissing = true)
public class StubFsuServiceClient implements FsuServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StubFsuServiceClient.class);

    /**
     * 内置默认 GET_DATA 响应（SOAP 格式）。
     * 正常情况返回 5 个信号的数据，用于测试和设备开发。
     */
    /**
     * 内置默认 GET_THRESHOLD 响应（SOAP 格式）。
     * 返回 3 个信号的门限数据，用于测试和设备开发。
     */
    static final String DEFAULT_GET_THRESHOLD_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>GET_THRESHOLD</PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "        <Count>3</Count>\n"
            + "      </Info>\n"
            + "      <xmlData>\n"
            + "        <Signal>\n"
            + "          <SignalID>TEMP-001</SignalID>\n"
            + "          <AlarmUpper>60.0</AlarmUpper>\n"
            + "          <AlarmLower>-5.0</AlarmLower>\n"
            + "          <AlarmUpperUrgent>70.0</AlarmUpperUrgent>\n"
            + "          <AlarmLowerUrgent>-10.0</AlarmLowerUrgent>\n"
            + "        </Signal>\n"
            + "        <Signal>\n"
            + "          <SignalID>HUMI-001</SignalID>\n"
            + "          <AlarmUpper>90.0</AlarmUpper>\n"
            + "          <AlarmLower>10.0</AlarmLower>\n"
            + "          <AlarmUpperUrgent>95.0</AlarmUpperUrgent>\n"
            + "          <AlarmLowerUrgent>5.0</AlarmLowerUrgent>\n"
            + "        </Signal>\n"
            + "        <Signal>\n"
            + "          <SignalID>VOLT-001</SignalID>\n"
            + "          <AlarmUpper>245.0</AlarmUpper>\n"
            + "          <AlarmLower>198.0</AlarmLower>\n"
            + "          <AlarmUpperUrgent>260.0</AlarmUpperUrgent>\n"
            + "          <AlarmLowerUrgent>185.0</AlarmLowerUrgent>\n"
            + "        </Signal>\n"
            + "      </xmlData>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    /**
     * 内置默认 TIME_CHECK 响应（SOAP 格式）。
     * 返回 ResultCode=0 和 FSUTime（固定值，不修改系统时间）。
     */
    static final String DEFAULT_TIME_CHECK_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>TIME_CHECK</PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "        <FSUTime>2026-05-13T10:42:01+08:00</FSUTime>\n"
            + "      </Info>\n"
            + "      <xmlData/>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    /**
     * 内置默认 GET_LOGININFO 响应（SOAP 格式）。
     * 返回 FSU 登录/在线状态信息。
     */
    static final String DEFAULT_GET_LOGININFO_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>GET_LOGININFO</PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "      </Info>\n"
            + "      <xmlData>\n"
            + "        <LoginInfo>\n"
            + "          <FSUCode>FSU-001</FSUCode>\n"
            + "          <LoginStatus>LOGIN</LoginStatus>\n"
            + "          <OnlineStatus>ONLINE</OnlineStatus>\n"
            + "          <SessionID>SESSION-FSU-001-20260513-A3B8</SessionID>\n"
            + "          <LoginTime>2026-05-13T10:30:00+08:00</LoginTime>\n"
            + "          <LastHeartbeat>2026-05-13T10:40:00+08:00</LastHeartbeat>\n"
            + "        </LoginInfo>\n"
            + "      </xmlData>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    /**
     * 内置默认 GET_FTP 响应（SOAP 格式）。
     * 返回 FSU 的 FTP 配置参数。
     */
    static final String DEFAULT_GET_FTP_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>GET_FTP</PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "      </Info>\n"
            + "      <xmlData>\n"
            + "        <FTPConfig>\n"
            + "          <Host>192.168.1.200</Host>\n"
            + "          <Port>21</Port>\n"
            + "          <Username>fsu_ftp</Username>\n"
            + "          <PassiveMode>true</PassiveMode>\n"
            + "          <BasePath>/fsu/images/</BasePath>\n"
            + "        </FTPConfig>\n"
            + "      </xmlData>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    /**
     * 内置默认 GET_SUINFO 响应（SOAP 格式，BIF-P4-012）。
     * 返回 SUID/CPUUsage/MEMUsage/SUDateTime/Result=SUCCESS。
     */
    static final String DEFAULT_GET_SUINFO_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>\n"
            + "        <Name>GET_SUINFO_ACK</Name>\n"
            + "        <Code>1002</Code>\n"
            + "      </PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "      </Info>\n"
            + "      <xmlData>\n"
            + "        <TSUStatus>\n"
            + "          <SUID>FSU-001</SUID>\n"
            + "          <CPUUsage>35.2</CPUUsage>\n"
            + "          <MEMUsage>62.8</MEMUsage>\n"
            + "          <SUDateTime>2026-05-15 12:00:00</SUDateTime>\n"
            + "        </TSUStatus>\n"
            + "      </xmlData>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    /**
     * 内置默认 GET_SUFTP 响应（SOAP 格式，BIF-P4-013）。
     * Password 为占位值，不包含真实凭证。
     */
    static final String DEFAULT_GET_SUFTP_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>\n"
            + "        <Name>GET_SUFTP_ACK</Name>\n"
            + "        <Code>802</Code>\n"
            + "      </PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "      </Info>\n"
            + "      <xmlData>\n"
            + "        <SUFTPConfig>\n"
            + "          <SUID>FSU-001</SUID>\n"
            + "          <UserName>fsu_ftp_user</UserName>\n"
            + "          <Password>placeholder_not_real</Password>\n"
            + "          <FTPPort>21</FTPPort>\n"
            + "        </SUFTPConfig>\n"
            + "      </xmlData>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    /**
     * 内置默认 SET_TIME 响应（SOAP 格式，BIF-P4-014）。
     * Result=SUCCESS，不修改任何系统/设备时间。
     */
    static final String DEFAULT_SET_TIME_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>\n"
            + "        <Name>SET_TIME_ACK</Name>\n"
            + "        <Code>902</Code>\n"
            + "      </PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "      </Info>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    /**
     * 内置默认 SET_THRESHOLD 响应（SOAP 格式）。
     * 返回 ResultCode=0 表示设置成功。
     */
    static final String DEFAULT_SET_THRESHOLD_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>SET_THRESHOLD</PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "        <Count>1</Count>\n"
            + "      </Info>\n"
            + "      <xmlData/>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    static final String DEFAULT_GET_DATA_RESPONSE =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "  <soap:Body>\n"
            + "    <Response>\n"
            + "      <PK_Type>GET_DATA</PK_Type>\n"
            + "      <Info>\n"
            + "        <ResultCode>0</ResultCode>\n"
            + "        <Count>5</Count>\n"
            + "      </Info>\n"
            + "      <xmlData>\n"
            + "        <Signal>\n"
            + "          <SignalID>TEMP-001</SignalID>\n"
            + "          <Value>25.8</Value>\n"
            + "          <Quality>1</Quality>\n"
            + "          <Status>NORMAL</Status>\n"
            + "          <CollectTime>2026-05-13T10:35:00+08:00</CollectTime>\n"
            + "        </Signal>\n"
            + "        <Signal>\n"
            + "          <SignalID>HUMI-001</SignalID>\n"
            + "          <Value>54.5</Value>\n"
            + "          <Quality>1</Quality>\n"
            + "          <Status>NORMAL</Status>\n"
            + "          <CollectTime>2026-05-13T10:35:00+08:00</CollectTime>\n"
            + "        </Signal>\n"
            + "        <Signal>\n"
            + "          <SignalID>VOLT-001</SignalID>\n"
            + "          <Value>221.2</Value>\n"
            + "          <Quality>1</Quality>\n"
            + "          <Status>NORMAL</Status>\n"
            + "          <CollectTime>2026-05-13T10:35:00+08:00</CollectTime>\n"
            + "        </Signal>\n"
            + "        <Signal>\n"
            + "          <SignalID>DOOR-001</SignalID>\n"
            + "          <Value>CLOSE</Value>\n"
            + "          <Quality>1</Quality>\n"
            + "          <Status>NORMAL</Status>\n"
            + "          <CollectTime>2026-05-13T10:35:00+08:00</CollectTime>\n"
            + "        </Signal>\n"
            + "        <Signal>\n"
            + "          <SignalID>WATER-001</SignalID>\n"
            + "          <Value>DRY</Value>\n"
            + "          <Quality>1</Quality>\n"
            + "          <Status>NORMAL</Status>\n"
            + "          <CollectTime>2026-05-13T10:35:00+08:00</CollectTime>\n"
            + "        </Signal>\n"
            + "      </xmlData>\n"
            + "    </Response>\n"
            + "  </soap:Body>\n"
            + "</soap:Envelope>";

    private final SoapMessageHandler soapMessageHandler;
    private final XmlDataParser xmlDataParser;

    public StubFsuServiceClient(SoapMessageHandler soapMessageHandler, XmlDataParser xmlDataParser) {
        this.soapMessageHandler = soapMessageHandler;
        this.xmlDataParser = xmlDataParser;
        log.info("StubFsuServiceClient 已初始化（默认实现，不访问真实设备）");
    }

    @Override
    public FsuServiceResponse call(FsuServiceRequest request) {
        if (request == null) {
            return FsuServiceResponse.error("请求参数为空");
        }

        log.info("StubFSU 调用: pkType={}, fsuCode={}, serviceUrl={}",
                request.getPkType(), request.getFsuCode(), request.getServiceUrl());

        try {
            switch (request.getPkType()) {
                case GET_DATA:
                    return handleGetData(request);
                case GET_THRESHOLD:
                    return handleGetThreshold(request);
                case SET_THRESHOLD:
                    return handleSetThreshold(request);
                case TIME_CHECK:
                    return handleTimeCheck(request);
                case GET_LOGININFO:
                    return handleGetLoginInfo(request);
                case GET_FTP:
                    return handleGetFtp(request);
                case GET_SUINFO:
                    return handleGetSuInfo(request);
                case GET_SUFTP:
                    return handleGetSuFtp(request);
                case SET_TIME:
                    return handleSetTime(request);
                case GET_SPCONFIGOPTION:
                    return handleGetSpConfigOption(request);
                case GET_ACTIVEALARM:
                    return handleGetActiveAlarm(request);
                default:
                    return FsuServiceResponse.fail("1",
                            "StubFsuServiceClient 不支持的 PK_Type: " + request.getPkType());
            }
        } catch (Exception e) {
            log.error("StubFSU 调用异常: pkType={}, fsuCode={}", request.getPkType(), request.getFsuCode(), e);
            return FsuServiceResponse.error("StubFSU 处理异常: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleGetThreshold(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_GET_THRESHOLD_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            log.debug("StubFSU GET_THRESHOLD 返回: count={}, signals={}",
                    xmlData.itemCount(), xmlData.getItemFieldValues("SignalID"));

            return FsuServiceResponse.success(DEFAULT_GET_THRESHOLD_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU GET_THRESHOLD 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_THRESHOLD 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleSetThreshold(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_SET_THRESHOLD_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null && !rawXmlData.trim().isEmpty())
                    ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            log.debug("StubFSU SET_THRESHOLD 返回: resultCode={}",
                    infoXml != null ? extractResultCode(infoXml) : "unknown");

            return FsuServiceResponse.success(DEFAULT_SET_THRESHOLD_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU SET_THRESHOLD 处理失败", e);
            return FsuServiceResponse.error("StubFSU SET_THRESHOLD 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleTimeCheck(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_TIME_CHECK_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();

            log.debug("StubFSU TIME_CHECK 返回: fsuTime={}",
                    extractFsuTime(infoXml));

            XmlDataModel xmlData = (rawXmlData != null && !rawXmlData.trim().isEmpty())
                    ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();
            return FsuServiceResponse.success(DEFAULT_TIME_CHECK_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU TIME_CHECK 处理失败", e);
            return FsuServiceResponse.error("StubFSU TIME_CHECK 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleGetLoginInfo(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_GET_LOGININFO_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            log.debug("StubFSU GET_LOGININFO 返回: loginStatus={}",
                    xmlData.getFirstItemField("LoginStatus"));

            return FsuServiceResponse.success(DEFAULT_GET_LOGININFO_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU GET_LOGININFO 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_LOGININFO 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleGetSuInfo(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_GET_SUINFO_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            log.debug("StubFSU GET_SUINFO 返回: suid={}, cpu={}, mem={}",
                    xmlData.getFirstItemField("SUID"),
                    xmlData.getFirstItemField("CPUUsage"),
                    xmlData.getFirstItemField("MEMUsage"));

            return FsuServiceResponse.success(DEFAULT_GET_SUINFO_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU GET_SUINFO 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_SUINFO 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleSetTime(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_SET_TIME_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null && !rawXmlData.trim().isEmpty())
                    ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();
            log.debug("StubFSU SET_TIME 返回: resultCode=0");
            return FsuServiceResponse.success(DEFAULT_SET_TIME_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU SET_TIME 处理失败", e);
            return FsuServiceResponse.error("StubFSU SET_TIME 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleGetActiveAlarm(FsuServiceRequest request) {
        try {
            String resp = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                    + "  <soap:Body>\n"
                    + "    <Response>\n"
                    + "      <PK_Type>\n"
                    + "        <Name>GET_ACTIVEALARM_ACK</Name>\n"
                    + "        <Code>604</Code>\n"
                    + "      </PK_Type>\n"
                    + "      <Info>\n"
                    + "        <ResultCode>0</ResultCode>\n"
                    + "      </Info>\n"
                    + "      <xmlData>\n"
                    + "        <Alarm>\n"
                    + "          <SerialNo>0012345678</SerialNo>\n"
                    + "          <DeviceID>11010110100001</DeviceID>\n"
                    + "          <SPID>0430101001</SPID>\n"
                    + "          <StartTime>2026-05-16 08:00:00</StartTime>\n"
                    + "          <TriggerVal>46.1</TriggerVal>\n"
                    + "          <AlarmLevel>二级</AlarmLevel>\n"
                    + "          <AlarmFlag>开始</AlarmFlag>\n"
                    + "          <AlarmDesc>欠压告警</AlarmDesc>\n"
                    + "          <AlarmFriDesc>电池电压低于门限</AlarmFriDesc>\n"
                    + "        </Alarm>\n"
                    + "      </xmlData>\n"
                    + "    </Response>\n"
                    + "  </soap:Body>\n"
                    + "</soap:Envelope>";
            BInterfaceMessage msg = soapMessageHandler.parse(resp);
            String infoXml = msg.getInfo();
            String rawXmlData = msg.getXmlData();
            XmlDataModel xd = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();
            log.debug("StubFSU GET_ACTIVEALARM 返回: alarmCount={}", xd.itemCount());
            return FsuServiceResponse.success(resp, infoXml, rawXmlData, xd);
        } catch (Exception e) {
            log.error("StubFSU GET_ACTIVEALARM 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_ACTIVEALARM 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleGetSpConfigOption(FsuServiceRequest request) {
        try {
            String resp = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                    + "  <soap:Body>\n"
                    + "    <Response>\n"
                    + "      <PK_Type>\n"
                    + "        <Name>GET_SPCONFIGOPTION_ACK</Name>\n"
                    + "        <Code>402</Code>\n"
                    + "      </PK_Type>\n"
                    + "      <Info>\n"
                    + "        <ResultCode>0</ResultCode>\n"
                    + "      </Info>\n"
                    + "    </Response>\n"
                    + "  </soap:Body>\n"
                    + "</soap:Envelope>";
            BInterfaceMessage msg = soapMessageHandler.parse(resp);
            XmlDataModel xd = new XmlDataModel();
            log.debug("StubFSU GET_SPCONFIGOPTION 返回: resultCode=0");
            return FsuServiceResponse.success(resp, msg.getInfo(), msg.getXmlData(), xd);
        } catch (Exception e) {
            log.error("StubFSU GET_SPCONFIGOPTION 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_SPCONFIGOPTION 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleGetSuFtp(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_GET_SUFTP_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            log.debug("StubFSU GET_SUFTP 返回: suid={}, userName={}",
                    xmlData.getFirstItemField("SUID"),
                    xmlData.getFirstItemField("UserName"));

            return FsuServiceResponse.success(DEFAULT_GET_SUFTP_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU GET_SUFTP 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_SUFTP 处理失败: " + e.getMessage());
        }
    }

    private FsuServiceResponse handleGetFtp(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_GET_FTP_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            log.debug("StubFSU GET_FTP 返回: host={}, port={}",
                    xmlData.getFirstItemField("Host"),
                    xmlData.getFirstItemField("Port"));

            return FsuServiceResponse.success(DEFAULT_GET_FTP_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU GET_FTP 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_FTP 处理失败: " + e.getMessage());
        }
    }

    private String extractFsuTime(String infoXml) {
        if (infoXml == null) return null;
        int start = infoXml.indexOf("<FSUTime>");
        if (start < 0) {
            start = infoXml.indexOf("<fsutime>");
            if (start < 0) return null;
        }
        start = infoXml.indexOf('>', start) + 1;
        int end = infoXml.indexOf("</FSUTime>", start);
        if (end < 0) {
            end = infoXml.indexOf("</fsutime>", start);
            if (end < 0) return null;
        }
        return infoXml.substring(start, end).trim();
    }

    private String extractResultCode(String infoXml) {
        int start = infoXml.indexOf("<ResultCode>");
        if (start < 0) return "unknown";
        start += "<ResultCode>".length();
        int end = infoXml.indexOf("</ResultCode>", start);
        if (end < 0) return "unknown";
        return infoXml.substring(start, end).trim();
    }

    private FsuServiceResponse handleGetData(FsuServiceRequest request) {
        try {
            BInterfaceMessage message = soapMessageHandler.parse(DEFAULT_GET_DATA_RESPONSE);
            String infoXml = message.getInfo();
            String rawXmlData = message.getXmlData();
            XmlDataModel xmlData = (rawXmlData != null) ? xmlDataParser.parse(rawXmlData) : new XmlDataModel();

            log.debug("StubFSU GET_DATA 返回: count={}, signals={}",
                    xmlData.itemCount(), xmlData.getItemFieldValues("SignalID"));

            return FsuServiceResponse.success(DEFAULT_GET_DATA_RESPONSE, infoXml, rawXmlData, xmlData);
        } catch (Exception e) {
            log.error("StubFSU GET_DATA 处理失败", e);
            return FsuServiceResponse.error("StubFSU GET_DATA 处理失败: " + e.getMessage());
        }
    }
}
