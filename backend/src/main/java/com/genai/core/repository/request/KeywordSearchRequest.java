package com.genai.core.repository.request;

import lombok.*;

import java.util.List;
import java.util.Map;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class KeywordSearchRequest {

    private final int size;

    private final Map<SortField, Order> sort;

    private final Map<QueryField, Query> query;

    public static Map<SortField, Order> sort(SortField sortField, String direction) {
        return Map.of(sortField, Order.builder().order(direction).build());
    }

    public static Map<QueryField, Query> query(QueryField queryField, QueryType queryType, String query, List<String> fields) {
        return Map.of(queryField, Query.builder()
                .type(queryType.name())
                .query(query)
                .fields(fields)
                .build());
    }

    @Builder
    public record Order(String order) {}
    @Builder
    public record Query(String query, List<String> fields, String type) {}

    public enum SortField {
        _score
    }

    public enum QueryField {
        multi_match
    }
    public enum QueryType {
        best_fields
    }
}
