package com.urlshortener.analytics.controller;

import com.urlshortener.analytics.dto.DashboardResponse;
import com.urlshortener.analytics.dto.GeoCount;
import com.urlshortener.analytics.dto.UrlAnalyticsResponse;
import com.urlshortener.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Analytics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/urls/{id}")
    @Operation(summary = "Get URL analytics")
    public ResponseEntity<UrlAnalyticsResponse> getUrlAnalytics(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null) to = Instant.now();
        return ResponseEntity.ok(analyticsService.getUrlAnalytics(userId, id, from, to));
    }

    @GetMapping("/urls/{id}/geo")
    @Operation(summary = "Get geo analytics for URL")
    public ResponseEntity<List<GeoCount>> getGeoAnalytics(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(analyticsService.getGeoAnalytics(userId, id));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary")
    public ResponseEntity<DashboardResponse> getDashboard(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(analyticsService.getDashboard(userId));
    }
}
