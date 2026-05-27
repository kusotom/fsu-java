package com.dcim.platform.binterface;

import com.dcim.platform.module.binterface.model.BInterfaceMessage;
import com.dcim.platform.module.binterface.soap.SoapMessageHandler;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.dcim.platform.binterface.BInterface2016StandardTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-TEST-P0-001: B接口2016 XML 样本回放测试。
 *
 * 标准样本放在 fixtures/b_interface_2016_standard/，厂商样本必须放在
 * fixtures/b_interface_2016_emerson/，不得混用。
 */
class BInterface2016XmlSampleReplayTest {

    private final SoapMessageHandler soap = new SoapMessageHandler();

    @Test
    void xmlSampleIndexShouldRemainAvailableAsTheProtocolSourceIndex() {
        Path index = Path.of("../openspec/protocols/binterface-2016/matrices/xml-sample-index.md");
        if (!Files.exists(index)) {
            index = Path.of("openspec/protocols/binterface-2016/matrices/xml-sample-index.md");
        }
        assertTrue(Files.exists(index), "SPEC-P0-001 xml-sample-index.md must exist");
    }

    @Test
    void standardFixtureSetShouldCoverEachPrimaryCommandWithSuccessAndFailureSamples() {
        Set<String> successCommands = SAMPLES.stream()
                .filter(Sample::success)
                .map(Sample::command)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> failureCommands = SAMPLES.stream()
                .filter(sample -> !sample.success())
                .map(Sample::command)
                .collect(java.util.stream.Collectors.toSet());

        assertAll(PRIMARY_COMMANDS.stream().flatMap(spec -> Stream.of(
                () -> assertTrue(successCommands.contains(spec.name()),
                        "Missing standard success sample for " + spec.name()),
                () -> assertTrue(failureCommands.contains(spec.name()),
                        "Missing standard failure sample for " + spec.name()))));
    }

    @TestFactory
    Stream<DynamicTest> standardSamplesShouldReplayThroughSoapParser() {
        return SAMPLES.stream().map(sample -> DynamicTest.dynamicTest(sample.resource(), () -> {
            String xml = readResource(sample.resource());
            BInterfaceMessage message = soap.parse(xml);
            var doc = parse(xml);

            assertNotNull(first(doc, sample.root()),
                    "Expected business root " + sample.root() + " in " + sample.resource());
            assertEquals(sample.expectedPkType(), message.getPkType().name());
            assertEquals(sample.expectedPkType(), text(doc, "Name"));
            assertEquals(sample.expectedCode(), intText(doc, "Code"));
            assertNotNull(message.getInfo(), "Info must be parseable for " + sample.resource());
            assertFalse(containsResultCode(xml),
                    "standard-2016 fixtures must not contain non-standard ResultCode");

            if ("Response".equals(sample.root())) {
                assertEquals(sample.success() ? "1" : "0", text(doc, "Result"));
            }
        }));
    }

    @Test
    void parserShouldAcceptStandardNameCodePkTypeForAllSampleCommands() {
        for (Sample sample : SAMPLES) {
            String xml = readResource(sample.resource());
            BInterfaceMessage message = soap.parse(xml);
            assertNotNull(message.getPkTypeDescriptor(), sample.resource());
            assertEquals(sample.expectedPkType(), message.getPkType().name(), sample.resource());
        }
    }

    private static final List<Sample> SAMPLES = List.of(
            Sample.request("LOGIN", "login_success.request.xml"),
            Sample.failure("LOGIN", "login_failure.response.xml"),
            Sample.request("LOGOUT", "logout_success.request.xml"),
            Sample.failure("LOGOUT", "logout_failure.response.xml"),
            Sample.request("SEND_ALARM", "send_alarm_success.request.xml"),
            Sample.failure("SEND_ALARM", "send_alarm_failure.response.xml"),
            Sample.successResponse("GET_DATA", "get_data_success.response.xml"),
            Sample.failure("GET_DATA", "get_data_failure.response.xml"),
            Sample.successResponse("GET_LOGININFO", "get_logininfo_success.response.xml"),
            Sample.failure("GET_LOGININFO", "get_logininfo_failure.response.xml"),
            Sample.successResponse("GET_FTP", "get_ftp_success.response.xml"),
            Sample.failure("GET_FTP", "get_ftp_failure.response.xml"),
            Sample.successResponse("GET_FSUINFO", "get_fsuinfo_success.response.xml"),
            Sample.failure("GET_FSUINFO", "get_fsuinfo_failure.response.xml"),
            Sample.successResponse("GET_THRESHOLD", "get_threshold_success.response.xml"),
            Sample.failure("GET_THRESHOLD", "get_threshold_failure.response.xml"),
            Sample.successResponse("TIME_CHECK", "time_check_success.response.xml"),
            Sample.failure("TIME_CHECK", "time_check_failure.response.xml")
    );

    private record Sample(String command, String resource, String root,
                          String expectedPkType, int expectedCode, boolean success) {

        static Sample request(String command, String fileName) {
            CommandSpec spec = spec(command);
            return new Sample(command, prefix(fileName), "Request", spec.name(), spec.code(), true);
        }

        static Sample successResponse(String command, String fileName) {
            CommandSpec spec = spec(command);
            return new Sample(command, prefix(fileName), "Response", spec.ackName(), spec.ackCode(), true);
        }

        static Sample failure(String command, String fileName) {
            CommandSpec spec = spec(command);
            return new Sample(command, prefix(fileName), "Response", spec.ackName(), spec.ackCode(), false);
        }

        private static String prefix(String fileName) {
            return "fixtures/b_interface_2016_standard/" + fileName;
        }
    }
}
