package com.urlshortener.analytics.dto;

public record RecentClick(
        String ipAddress,
        String countryCode,
        String city,
        String userAgent,
        String clickedAt
) {}
