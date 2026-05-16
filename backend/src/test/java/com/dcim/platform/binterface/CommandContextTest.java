package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.command.CommandContext;
import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.model.BInterfacePkType;
import com.dcim.platform.module.binterface.xml.XmlDataModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * B接口 CommandContext 基础模型测试。
 */
class CommandContextTest {

    @Test
    void shouldCreateContext() {
        BInterfaceMessage msg = new BInterfaceMessage();
        msg.setPkType(BInterfacePkType.HEARTBEAT);
        XmlDataModel xmlData = new XmlDataModel();
        xmlData.setField("CPU", "35");

        CommandContext ctx = new CommandContext(BInterfacePkType.HEARTBEAT, msg, xmlData, "<soap>", "<CPU>35</CPU>");

        assertEquals(BInterfacePkType.HEARTBEAT, ctx.getPkType());
        assertSame(msg, ctx.getSoapMessage());
        assertSame(xmlData, ctx.getXmlData());
        assertEquals("<soap>", ctx.getRawSoap());
        assertEquals("<CPU>35</CPU>", ctx.getRawXmlData());
    }

    @Test
    void shouldHandleAttributes() {
        CommandContext ctx = new CommandContext(
                BInterfacePkType.LOGIN, new BInterfaceMessage(), new XmlDataModel(), null, null);

        ctx.setAttribute("key1", "value1");
        ctx.setAttribute("key2", 42);

        assertEquals("value1", ctx.getAttribute("key1"));
        assertEquals(42, (int) ctx.getAttribute("key2"));
        assertNull(ctx.getAttribute("nonexistent"));
    }

    @Test
    void shouldHandleNullValues() {
        CommandContext ctx = new CommandContext(null, null, null, null, null);
        assertNull(ctx.getPkType());
        assertNull(ctx.getSoapMessage());
        assertNull(ctx.getXmlData());
    }

    @Test
    void toStringShouldNotThrow() {
        CommandContext ctx = new CommandContext(
                BInterfacePkType.SEND_DATA, new BInterfaceMessage(), new XmlDataModel(), null, null);
        assertNotNull(ctx.toString());
    }
}
