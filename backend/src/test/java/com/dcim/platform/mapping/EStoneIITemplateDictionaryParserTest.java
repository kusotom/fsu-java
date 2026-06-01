package com.dcim.platform.mapping;

import com.dcim.platform.module.mapping.service.EStoneIITemplateDictionaryParser;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class EStoneIITemplateDictionaryParserTest {

    private final EStoneIITemplateDictionaryParser parser =
            new EStoneIITemplateDictionaryParser(new DefaultResourceLoader());

    @Test
    void parsesEStoneIITemplateDictionaryCountsAndVariants() {
        EStoneIITemplateDictionaryParser.TemplateDictionaryData data = parser.parseAll();

        assertEquals(39, data.signals().size(), "WITH_MIDPOINT D 类假设全集应包含 39 个 Signal");
        assertEquals(13, data.events().size(), "WITH_MIDPOINT D 类假设全集应包含 13 个 Event");
        assertEquals(4, data.controls().size(), "Controls 仅作为禁用参考入库");
        assertEquals(23, data.candidates().size(), "现场候选映射应为 23 条");
        assertEquals(4, data.candidates().stream().filter(c -> "HIGH".equals(c.confidence())).count());
        assertEquals(6, data.candidates().stream().filter(c -> "MEDIUM".equals(c.confidence())).count());
        assertEquals(13, data.candidates().stream().filter(c -> "PENDING_REAL_DATA".equals(c.confidence())).count());

        long noMidSignals = data.signals().stream()
                .filter(s -> Set.of("BOTH", "NO_MIDPOINT").contains(s.templateVariant()))
                .count();
        long noMidEvents = data.events().stream()
                .filter(e -> Set.of("BOTH", "NO_MIDPOINT").contains(e.templateVariant()))
                .count();
        assertEquals(37, noMidSignals, "NO_MIDPOINT 应包含 37 个 Signal");
        assertEquals(11, noMidEvents, "NO_MIDPOINT 应包含 11 个 Event");

        Map<String, String> signalNames = data.signals().stream()
                .collect(Collectors.toMap(EStoneIITemplateDictionaryParser.SignalDefinition::signalId,
                        EStoneIITemplateDictionaryParser.SignalDefinition::signalName));
        assertEquals("I2C温度", signalNames.get("510000211"));
        assertEquals("烟感", signalNames.get("510000250"));
        assertFalse(signalNames.containsKey("51160001"), "当前 P0 范围不得引入 StoneIII / 511600xx");
    }

    @Test
    void parsesPerSignalMeaningsWithoutGlobalDiAssumption() {
        EStoneIITemplateDictionaryParser.TemplateDictionaryData data = parser.parseAll();
        EStoneIITemplateDictionaryParser.SignalDefinition smoke = data.signals().stream()
                .filter(s -> "510000250".equals(s.signalId()))
                .findFirst()
                .orElseThrow();
        EStoneIITemplateDictionaryParser.SignalDefinition acFault = data.signals().stream()
                .filter(s -> "510000260".equals(s.signalId()))
                .findFirst()
                .orElseThrow();

        assertEquals("无告警", parser.meaningOf(smoke.signalMeaningsRaw(), "0"));
        assertEquals("有告警", parser.meaningOf(smoke.signalMeaningsRaw(), "1"));
        assertEquals("有告警", parser.meaningOf(acFault.signalMeaningsRaw(), "0"));
        assertEquals("无告警", parser.meaningOf(acFault.signalMeaningsRaw(), "1"));
    }
}
