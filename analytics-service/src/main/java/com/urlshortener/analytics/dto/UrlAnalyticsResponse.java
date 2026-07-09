package com.urlshortener.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public record UrlAnalyticsResponse(
        long totalClicks,
        List<ClickByDay> clicksByDay,
        List<GeoCount> topCountries,
        List<RecentClick> recentClicks
) {}
