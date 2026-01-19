package com.genai.core.repository.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class CreateIndexBulkRequest {

    private final Index index;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Index {
        @JsonProperty("_index")
        private String collectionId;
        @JsonProperty("_id")
        private String id;
    }
}
