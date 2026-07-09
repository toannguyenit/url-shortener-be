package com.urlshortener.analytics.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "click_events")
public class ClickEvent {

    @Id
    private String id;

    @Indexed
    private UUID urlId;

    @Indexed
    private Instant clickedAt;

    private String ipAddress;

    private String userAgent;

    private String countryCode;

    private String city;

    private String referrer;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public UUID getUrlId() { return urlId; }
    public void setUrlId(UUID urlId) { this.urlId = urlId; }
    public Instant getClickedAt() { return clickedAt; }
    public void setClickedAt(Instant clickedAt) { this.clickedAt = clickedAt; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
}
