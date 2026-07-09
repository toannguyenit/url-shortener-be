package com.urlshortener.url.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class UpdateUrlRequest {
    private Boolean active;
    private Instant expiresAt;
}
