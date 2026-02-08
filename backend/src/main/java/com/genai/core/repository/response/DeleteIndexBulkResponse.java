package com.genai.core.repository.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteIndexBulkResponse {

    private Boolean errors;

    private List<Item> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        @JsonProperty("_id")
        private String id;
        @JsonProperty("status")
        private Integer status;
        @JsonProperty("error")
        private Map<String, Object> error;
    }
}
