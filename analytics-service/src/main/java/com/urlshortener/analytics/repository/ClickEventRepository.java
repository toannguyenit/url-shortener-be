package com.urlshortener.analytics.repository;

import com.urlshortener.analytics.entity.ClickEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface ClickEventRepository extends MongoRepository<ClickEvent, String> {

    long countByUrlId(UUID urlId);

    List<ClickEvent> findByUrlIdOrderByClickedAtDesc(UUID urlId, Pageable pageable);
}
