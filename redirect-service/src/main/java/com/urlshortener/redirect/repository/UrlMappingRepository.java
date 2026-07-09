package com.urlshortener.redirect.repository;

import com.urlshortener.redirect.entity.UrlMapping;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface UrlMappingRepository extends MongoRepository<UrlMapping, UUID> {
    Optional<UrlMapping> findByShortCodeAndDeletedFalse(String shortCode);
}
