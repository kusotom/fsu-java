package com.dcim.platform.binterface;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import static com.dcim.platform.binterface.BInterface2016StandardTestSupport.readResource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * SPEC-TEST-P0-001: Emerson Profile 差异隔离测试。
 *
 * 依据:
 * - SPEC-2016-PROFILE-EMERSON-001
 * - SPEC-2016-PROFILE-EMERSON-002
 * - profiles/standard-2016.md
 * - profiles/emerson-2016.md
 */
class BInterface2016EmersonProfileCompatibilityTest {

    @Test
    void emersonProfileMustStaySeparateFromStandardProfile() throws Exception {
        String standard = Files.readString(protocolPath("profiles/standard-2016.md"));
        String emerson = Files.readString(protocolPath("profiles/emerson-2016.md"));

        assertFalse(standard.contains("厂商实测"),
                "standard-2016 profile must not contain vendor-measured rules");
        assertTrue(emerson.contains("厂商实测"),
                "emerson-2016 profile must explicitly classify vendor behavior");
        assertTrue(emerson.contains("SPEC-2016-PROFILE-EMERSON-001"));
        assertTrue(emerson.contains("SPEC-2016-PROFILE-EMERSON-002"));
    }

    @Test
    void emersonOnlyUppercaseFsuCodeSingleTagFixtureMustNotBeInStandardFixtures() throws Exception {
        Path standardFixtureDir = Path.of("src/test/resources/fixtures/b_interface_2016_standard");
        assertTrue(Files.exists(standardFixtureDir));

        try (var files = Files.walk(standardFixtureDir)) {
            var violations = files
                    .filter(path -> path.toString().endsWith(".xml"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains("<FSUCode>");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();
            assertTrue(violations.isEmpty(),
                    "Emerson-only <FSUCode> single-tag fixture must stay out of standard fixtures: "
                            + violations);
        }
    }

    @Test
    void emersonGetDataEmptyDeviceListFixtureShouldBeProfileOnly() {
        String emerson = readResource(
                "fixtures/b_interface_2016_emerson/emerson_get_data_empty_device_list.response.xml");
        assertTrue(emerson.contains("<FSUCode>"));
        assertTrue(emerson.contains("<Result>1</Result>"));
        assertTrue(emerson.toUpperCase(Locale.ROOT).contains("DEVICELIST"));
    }

    @Test
    void future2024CompatibilityProfileMustWarnAgainstUsing2024On2016Mainline() throws Exception {
        String future = Files.readString(protocolPath("profiles/future-2024-compatibility.md"));
        assertTrue(future.contains("2024"));
        assertTrue(future.contains("2016"));
        assertTrue(future.contains("GET_DATA"));
        assertTrue(future.contains("SEND_ALARM"));
    }

    private Path protocolPath(String relative) {
        Path fromBackend = Path.of("../openspec/protocols/binterface-2016").resolve(relative);
        if (Files.exists(fromBackend)) {
            return fromBackend;
        }
        return Path.of("openspec/protocols/binterface-2016").resolve(relative);
    }
}
