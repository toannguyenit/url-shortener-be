package com.urlshortener.analytics.repository;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ClickEventAnalyticsRepository {

    private final MongoTemplate mongoTemplate;

    public List<DayCount> countClicksByDay(UUID urlId, Instant from, Instant to) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("urlId").is(urlId)
                        .and("clickedAt").gte(from).lte(to)),
                Aggregation.project()
                        .andExpression("dateToString('%Y-%m-%d', clickedAt)").as("day"),
                Aggregation.group("day").count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        return mongoTemplate.aggregate(agg, "click_events", DayCount.class).getMappedResults();
    }

    public List<CountryCount> countByCountry(UUID urlId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("urlId").is(urlId).and("countryCode").ne(null)),
                Aggregation.group("countryCode").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count")
        );
        return mongoTemplate.aggregate(agg, "click_events", CountryCount.class).getMappedResults();
    }

    public List<CityCountResult> countByCity(UUID urlId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("urlId").is(urlId).and("city").ne(null)),
                Aggregation.group("city", "countryCode").count().as("count"),
                Aggregation.sort(Sort.Direction.DESC, "count")
        );
        return mongoTemplate.aggregate(agg, "click_events", CityCountResult.class).getMappedResults();
    }

    public List<DayCount> dashboardClicksByDay(UUID userId, Instant from) {
        List<UUID> urlIds = mongoTemplate.find(
                Query.query(Criteria.where("userId").is(userId).and("deleted").is(false)),
                UrlIdOnly.class,
                "urls"
        ).stream().map(u -> u.id).toList();

        if (urlIds.isEmpty()) {
            return List.of();
        }

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("urlId").in(urlIds).and("clickedAt").gte(from)),
                Aggregation.project()
                        .andExpression("dateToString('%Y-%m-%d', clickedAt)").as("day"),
                Aggregation.group("day").count().as("count"),
                Aggregation.sort(Sort.Direction.ASC, "_id")
        );
        return mongoTemplate.aggregate(agg, "click_events", DayCount.class).getMappedResults();
    }

    public void incrementClickCount(UUID urlId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(urlId)),
                new Update().inc("clickCount", 1),
                "urls"
        );
    }

    public long sumClickCountByUserId(UUID userId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("userId").is(userId).and("deleted").is(false)),
                Aggregation.group().sum("clickCount").as("total")
        );
        TotalCount result = mongoTemplate.aggregate(agg, "urls", TotalCount.class).getUniqueMappedResult();
        return result != null ? result.getTotal() : 0L;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayCount {
        private String id;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CountryCount {
        private String id;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityCountResult {
        private CityKey id;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityKey {
        private String city;
        private String countryCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class UrlIdOnly {
        private UUID id;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TotalCount {
        private long total;
    }
}
