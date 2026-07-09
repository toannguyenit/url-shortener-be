package com.urlshortener.analytics.dto;

public record GeoCount(String countryCode, String city, long count) {
    public GeoCount(String countryCode, long count) {
        this(countryCode, null, count);
    }
}
