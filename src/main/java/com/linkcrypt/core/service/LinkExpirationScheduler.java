package com.linkcrypt.core.service;

import com.linkcrypt.core.domain.ShortLink;
import com.linkcrypt.core.port.AppLogger;
import com.linkcrypt.core.port.Clock;
import com.linkcrypt.core.port.LinkRepository;
import com.linkcrypt.core.port.NotificationService;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;

public class LinkExpirationScheduler implements AutoCloseable {
    private final ScheduledExecutorService executor;
    private final LinkRepository linkRepository;
    private final NotificationService notificationService;
    private final Clock clock;
    private final Duration interval;
    private final AppLogger logger;

    public LinkExpirationScheduler(LinkRepository linkRepository,
                                   NotificationService notificationService,
                                   Clock clock,
                                   Duration interval,
                                   AppLogger logger) {
        this.linkRepository = linkRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.interval = interval;
        this.logger = logger;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "link-expiration-scheduler");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void start() {
        long ms = interval.toMillis();
        logger.info("scheduler started: interval=" + interval);
        executor.scheduleAtFixedRate(this::cleanupSafely, ms, ms, TimeUnit.MILLISECONDS);
    }

    private void cleanupSafely() {
        try {
            cleanup();
        } catch (Exception exception) {
            logger.error("scheduler cleanup failed", exception);
        }
    }

    void cleanup() {
        Instant now = clock.now();
        int removed = 0;
        for (ShortLink link : linkRepository.findAll()) {
            if (link.isExpired(now)) {
                linkRepository.delete(link.getCode());
                logger.info("expired link removed: code=" + link.getCode()
                    + " owner=" + link.getOwnerId());
                notificationService.notify(link.getOwnerId(),
                    "Link '" + link.getCode() + "' has expired and was removed. Please create a new link.");
                removed++;
            }
        }
        if (removed > 0) {
            logger.info("cleanup finished: removed=" + removed);
        }
    }

    @Override
    public void close() {
        logger.info("scheduler stopping");
        executor.shutdownNow();
    }
}
