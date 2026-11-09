package com.linkcrypt.core.domain;

import com.linkcrypt.core.exception.LimitExceededException;
import com.linkcrypt.core.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ShortLink {
    private final String code;
    private final UUID ownerId;
    private final String originalUrl;
    private final Instant createdAt;
    private final Instant expiresAt;
    private int clickCount;
    private int maxClicks;

    public ShortLink(String code, UUID ownerId, String originalUrl,
                     Instant createdAt, Instant expiresAt, int maxClicks) {
        if (maxClicks <= 0) {
            throw new ValidationException("maxClicks must be positive");
        }

        this.code = Objects.requireNonNull(code);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.originalUrl = Objects.requireNonNull(originalUrl);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.maxClicks = maxClicks;
        this.clickCount = 0;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isLimitReached() {
        return clickCount >= maxClicks;
    }

    public void incrementClicks() {
        if (isLimitReached()) {
            throw new LimitExceededException(code);
        }

        clickCount++;
    }

    public void setMaxClicks(int newMax, int allowedMax) {
        if (newMax <= 0) {
            throw new ValidationException("maxClicks must be > 0");
        }

        if (newMax > allowedMax) {
            throw new ValidationException("maxClicks exceeds allowed limit " + allowedMax);
        }

        if (newMax < clickCount) {
            throw new ValidationException("cannot set limit below current click count");
        }

        this.maxClicks = newMax;
    }

    public String getCode() {
        return code;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getMaxClicks() {
        return maxClicks;
    }
}
