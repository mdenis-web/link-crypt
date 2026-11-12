package com.linkcrypt.core.service;

import com.linkcrypt.config.AppConfig;
import com.linkcrypt.core.domain.ShortLink;
import com.linkcrypt.core.exception.*;
import com.linkcrypt.core.exception.*;
import com.linkcrypt.core.port.LinkRepository;
import com.linkcrypt.core.port.UserRepository;
import com.linkcrypt.infra.*;
import com.linkcrypt.support.*;
import com.linkcrypt.infra.InMemoryLinkRepository;
import com.linkcrypt.infra.InMemoryUserRepository;
import com.linkcrypt.support.CollectingLogger;
import com.linkcrypt.support.CollectingNotificationService;
import com.linkcrypt.support.FakeClock;
import org.junit.jupiter.api.*;

import java.time.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LinkServiceTest {
    private FakeClock clock;
    private UserService userService;
    private LinkService linkService;
    private CollectingNotificationService notifier;
    private CollectingLogger logger;
    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        clock = new FakeClock(Instant.parse("2025-01-01T00:00:00Z"));
        UserRepository userRepo = new InMemoryUserRepository();
        LinkRepository linkRepo = new InMemoryLinkRepository();
        notifier = new CollectingNotificationService();
        logger = new CollectingLogger();
        AppConfig config = new AppConfig(
            Duration.ofHours(24), 5, 100, Duration.ofSeconds(60),
            "http://s/", 7, "logs/test.log");
        userService = new UserService(userRepo, logger);
        linkService = new LinkService(
            linkRepo, userRepo, notifier, clock, config,
            new ShortCodeGenerator(7), logger);
        userA = userService.register().getId();
        userB = userService.register().getId();
    }

    @Test
    void sameUserSameUrlGetsSameLink() {
        ShortLink link = linkService.create(userA, "https://example.com", 10);
        ShortLink found = linkService.getByUrl(userA, "https://example.com");

        assertNotNull(found);
        assertEquals(link.getCode(), found.getCode());
        assertEquals(link.getOriginalUrl(), found.getOriginalUrl());
    }

    @Test
    void differentUsersGetDifferentCodesForSameUrl() {
        ShortLink linkA = linkService.create(userA, "https://example.com", 10);
        ShortLink linkB = linkService.create(userB, "https://example.com", 10);

        assertNotEquals(linkA.getCode(), linkB.getCode());
        assertNotEquals(linkA.getOwnerId(), linkB.getOwnerId());

        ShortLink foundA = linkService.getByUrl(userA, "https://example.com");
        ShortLink foundB = linkService.getByUrl(userB, "https://example.com");

        assertEquals(linkA.getCode(), foundA.getCode());
        assertEquals(linkB.getCode(), foundB.getCode());
    }

    @Test
    void getByUrlReturnsNullIfNotFound() {
        assertNull(linkService.getByUrl(userA, "https://not-exists.com"));
    }

    @Test
    void createUsesDefaultLimitWhenNotProvided() {
        ShortLink link = linkService.create(userA, "https://example.com", null);
        assertEquals(5, link.getMaxClicks());
    }

    @Test
    void openIncrementsClickCounter() {
        ShortLink link = linkService.create(userA, "https://example.com", 3);
        String url = linkService.open(link.getCode(), userB);
        assertEquals("https://example.com", url);
        assertEquals(1, linkService.getByCode(link.getCode()).getClickCount());
    }

    @Test
    void openAtLimitThrowsAndNotifiesOwner() {
        ShortLink link = linkService.create(userA, "https://example.com", 1);
        linkService.open(link.getCode(), userB);
        assertThrows(LimitExceededException.class, () -> linkService.open(link.getCode(), userB));
        assertFalse(notifier.forUser(userA).isEmpty());
    }

    @Test
    void openExpiredThrowsAndNotifiesOwner() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        clock.advanceSeconds(Duration.ofHours(25).toSeconds());
        assertThrows(LinkExpiredException.class, () -> linkService.open(link.getCode(), userB));
        assertFalse(notifier.forUser(userA).isEmpty());
    }

    @Test
    void editLimitRequiresOwner() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        assertThrows(UnauthorizedException.class,
            () -> linkService.updateMaxClicks(link.getCode(), userB, 10));
    }

    @Test
    void editLimitUpdatesValue() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        linkService.updateMaxClicks(link.getCode(), userA, 20);
        assertEquals(20, linkService.getByCode(link.getCode()).getMaxClicks());
    }

    @Test
    void editLimitRejectsAboveMax() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        assertThrows(ValidationException.class,
            () -> linkService.updateMaxClicks(link.getCode(), userA, 101));
    }

    @Test
    void deleteRequiresOwner() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        assertThrows(UnauthorizedException.class,
            () -> linkService.delete(link.getCode(), userB));
    }

    @Test
    void deleteRemovesLink() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        linkService.delete(link.getCode(), userA);
        assertThrows(LinkNotFoundException.class, () -> linkService.getByCode(link.getCode()));
    }

    @Test
    void listByUserOnlyOwnLinks() {
        linkService.create(userA, "https://a.com", 1);
        linkService.create(userB, "https://b.com", 1);
        assertEquals(1, linkService.listByUser(userA).size());
        assertEquals(1, linkService.listByUser(userB).size());
    }

    @Test
    void createIsLogged() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        assertTrue(logger.containsMessage("link created: code=" + link.getCode()));
    }

    @Test
    void unauthorizedDeleteIsLogged() {
        ShortLink link = linkService.create(userA, "https://example.com", 5);
        assertThrows(UnauthorizedException.class,
            () -> linkService.delete(link.getCode(), userB));
        assertTrue(logger.containsLevel("WARN"));
        assertTrue(logger.containsMessage("owner check failed"));
    }

    @Test
    void limitReachedIsLogged() {
        ShortLink link = linkService.create(userA, "https://example.com", 1);
        linkService.open(link.getCode(), userB);
        assertThrows(LimitExceededException.class, () -> linkService.open(link.getCode(), userB));
        assertTrue(logger.containsMessage("open failed (limit)"));
    }
}
