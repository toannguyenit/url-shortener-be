package com.urlshortener.url.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateUrlRequest {

    @NotBlank(message = "Long URL is required")
    @Pattern(regexp = "https?://.+", message = "URL must start with http:// or https://")
    private String longUrl;

    /** Optional. Leave empty to auto-generate a random short code. */
    private String customAlias;

    private Instant expiresAt;
}
