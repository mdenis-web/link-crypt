package com.linkcrypt.core.port;

import com.linkcrypt.core.domain.ShortLink;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {
    void save(ShortLink link);

    Optional<ShortLink> findByCode(String code);

    void delete(String code);

    List<ShortLink> findAll();

    List<ShortLink> findByOwner(UUID ownerId);

    boolean existsByCode(String code);
}
