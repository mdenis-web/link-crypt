package com.linkcrypt.infra;

import com.linkcrypt.core.port.AppLogger;
import com.linkcrypt.core.port.NotificationService;

import java.io.PrintStream;
import java.util.UUID;

public class ConsoleNotificationService implements NotificationService {
    private final PrintStream out;
    private final AppLogger logger;

    public ConsoleNotificationService(PrintStream out, AppLogger logger) {
        this.out = out;
        this.logger = logger;
    }

    @Override
    public synchronized void notify(UUID userId, String message) {
        out.println();
        out.println("[NOTIFICATION -> " + userId + "] " + message);
        logger.info("notification to user=" + userId + ": " + message);
    }
}
