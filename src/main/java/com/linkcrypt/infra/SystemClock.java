package com.linkcrypt.infra;

import com.linkcrypt.core.port.Clock;

import java.time.Instant;

public class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
