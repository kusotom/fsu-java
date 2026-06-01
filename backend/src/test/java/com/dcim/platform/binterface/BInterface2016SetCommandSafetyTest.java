package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.safety.SetCommandSafetyDecision;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyGate;
import com.dcim.platform.module.binterface.safety.SetCommandSafetyProperties;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import static com.dcim.platform.binterface.BInterface2016StandardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-TEST-P0-001: B接口2016 SET 类命令安全门测试。
 *
 * 依据:
 * - SPEC-2016-SECURITY-SET-POINT-001
 * - SPEC-2016-SECURITY-SET-LOGININFO-001
 * - SPEC-2016-SECURITY-SET-FTP-001
 * - SPEC-2016-SECURITY-SET-FSUREBOOT-001
 * - SPEC-2016-SECURITY-SET-THRESHOLD-001
 */
class BInterface2016SetCommandSafetyTest {

    @Test
    void defaultSafetyGateShouldRejectEvery2016SetCommandRealExecution() {
        SetCommandSafetyGate gate = new SetCommandSafetyGate(new SetCommandSafetyProperties(), null);

        assertAll(SET_COMMANDS.stream().map(spec -> () -> {
            SetCommandSafetyDecision d = gate.evaluate(spec.name(), FSU_CODE,
                    null, false, 1, true);
            assertFalse(d.isAllowed(), spec.name() + " must be rejected by default");
            assertFalse(d.isRealCallAllowed(), spec.name() + " real call must be rejected by default");
            assertNotNull(d.getReasonCode(), spec.name() + " rejection must be auditable");
        }));
    }

    @Test
    void every2016SetCommandShouldBeRecognizedByTheSafetyGate() {
        SetCommandSafetyGate gate = new SetCommandSafetyGate(new SetCommandSafetyProperties(), null);

        assertAll(SET_COMMANDS.stream().map(spec -> () -> {
            SetCommandSafetyDecision d = gate.evaluate(spec.name(), FSU_CODE,
                    null, false, 1, true);
            assertNotEquals("UNKNOWN_SET_COMMAND", d.getReasonCode(),
                    spec.name() + " is a standard B接口2016 SET command and must be in the safety gate scope");
        }));
    }

    @Test
    void all2016SetCommandsShouldBeMarkedHighRisk() {
        SetCommandSafetyGate gate = new SetCommandSafetyGate(new SetCommandSafetyProperties(), null);

        assertAll(SET_COMMANDS.stream().map(spec -> () -> {
            SetCommandSafetyDecision d = gate.evaluate(spec.name(), FSU_CODE,
                    null, false, 1, true);
            assertTrue(d.isHighRisk(), spec.name() + " must be marked high risk by SPEC-P0-001");
        }));
    }

    @Test
    void schedulerTriggeredSetCommandShouldBeRejectedEvenWhenDryRunIsEnabled() {
        SetCommandSafetyProperties props = enabledDryRunProperties();
        SetCommandSafetyGate gate = new SetCommandSafetyGate(props, null);

        for (String command : new String[]{"SET_POINT", "SET_THRESHOLD", "SET_FTP", "SET_FSUREBOOT"}) {
            SetCommandSafetyDecision d = gate.evaluate(command, FSU_CODE,
                    "confirmed", true, 1, false);
            assertFalse(d.isAllowed(), command + " must not be scheduler-triggered");
            assertEquals("SCHEDULER_FORBIDDEN", d.getReasonCode(), command);
        }
    }

    @Test
    void restRunOnceControllerMustExposeReadOnlyEndpointsOnly() {
        Class<?> controller = com.dcim.platform.module.binterface.controller
                .BInterface2016ReadOnlyRunOnceController.class;
        RequestMapping root = controller.getAnnotation(RequestMapping.class);
        assertNotNull(root);
        assertTrue(String.join(",", root.value()).contains("/api/b-interface/2016/read-only"));

        for (Method method : controller.getDeclaredMethods()) {
            PostMapping post = method.getAnnotation(PostMapping.class);
            if (post == null) {
                continue;
            }
            String path = String.join(",", post.value()).toUpperCase(Locale.ROOT);
            assertFalse(path.contains("SET"), "run-once REST path must not expose SET: " + path);
            assertFalse(method.getName().toUpperCase(Locale.ROOT).contains("SET"),
                    "run-once REST method must not expose SET: " + method.getName());
        }
    }

    @Test
    void schedulerSourceShouldNotContainSetCommandExecutionEntry() throws Exception {
        Path sourceRoot = Path.of("src/main/java/com/dcim/platform/module/binterface");
        assertTrue(Files.exists(sourceRoot), "Test must run from backend Maven module");

        try (var files = Files.walk(sourceRoot)) {
            var scheduledSetFiles = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            String text = Files.readString(path);
                            String upper = text.toUpperCase(Locale.ROOT);
                            return upper.contains("@SCHEDULED")
                                    && (upper.contains("SET_POINT")
                                    || upper.contains("SET_THRESHOLD")
                                    || upper.contains("SET_LOGININFO")
                                    || upper.contains("SET_FTP")
                                    || upper.contains("SET_FSUREBOOT"));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            assertTrue(scheduledSetFiles.isEmpty(),
                    "Scheduler must not auto-trigger SET commands: " + scheduledSetFiles);
        }
    }

    @Test
    void setCommandXmlCanOnlyBeConstructedOfflineInThisTest() {
        SoapMessageHandler soap = new SoapMessageHandler();

        for (CommandSpec spec : SET_COMMANDS) {
            String payload = soap.buildRequest(spec.name(), spec.code(),
                    "<FsuCode>" + FSU_CODE + "</FsuCode>", "<OfflineSafetyTest>true</OfflineSafetyTest>");
            var doc = parse(payload);
            assertEquals(spec.name(), text(doc, "Name"));
            assertEquals(spec.code(), intText(doc, "Code"));
            assertFalse(payload.contains("http://"), "offline XML construction must not contain service URL");
        }
    }

    private SetCommandSafetyProperties enabledDryRunProperties() {
        SetCommandSafetyProperties props = new SetCommandSafetyProperties();
        props.setEnabled(true);
        props.setSchedulerForbidden(true);
        props.setAllowRealCall(false);
        props.setDryRunDefault(true);

        Map<String, String> normalized = Map.of(
                "SET_POINT", "SET_RMCTRLCMD",
                "SET_THRESHOLD", "SET_THRESHOLD",
                "SET_FTP", "SET_SUFTP",
                "SET_FSUREBOOT", "SET_SUREBOOT"
        );
        for (String name : normalized.values()) {
            SetCommandSafetyProperties.CommandSafetyConfig cfg =
                    new SetCommandSafetyProperties.CommandSafetyConfig();
            cfg.setEnabled(true);
            cfg.setAllowRealCall(false);
            cfg.setRequireConfirmation(true);
            cfg.setAuditRequired(true);
            props.getCommands().put(name, cfg);
        }
        return props;
    }
}
