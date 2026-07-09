package com.urlshortener.analytics.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "urls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlRef {

    @Id
    private UUID id;

    @Indexed
    private UUID userId;

    private String shortCode;

    private long clickCount;

    private boolean deleted;
}
