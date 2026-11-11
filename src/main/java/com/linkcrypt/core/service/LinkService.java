package com.linkcrypt.core.service;

import com.linkcrypt.config.AppConfig;
import com.linkcrypt.core.domain.ShortLink;
import com.linkcrypt.core.exception.*;
import com.linkcrypt.core.port.*;
import com.linkcrypt.core.exception.*;
import com.linkcrypt.core.port.*;
import com.linkcrypt.util.UrlValidator;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class LinkService {
    private final LinkRepository linkRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Clock clock;
    private final AppConfig config;
    private final ShortCodeGenerator codeGenerator;
    private final AppLogger logger;

    public LinkService(LinkRepository linkRepository,
                       UserRepository userRepository,
                       NotificationService notificationService,
                       Clock clock,
                       AppConfig config,
                       ShortCodeGenerator codeGenerator,
                       AppLogger logger) {
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.clock = clock;
        this.config = config;
        this.codeGenerator = codeGenerator;
        this.logger = logger;
    }

    public ShortLink create(UUID userId, String url, Integer maxClicksOverride) {
        logger.info("create: user=" + userId + " url=" + url + " limit=" + maxClicksOverride);

        try {
            UrlValidator.validate(url);
        } catch (ValidationException exception) {
            logger.warn("create rejected (invalid url) user=" + userId + ": " + exception.getMessage());
            throw exception;
        }

        if (!userRepository.exists(userId)) {
            logger.warn("create rejected (unknown user) user=" + userId);
            throw new UnauthorizedException("Unknown user: " + userId);
        }

        int maxClicks = maxClicksOverride != null ? maxClicksOverride : config.getDefaultMaxClicks();
        if (maxClicks <= 0) {
            logger.warn("create rejected (maxClicks<=0) user=" + userId + " value=" + maxClicks);
            throw new ValidationException("maxClicks must be > 0");
        }

        if (maxClicks > config.getMaxAllowedClicks()) {
            logger.warn("create rejected (maxClicks too large) user=" + userId + " value=" + maxClicks);
            throw new ValidationException(
                "maxClicks exceeds allowed limit " + config.getMaxAllowedClicks());
        }

        String code = generateUniqueCode();
        Instant now = clock.now();
        ShortLink link = new ShortLink(code, userId, url, now, now.plus(config.getDefaultTtl()), maxClicks);
        linkRepository.save(link);

        logger.info("link created: code=" + code + " owner=" + userId
            + " limit=" + maxClicks + " ttl=" + config.getDefaultTtl()
            + " expiresAt=" + link.getExpiresAt());
        return link;
    }

    public String open(String code, UUID userId) {
        logger.info("open: code=" + code + " caller=" + userId);

        ShortLink link = linkRepository.findByCode(code)
            .orElseThrow(() -> {
                logger.warn("open failed (not found) code=" + code + " caller=" + userId);
                return new LinkNotFoundException(code);
            });

        Instant now = clock.now();
        if (link.isExpired(now)) {
            logger.warn("open failed (expired) code=" + code + " owner=" + link.getOwnerId());
            notificationService.notify(link.getOwnerId(),
                "Link '" + code + "' has expired. Please create a new link.");
            throw new LinkExpiredException(code);
        }
        if (link.isLimitReached()) {
            logger.warn("open failed (limit) code=" + code + " owner=" + link.getOwnerId()
                + " clicks=" + link.getClickCount() + "/" + link.getMaxClicks());
            notificationService.notify(link.getOwnerId(),
                "Link '" + code + "' reached its click limit. Please create a new link.");
            throw new LimitExceededException(code);
        }

        link.incrementClicks();
        linkRepository.save(link);
        logger.info("open ok: code=" + code + " clicks="
            + link.getClickCount() + "/" + link.getMaxClicks());

        if (link.isLimitReached()) {
            logger.info("link reached limit: code=" + code + " owner=" + link.getOwnerId());
            notificationService.notify(link.getOwnerId(),
                "Link '" + code + "' just reached its click limit ("
                    + link.getMaxClicks() + "). Please create a new link.");
        }
        return link.getOriginalUrl();
    }

    public void updateMaxClicks(String code, UUID userId, int newMax) {
        logger.info("update-limit: code=" + code + " caller=" + userId + " newLimit=" + newMax);
        ShortLink link = requireOwned(code, userId);
        try {
            link.setMaxClicks(newMax, config.getMaxAllowedClicks());
        } catch (ValidationException exception) {
            logger.warn("update-limit rejected code=" + code + ": " + exception.getMessage());
            throw exception;
        }
        linkRepository.save(link);
        logger.info("update-limit ok: code=" + code + " newLimit=" + newMax);
    }

    public void delete(String code, UUID userId) {
        logger.info("delete: code=" + code + " caller=" + userId);
        requireOwned(code, userId);
        linkRepository.delete(code);
        logger.info("delete ok: code=" + code);
    }

    public List<ShortLink> listByUser(UUID userId) {
        List<ShortLink> links = linkRepository.findByOwner(userId);
        logger.info("list: user=" + userId + " count=" + links.size());
        return links;
    }

    public ShortLink getByCode(String code) {
        return linkRepository.findByCode(code)
            .orElseThrow(() -> new LinkNotFoundException(code));
    }

    public ShortLink getByUrl(UUID userId, String url) {
        return linkRepository.findByOwner(userId).stream()
            .filter(link -> link.getOriginalUrl().equals(url))
            .findFirst()
            .orElse(null);
    }

    private ShortLink requireOwned(String code, UUID userId) {
        ShortLink link = linkRepository.findByCode(code)
            .orElseThrow(() -> {
                logger.warn("owner check failed (not found) code=" + code + " caller=" + userId);
                return new LinkNotFoundException(code);
            });
        if (!link.getOwnerId().equals(userId)) {
            logger.warn("owner check failed (not owner) code=" + code
                + " caller=" + userId + " owner=" + link.getOwnerId());
            throw new UnauthorizedException("You are not the owner of link " + code);
        }
        return link;
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 10; i++) {
            String code = codeGenerator.generate();
            if (!linkRepository.existsByCode(code)) return code;
            logger.warn("code collision, retrying: " + code);
        }
        logger.error("failed to generate unique code after 10 attempts");
        throw new IllegalStateException("Failed to generate unique code after 10 attempts");
    }
}
