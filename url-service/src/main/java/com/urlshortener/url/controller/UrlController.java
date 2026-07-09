package com.urlshortener.url.controller;

import com.urlshortener.url.dto.CreateUrlRequest;
import com.urlshortener.url.dto.UpdateUrlRequest;
import com.urlshortener.url.dto.UrlResponse;
import com.urlshortener.url.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
@Tag(name = "URLs", description = "URL management endpoints")
public class UrlController {

    private final UrlService urlService;

    @PostMapping
    @Operation(summary = "Create a short URL")
    public ResponseEntity<UrlResponse> create(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody CreateUrlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(urlService.createUrl(userId, request));
    }

    @GetMapping
    @Operation(summary = "List user's URLs")
    public ResponseEntity<Page<UrlResponse>> list(
            @RequestHeader("X-User-Id") UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(urlService.getUserUrls(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get URL details")
    public ResponseEntity<UrlResponse> get(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(urlService.getUrl(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update URL")
    public ResponseEntity<UrlResponse> update(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id,
            @RequestBody UpdateUrlRequest request) {
        return ResponseEntity.ok(urlService.updateUrl(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete URL")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        urlService.deleteUrl(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/qr")
    @Operation(summary = "Generate QR code for URL")
    public ResponseEntity<byte[]> qrCode(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        byte[] qr = urlService.generateQrCode(userId, id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=qr.png")
                .contentType(MediaType.IMAGE_PNG)
                .body(qr);
    }
}
