package com.urlshortener.url.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.urlshortener.common.exception.BadRequestException;
import com.urlshortener.common.exception.ConflictException;
import com.urlshortener.common.exception.ResourceNotFoundException;
import com.urlshortener.url.dto.CreateUrlRequest;
import com.urlshortener.url.dto.UpdateUrlRequest;
import com.urlshortener.url.dto.UrlResponse;
import com.urlshortener.url.entity.AliasType;
import com.urlshortener.url.entity.Url;
import com.urlshortener.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final Pattern CUSTOM_ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final MongoTemplate mongoTemplate;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.short-url-base:http://localhost:8083}")
    private String shortUrlBase;

    public UrlResponse createUrl(UUID userId, CreateUrlRequest request) {
        String shortCode;
        AliasType aliasType;

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            shortCode = request.getCustomAlias().trim();
            if (!CUSTOM_ALIAS_PATTERN.matcher(shortCode).matches()) {
                throw new BadRequestException("Custom alias must be 3-20 characters: letters, numbers, underscore, hyphen");
            }
            if (urlRepository.existsByShortCode(shortCode)) {
                throw new ConflictException("Custom alias already taken");
            }
            aliasType = AliasType.CUSTOM;
        } else {
            shortCode = shortCodeGenerator.generate();
            aliasType = AliasType.AUTO;
        }

        Url url = Url.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .longUrl(request.getLongUrl())
                .shortCode(shortCode)
                .aliasType(aliasType)
                .expiresAt(request.getExpiresAt())
                .build();

        url = urlRepository.save(url);
        return toResponse(url);
    }

    public Page<UrlResponse> getUserUrls(UUID userId, Pageable pageable) {
        return urlRepository.findByUserIdAndDeletedFalse(userId, pageable).map(this::toResponse);
    }

    public UrlResponse getUrl(UUID userId, UUID id) {
        Url url = urlRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));
        return toResponse(url);
    }

    public UrlResponse updateUrl(UUID userId, UUID id, UpdateUrlRequest request) {
        Url url = urlRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));

        if (request.getActive() != null) {
            url.setActive(request.getActive());
        }
        if (request.getExpiresAt() != null) {
            url.setExpiresAt(request.getExpiresAt());
        }

        url = urlRepository.save(url);
        invalidateCache(url.getShortCode());
        return toResponse(url);
    }

    public void deleteUrl(UUID userId, UUID id) {
        Url url = urlRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));
        url.setDeleted(true);
        url.setActive(false);
        urlRepository.save(url);
        invalidateCache(url.getShortCode());
    }

    public byte[] generateQrCode(UUID userId, UUID id) {
        Url url = urlRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("URL not found"));
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    shortUrlBase + "/" + url.getShortCode(),
                    BarcodeFormat.QR_CODE,
                    300,
                    300
            );
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate QR code");
        }
    }

    @Scheduled(fixedRate = 3600000)
    public void deactivateExpiredUrls() {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("expiresAt").lt(Instant.now()).and("active").is(true)),
                new Update().set("active", false),
                Url.class
        );
    }

    private void invalidateCache(String shortCode) {
        redisTemplate.delete("url:" + shortCode);
    }

    private UrlResponse toResponse(Url url) {
        return UrlResponse.builder()
                .id(url.getId())
                .longUrl(url.getLongUrl())
                .shortCode(url.getShortCode())
                .shortUrl(shortUrlBase + "/" + url.getShortCode())
                .aliasType(url.getAliasType())
                .active(url.isActive())
                .expiresAt(url.getExpiresAt())
                .clickCount(url.getClickCount())
                .createdAt(url.getCreatedAt())
                .updatedAt(url.getUpdatedAt())
                .build();
    }
}
