package com.genai.core.repository.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class DeleteIndexBulkRequest {

    private final Delete delete;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delete {
        @JsonProperty("_id")
        private String id;
    }
}
