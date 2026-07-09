package com.urlshortener.url.service;

import com.urlshortener.common.util.Base62;
import com.urlshortener.url.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class ShortCodeGenerator {

    private static final int CODE_LENGTH = 7;
    private static final int MAX_ATTEMPTS = 10;

    private final UrlRepository urlRepository;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = Base62.random(CODE_LENGTH, random);
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique short code");
    }
}
