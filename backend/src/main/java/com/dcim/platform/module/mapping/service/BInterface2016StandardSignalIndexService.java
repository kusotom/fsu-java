package com.dcim.platform.module.mapping.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class BInterface2016StandardSignalIndexService {

    public static final String SOURCE = "BINTERFACE_2016_STANDARD";
    private static final String RESOURCE =
            "classpath:dictionary/binterface2016/standard-signal-index.csv";

    private final ResourceLoader resourceLoader;
    private volatile Map<String, StandardSignal> cache;

    public BInterface2016StandardSignalIndexService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Optional<StandardSignal> findBySignalId(String signalId) {
        if (signalId == null || signalId.trim().isEmpty()) return Optional.empty();
        return Optional.ofNullable(load().get(signalId.trim()));
    }

    private Map<String, StandardSignal> load() {
        Map<String, StandardSignal> local = cache;
        if (local != null) return local;
        synchronized (this) {
            if (cache == null) cache = readResource();
            return cache;
        }
    }

    private Map<String, StandardSignal> readResource() {
        Resource resource = resourceLoader.getResource(RESOURCE);
        Map<String, StandardSignal> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split(",", -1);
                if (cols.length < 8) {
                    throw new IllegalStateException("Invalid BInterface2016 signal index row: " + line);
                }
                StandardSignal signal = new StandardSignal(
                        clean(cols[0]), clean(cols[1]), clean(cols[2]), clean(cols[3]),
                        clean(cols[4]), clean(cols[5]), clean(cols[6]), clean(cols[7]));
                if (!signal.signalId().isEmpty()) result.put(signal.signalId(), signal);
            }
            return Map.copyOf(result);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read BInterface2016 signal index", e);
        }
    }

    private static String clean(String value) {
        if (value == null) return "";
        return value.replace("\uFEFF", "").trim();
    }

    public record StandardSignal(
            String signalId,
            String signalName,
            String signalCategory,
            String signalType,
            String unit,
            String valueMeaningsRaw,
            String sourceRef,
            String note
    ) {}
}
