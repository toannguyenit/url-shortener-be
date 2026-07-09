package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.entity.UrlRef;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UrlRefRepository extends MongoRepository<UrlRef, UUID> {

    Optional<UrlRef> findByIdAndUserIdAndDeletedFalse(UUID id, UUID userId);

    long countByUserIdAndDeletedFalse(UUID userId);

    List<UrlRef> findByUserIdAndDeletedFalseOrderByClickCountDesc(UUID userId, Pageable pageable);
}
