package com.genai.core.repository.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class KeywordSearchRequest {

    private final int size;

    private final List<Map<SortField, Order>> sort;

    private final Query query;

    public static Map<SortField, Order> sort(SortField sortField, String direction) {
        return Map.of(sortField, Order.builder().order(direction).build());
    }

    public static Query query(QueryType queryType, String query, List<String> fields, List<String> aliases) {

        Query.Bool.Must.MultiMatch multiMatch = Query.Bool.Must.MultiMatch.builder()
                .type(queryType.name())
                .query(query)
                .fields(fields)
                .build();

        Query.Bool.Must must = Query.Bool.Must.builder()
                .multiMatch(multiMatch)
                .build();

        List<Query.Bool.Filter> filter = aliases.stream()
                .map(alias -> Query.Bool.Filter.builder()
                        .term(Query.Bool.Filter.Term.builder()
                                .alias(alias)
                                .build())
                        .build())
                .toList();

        Query.Bool bool = Query.Bool.builder()
                .must(List.of(must))
                .filter(filter)
                .build();

        return Query.builder()
                .bool(bool)
                .build();
    }

    @Builder
    public record Order(String order) {}

    @Builder
    public record Query(Bool bool) {
        @Builder
        public record Bool(List<Must> must, List<Filter> filter) {
            @Builder
            public record Must(@JsonProperty("multi_match") MultiMatch multiMatch) {
                @Builder
                public record MultiMatch(String query, List<String> fields, String type) {}
            }
            @Builder
            public record Filter(Term term) {
                @Builder
                public record Term(String alias) {}
            }
        }
    }

    public enum SortField {
        _score
    }

    public enum QueryType {
        best_fields
    }
}
