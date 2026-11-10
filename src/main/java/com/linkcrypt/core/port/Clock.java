package com.linkcrypt.core.port;

import java.time.Instant;

public interface Clock {
    Instant now();
}
