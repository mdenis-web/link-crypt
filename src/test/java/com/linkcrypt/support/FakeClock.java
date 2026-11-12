package com.linkcrypt.support;

import com.linkcrypt.core.port.Clock;

import java.time.Instant;

public class FakeClock implements Clock {
    private Instant now;

    public FakeClock(Instant start) {
        this.now = start;
    }

    @Override
    public Instant now() {
        return now;
    }

    public void advanceSeconds(long sec) {
        now = now.plusSeconds(sec);
    }

    public void set(Instant instant) {
        this.now = instant;
    }
}
