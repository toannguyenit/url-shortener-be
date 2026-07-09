package com.urlshortener.analytics.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.InetAddress;
import java.util.Optional;

@Service
public class GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);

    @Value("${geoip.database-path:/data/GeoLite2-City.mmdb}")
    private String databasePath;

    private DatabaseReader reader;

    @PostConstruct
    public void init() {
        try {
            File dbFile = new File(databasePath);
            if (dbFile.exists()) {
                reader = new DatabaseReader.Builder(dbFile).build();
                log.info("GeoIP database loaded from {}", databasePath);
            } else {
                log.warn("GeoIP database not found at {}. Geo lookup disabled.", databasePath);
            }
        } catch (Exception e) {
            log.warn("Failed to load GeoIP database: {}", e.getMessage());
        }
    }

    public GeoLocation lookup(String ip) {
        if (reader == null || ip == null || ip.isBlank() || isPrivateIp(ip)) {
            return new GeoLocation(null, null);
        }
        try {
            Optional<CityResponse> response = reader.tryCity(InetAddress.getByName(ip));
            if (response.isPresent()) {
                CityResponse city = response.get();
                String country = city.getCountry() != null ? city.getCountry().getIsoCode() : null;
                String cityName = city.getCity() != null ? city.getCity().getName() : null;
                return new GeoLocation(country, cityName);
            }
        } catch (Exception e) {
            log.debug("GeoIP lookup failed for {}: {}", ip, e.getMessage());
        }
        return new GeoLocation(null, null);
    }

    private boolean isPrivateIp(String ip) {
        return ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")
                || ip.startsWith("172.") || ip.equals("0:0:0:0:0:0:0:1") || ip.equals("::1");
    }

    public record GeoLocation(String countryCode, String city) {}
}
