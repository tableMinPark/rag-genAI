package com.genai.core.repository.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class VectorSearchRequest {

    private final int size;

    private final Map<QueryField, Map<String, Field>> query;

    public static Map<QueryField, Map<String, Field>> query(QueryField queryField, List<String> fields, List<Float> vector) {

        Map<String, Field> queries = new HashMap<>();

        fields.forEach(field -> queries.put(field, Field.builder()
                .k(20)
                .vector(vector)
                .build()));

        return Map.of(queryField, queries);
    }

    @Builder
    public record Field(int k, List<Float> vector) {}

    public enum QueryField {
        knn
    }
}
