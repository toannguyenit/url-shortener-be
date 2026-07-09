package com.urlshortener.analytics.dto;

import java.time.LocalDate;

public record ClickByDay(LocalDate date, long count) {}
