package com.linkcrypt.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    void loadsAllProperties() {
        Properties properties = new Properties();
        properties.setProperty("link.default.ttl.seconds", "3600");
        properties.setProperty("link.max.clicks.default", "5");
        properties.setProperty("link.max.clicks.limit", "10");
        properties.setProperty("link.cleanup.interval.seconds", "30");
        properties.setProperty("link.base.url", "https://s.io/");
        properties.setProperty("link.code.length", "8");
        properties.setProperty("logging.file.path", "/tmp/app.log");

        AppConfig config = ConfigLoader.fromProperties(properties);
        assertEquals(Duration.ofHours(1), config.getDefaultTtl());
        assertEquals(5, config.getDefaultMaxClicks());
        assertEquals(10, config.getMaxAllowedClicks());
        assertEquals(Duration.ofSeconds(30), config.getCleanupInterval());
        assertEquals("https://s.io/", config.getBaseUrl());
        assertEquals(8, config.getCodeLength());
        assertEquals("/tmp/app.log", config.getLogFilePath());
    }

    @Test
    void usesDefaultsWhenMissing() {
        AppConfig config = ConfigLoader.fromProperties(new Properties());
        assertEquals(Duration.ofSeconds(86400), config.getDefaultTtl());
        assertEquals(100, config.getDefaultMaxClicks());
        assertEquals(1_000_000, config.getMaxAllowedClicks());
        assertEquals("logs/app.log", config.getLogFilePath());
    }
}
