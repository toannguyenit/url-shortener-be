package com.urlshortener.url.repository;

import com.urlshortener.url.entity.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface UrlRepository extends MongoRepository<Url, UUID> {

    Page<Url> findByUserIdAndDeletedFalse(UUID userId, Pageable pageable);

    Optional<Url> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    Optional<Url> findByShortCodeAndDeletedFalse(String shortCode);

    boolean existsByShortCode(String shortCode);

    long countByUserIdAndDeletedFalse(UUID userId);
}
