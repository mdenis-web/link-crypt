package com.linkcrypt.config;

import java.time.Duration;

public final class AppConfig {
    private final Duration defaultTtl;
    private final int defaultMaxClicks;
    private final int maxAllowedClicks;
    private final Duration cleanupInterval;
    private final String baseUrl;
    private final int codeLength;
    private final String logFilePath;

    public AppConfig(Duration defaultTtl, int defaultMaxClicks, int maxAllowedClicks,
                     Duration cleanupInterval, String baseUrl, int codeLength,
                     String logFilePath) {
        if (defaultTtl.isNegative() || defaultTtl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }

        if (defaultMaxClicks <= 0 || maxAllowedClicks < defaultMaxClicks) {
            throw new IllegalArgumentException("invalid max clicks");
        }

        if (cleanupInterval.isNegative() || cleanupInterval.isZero()) {
            throw new IllegalArgumentException("cleanup interval must be positive");
        }

        if (codeLength < 4) {
            throw new IllegalArgumentException("code length must be >= 4");
        }

        if (logFilePath == null || logFilePath.isBlank()) {
            throw new IllegalArgumentException("log file path must not be empty");
        }

        this.defaultTtl = defaultTtl;
        this.defaultMaxClicks = defaultMaxClicks;
        this.maxAllowedClicks = maxAllowedClicks;
        this.cleanupInterval = cleanupInterval;
        this.baseUrl = baseUrl;
        this.codeLength = codeLength;
        this.logFilePath = logFilePath;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public int getDefaultMaxClicks() {
        return defaultMaxClicks;
    }

    public int getMaxAllowedClicks() {
        return maxAllowedClicks;
    }

    public Duration getCleanupInterval() {
        return cleanupInterval;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getCodeLength() {
        return codeLength;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public String extractCode(String input) {
        if (input.startsWith(baseUrl)) {
            return input.substring(baseUrl.length());
        }

        return input;
    }
}
