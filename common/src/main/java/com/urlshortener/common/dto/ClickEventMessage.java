package com.urlshortener.common.dto;

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
public class ClickEventMessage {
    private UUID urlId;
    private String shortCode;
    private String ipAddress;
    private String userAgent;
    private String referrer;
    private Instant timestamp;
}
