package com.urlshortener.url.dto;

import com.urlshortener.url.entity.AliasType;
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
public class UrlResponse {
    private UUID id;
    private String longUrl;
    private String shortCode;
    private String shortUrl;
    private AliasType aliasType;
    private boolean active;
    private Instant expiresAt;
    private long clickCount;
    private Instant createdAt;
    private Instant updatedAt;
}
