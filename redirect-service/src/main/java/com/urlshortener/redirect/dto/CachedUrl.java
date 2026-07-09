package com.urlshortener.redirect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CachedUrl {
    private UUID id;
    private String longUrl;
    private boolean active;
    private Instant expiresAt;
}
