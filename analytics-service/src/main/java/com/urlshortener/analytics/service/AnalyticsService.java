package com.urlshortener.analytics.service;

import com.urlshortener.analytics.dto.*;
import com.urlshortener.analytics.entity.ClickEvent;
import com.urlshortener.analytics.repository.ClickEventAnalyticsRepository;
import com.urlshortener.analytics.repository.ClickEventRepository;
import com.urlshortener.analytics.repository.UrlRefRepository;
import com.urlshortener.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClickEventRepository clickEventRepository;
    private final ClickEventAnalyticsRepository clickEventAnalyticsRepository;
    private final UrlRefRepository urlRefRepository;

    public UrlAnalyticsResponse getUrlAnalytics(UUID userId, UUID urlId, Instant from, Instant to) {
        urlRefRepository.findByIdAndUserIdAndDeletedFalse(urlId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));

        long totalClicks = clickEventRepository.countByUrlId(urlId);

        List<ClickByDay> clicksByDay = clickEventAnalyticsRepository.countClicksByDay(urlId, from, to).stream()
                .filter(row -> row.getId() != null && !row.getId().isBlank())
                .map(row -> new ClickByDay(LocalDate.parse(row.getId()), row.getCount()))
                .toList();

        List<GeoCount> topCountries = clickEventAnalyticsRepository.countByCountry(urlId).stream()
                .limit(10)
                .map(row -> new GeoCount(row.getId(), row.getCount()))
                .toList();

        List<RecentClick> recentClicks = clickEventRepository
                .findByUrlIdOrderByClickedAtDesc(urlId, PageRequest.of(0, 20)).stream()
                .map(this::toRecentClick)
                .toList();

        return new UrlAnalyticsResponse(totalClicks, clicksByDay, topCountries, recentClicks);
    }

    public List<GeoCount> getGeoAnalytics(UUID userId, UUID urlId) {
        urlRefRepository.findByIdAndUserIdAndDeletedFalse(urlId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));

        return clickEventAnalyticsRepository.countByCity(urlId).stream()
                .limit(20)
                .filter(row -> row.getId() != null)
                .map(row -> new GeoCount(
                        row.getId().getCountryCode(),
                        row.getId().getCity(),
                        row.getCount()))
                .toList();
    }

    public DashboardResponse getDashboard(UUID userId) {
        long totalLinks = urlRefRepository.countByUserIdAndDeletedFalse(userId);
        Instant from = Instant.now().minus(7, ChronoUnit.DAYS);

        List<TopLink> topLinks = urlRefRepository
                .findByUserIdAndDeletedFalseOrderByClickCountDesc(userId, PageRequest.of(0, 5))
                .stream()
                .map(url -> new TopLink(
                        url.getId().toString(),
                        url.getShortCode(),
                        url.getClickCount()))
                .toList();

        long totalClicks = clickEventAnalyticsRepository.sumClickCountByUserId(userId);

        List<ClickByDay> clicksLast7Days = clickEventAnalyticsRepository.dashboardClicksByDay(userId, from).stream()
                .filter(row -> row.getId() != null && !row.getId().isBlank())
                .map(row -> new ClickByDay(LocalDate.parse(row.getId()), row.getCount()))
                .toList();

        return new DashboardResponse(totalLinks, totalClicks, topLinks, clicksLast7Days);
    }

    private RecentClick toRecentClick(ClickEvent event) {
        return new RecentClick(
                event.getIpAddress(),
                event.getCountryCode(),
                event.getCity(),
                event.getUserAgent(),
                event.getClickedAt().toString()
        );
    }
}
