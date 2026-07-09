package com.urlshortener.analytics.service;

import com.urlshortener.analytics.entity.ClickEvent;
import com.urlshortener.analytics.repository.ClickEventAnalyticsRepository;
import com.urlshortener.analytics.repository.ClickEventRepository;
import com.urlshortener.common.dto.ClickEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClickEventConsumer {

    private final ClickEventRepository clickEventRepository;
    private final ClickEventAnalyticsRepository clickEventAnalyticsRepository;
    private final GeoIpService geoIpService;

    public void process(ClickEventMessage message) {
        GeoIpService.GeoLocation geo = geoIpService.lookup(message.getIpAddress());

        ClickEvent event = new ClickEvent();
        event.setUrlId(message.getUrlId());
        event.setClickedAt(message.getTimestamp());
        event.setIpAddress(message.getIpAddress());
        event.setUserAgent(message.getUserAgent());
        event.setCountryCode(geo.countryCode());
        event.setCity(geo.city());
        event.setReferrer(message.getReferrer());

        clickEventRepository.save(event);
        clickEventAnalyticsRepository.incrementClickCount(message.getUrlId());
    }
}
