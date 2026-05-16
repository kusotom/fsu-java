package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandResult;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 CommandResult 基础模型测试。
 */
class CommandResultTest {

    @Test
    void shouldCreateNotImplementedResult() {
        CommandResult result = CommandResult.notImplemented(BInterfacePkType.SEND_DATA);
        assertFalse(result.isSuccess());
        assertEquals("1", result.getResultCode());
        assertEquals("Handler not implemented (BIF-P1 pending)", result.getResultDesc());
        assertFalse(result.isImplemented());
        assertEquals(BInterfacePkType.SEND_DATA, result.getPkType());
    }

    @Test
    void shouldCreateSuccessResult() {
        CommandResult result = CommandResult.success(BInterfacePkType.HEARTBEAT);
        assertTrue(result.isSuccess());
        assertEquals("0", result.getResultCode());
        assertTrue(result.isImplemented());
    }

    @Test
    void shouldCreateErrorResult() {
        CommandResult result = CommandResult.error(BInterfacePkType.LOGIN, "2001", "参数错误");
        assertFalse(result.isSuccess());
        assertEquals("2001", result.getResultCode());
        assertEquals("参数错误", result.getResultDesc());
        assertTrue(result.hasErrors());
    }

    @Test
    void shouldConvertToInfoXml() {
        CommandResult result = CommandResult.success(BInterfacePkType.GET_DATA);
        String infoXml = result.toInfoXml();
        assertNotNull(infoXml);
        assertTrue(infoXml.contains("<ResultCode>0</ResultCode>"));
    }

    @Test
    void shouldConvertXmlDataModelToXml() {
        CommandResult result = CommandResult.success(BInterfacePkType.HEARTBEAT);
        XmlDataModel model = new XmlDataModel();
        model.setField("CPU", "35");
        result.setResponseXmlData(model);

        String xmlDataXml = result.toXmlDataXml();
        assertNotNull(xmlDataXml);
        assertTrue(xmlDataXml.contains("<CPU>35</CPU>"));
    }

    @Test
    void shouldReturnNullForEmptyXmlData() {
        CommandResult result = CommandResult.success(BInterfacePkType.LOGIN);
        assertNull(result.toXmlDataXml()); // responseXmlData 为 null
    }

    @Test
    void shouldHandleErrors() {
        CommandResult result = CommandResult.error(BInterfacePkType.UNKNOWN, "1002", "错误");
        assertEquals(1, result.getErrors().size());
        assertEquals("错误", result.getErrors().get(0));
    }

    @Test
    void toStringShouldNotThrow() {
        CommandResult result = CommandResult.success(BInterfacePkType.TIME_CHECK);
        assertNotNull(result.toString());
    }
}
