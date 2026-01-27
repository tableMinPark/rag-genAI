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

    private final Query query;

    public static Query query(List<String> fields, List<Float> vector, List<String> aliases) {

        Map<String, Field> knn = new HashMap<>();

        fields.forEach(field -> knn.put(field, Field.builder()
                .k(20)
                .vector(vector)
                .build()));

        Query.Bool.Must must = Query.Bool.Must.builder()
                .knn(knn)
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
    public record Query(Bool bool) {
        @Builder
        public record Bool(List<Must> must, List<Filter> filter) {
            @Builder
            public record Must(Map<String, Field> knn) {
            }
            @Builder
            public record Filter(Filter.Term term) {
                @Builder
                public record Term(String alias) {}
            }
        }
    }

    @Builder
    public record Field(int k, List<Float> vector) {}

}
