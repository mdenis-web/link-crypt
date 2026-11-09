package com.linkcrypt.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static AppConfig load(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IOException("Config file not found: " + path.toAbsolutePath());
        }

        Properties props = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(r);
        }
        return fromProperties(props);
    }

    public static AppConfig fromProperties(Properties props) {
        long ttlSec = Long.parseLong(props.getProperty("link.default.ttl.seconds", "86400"));
        int defaultClicks = Integer.parseInt(props.getProperty("link.max.clicks.default", "100"));
        int maxClicks = Integer.parseInt(props.getProperty("link.max.clicks.limit", "1000000"));
        long cleanupSec = Long.parseLong(props.getProperty("link.cleanup.interval.seconds", "60"));
        String baseUrl = props.getProperty("link.base.url", "http://short.ly/");
        int codeLength = Integer.parseInt(props.getProperty("link.code.length", "7"));
        String logPath = props.getProperty("logging.file.path", "logs/app.log");

        return new AppConfig(
                Duration.ofSeconds(ttlSec), defaultClicks, maxClicks,
                Duration.ofSeconds(cleanupSec), baseUrl, codeLength, logPath);
    }
}
