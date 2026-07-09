package com.urlshortener.analytics.dto;

import java.util.List;

public record DashboardResponse(
        long totalLinks,
        long totalClicks,
        List<TopLink> topLinks,
        List<ClickByDay> clicksLast7Days
) {}
