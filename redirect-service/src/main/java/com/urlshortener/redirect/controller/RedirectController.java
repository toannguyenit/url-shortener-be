package com.urlshortener.redirect.controller;

import com.urlshortener.redirect.dto.CachedUrl;
import com.urlshortener.redirect.service.RedirectService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class RedirectController {

    private final RedirectService redirectService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        Optional<CachedUrl> urlOpt = redirectService.resolve(shortCode);
        if (urlOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        CachedUrl url = urlOpt.get();
        if (url.getLongUrl() == null || url.getLongUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.GONE).body("Invalid link target");
        }
        if (!url.isActive() || (url.getExpiresAt() != null && Instant.now().isAfter(url.getExpiresAt()))) {
            return ResponseEntity.status(HttpStatus.GONE).body("This link has expired or been deactivated");
        }

        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String referrer = request.getHeader("Referer");

        redirectService.publishClickEvent(url, shortCode, ip, userAgent, referrer);

        RedirectView redirectView = new RedirectView(url.getLongUrl());
        redirectView.setStatusCode(HttpStatus.FOUND);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", url.getLongUrl())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
