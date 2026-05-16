package com.dcim.platform.binterface.command;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.command.GetFtpCommandHandler;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.entity.BInterfaceFsuStatusEntity;
import com.dcim.platform.module.binterface.service.GetFtpResult;
import com.dcim.platform.module.binterface.service.GetFtpService;
import com.dcim.platform.module.binterface.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GetFtpCommandHandler 单元测试。
 */
class GetFtpCommandHandlerTest {

    private GetFtpCommandHandler handler;
    private StubLoginService loginService;
    private StubGetFtpService getFtpService;

    @BeforeEach
    void setUp() {
        loginService = new StubLoginService();
        getFtpService = new StubGetFtpService();
        handler = new GetFtpCommandHandler(loginService, getFtpService);
    }

    // ==================== 成功 ====================

    @Test
    void shouldHandleGetFtpSuccessfully() {
        loginService.loggedIn = true;
        getFtpService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", "IMAGE"));

        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertTrue(result.isImplemented());
        assertEquals(BInterfacePkType.GET_FTP, result.getPkType());
    }

    @Test
    void shouldIncludeFtpConfigInXmlData() {
        loginService.loggedIn = true;
        getFtpService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", "IMAGE"));

        String xmlData = result.toXmlDataXml();
        assertNotNull(xmlData);
        assertTrue(xmlData.contains("192.168.1.200"));
        assertTrue(xmlData.contains("FTPConfig"));
    }

    @Test
    void shouldWorkWithoutFileType() {
        loginService.loggedIn = true;
        getFtpService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", null));

        assertTrue(result.isSuccess());
    }

    // ==================== pkType ====================

    @Test
    void shouldSupportGetFtpPkType() {
        assertEquals(BInterfacePkType.GET_FTP, handler.getSupportedPkType());
    }

    // ==================== 校验失败 ====================

    @Test
    void shouldFailOnNullContext() {
        CommandResult result = handler.handle(null);

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
        assertEquals(BInterfacePkType.GET_FTP, result.getPkType());
    }

    @Test
    void shouldFailOnNullSoapMessage() {
        CommandContext ctx = new CommandContext(BInterfacePkType.GET_FTP, null, null, null, null);
        CommandResult result = handler.handle(ctx);

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnMissingFsuCode() {
        loginService.loggedIn = true;

        CommandResult result = handler.handle(createContext(null, "IMAGE"));

        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
    }

    @Test
    void shouldFailOnNotLoggedIn() {
        loginService.loggedIn = false;
        getFtpService.successResult = true;

        CommandResult result = handler.handle(createContext("FSU-001", "IMAGE"));

        assertFalse(result.isSuccess());
        assertEquals("1002", result.getResultCode());
    }

    // ==================== Service 错误 ====================

    @Test
    void shouldFailWhenServiceReturnsError() {
        loginService.loggedIn = true;
        getFtpService.successResult = false;
        getFtpService.resultCodeToReturn = "5001";

        CommandResult result = handler.handle(createContext("FSU-001", "IMAGE"));

        assertFalse(result.isSuccess());
        assertEquals("5001", result.getResultCode());
    }

    // ==================== extractFsuCode ====================

    @Test
    void extractFsuCodeShouldReturnCorrectValue() {
        String fsuCode = GetFtpCommandHandler.extractFsuCode("<FSUCode>FSU-001</FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldBeCaseInsensitive() {
        String fsuCode = GetFtpCommandHandler.extractFsuCode("<fsucode>FSU-001</fsucode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullForEmptyInput() {
        assertNull(GetFtpCommandHandler.extractFsuCode(null));
        assertNull(GetFtpCommandHandler.extractFsuCode(""));
    }

    @Test
    void extractFsuCodeShouldHandleWhitespace() {
        String fsuCode = GetFtpCommandHandler.extractFsuCode(
                "<FSUCode>  FSU-001  </FSUCode>");
        assertEquals("FSU-001", fsuCode);
    }

    @Test
    void extractFsuCodeShouldReturnNullWhenNotFound() {
        String fsuCode = GetFtpCommandHandler.extractFsuCode("<ResultCode>0</ResultCode>");
        assertNull(fsuCode);
    }

    // ==================== extractFileType ====================

    @Test
    void extractFileTypeShouldReturnCorrectValue() {
        String type = GetFtpCommandHandler.extractFileType("<FileType>IMAGE</FileType>");
        assertEquals("IMAGE", type);
    }

    @Test
    void extractFileTypeShouldBeCaseInsensitive() {
        String type = GetFtpCommandHandler.extractFileType("<filetype>VIDEO</filetype>");
        assertEquals("VIDEO", type);
    }

    @Test
    void extractFileTypeShouldReturnNullForEmptyInput() {
        assertNull(GetFtpCommandHandler.extractFileType(null));
        assertNull(GetFtpCommandHandler.extractFileType(""));
    }

    @Test
    void extractFileTypeShouldHandleWhitespace() {
        String type = GetFtpCommandHandler.extractFileType(
                "<FileType>  IMAGE  </FileType>");
        assertEquals("IMAGE", type);
    }

    @Test
    void extractFileTypeShouldReturnNullWhenNotFound() {
        assertNull(GetFtpCommandHandler.extractFileType("<ResultCode>0</ResultCode>"));
    }

    @Test
    void extractFileTypeShouldReturnNullForEmptyTag() {
        assertNull(GetFtpCommandHandler.extractFileType("<FileType>  </FileType>"));
    }

    // ==================== Helper ====================

    private CommandContext createContext(String fsuCode, String fileType) {
        BInterfaceMessage msg = new BInterfaceMessage();
        StringBuilder info = new StringBuilder();
        if (fsuCode != null) {
            info.append("<FSUCode>").append(fsuCode).append("</FSUCode>");
        }
        if (fileType != null) {
            info.append("<FileType>").append(fileType).append("</FileType>");
        }
        if (info.length() == 0) {
            info.append("<ResultCode>0</ResultCode>");
        }
        msg.setInfo(info.toString());
        msg.setPkType(BInterfacePkType.GET_FTP);
        return new CommandContext(BInterfacePkType.GET_FTP, msg, null, null, null);
    }

    // ==================== Stubs ====================

    static class StubLoginService extends LoginService {
        boolean loggedIn = true;

        StubLoginService() {
            super(null, null, null);
        }

        @Override
        public boolean isLoggedIn(String fsuCode) {
            return loggedIn;
        }

        @Override
        public Optional<BInterfaceFsuStatusEntity> getStatus(String fsuCode) {
            return Optional.empty();
        }
    }

    static class StubGetFtpService extends GetFtpService {
        boolean successResult = true;
        String resultCodeToReturn = "0";

        StubGetFtpService() {
            super(null, null);
        }

        @Override
        public GetFtpResult execute(String fsuCode, String fsuServiceUrl, String fileType) {
            if (successResult) {
                return GetFtpResult.success(fsuCode, "192.168.1.200", 21,
                        "fsu_ftp", true, "/fsu/images/");
            }
            return GetFtpResult.fail(resultCodeToReturn, "服务错误", fsuCode);
        }
    }
}
