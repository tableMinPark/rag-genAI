package com.genai.core.repository.request;

import lombok.*;

import java.util.List;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class RerankRequest {

    private final String query;

    private final List<Document> documents;

    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private String id;
        private String content;
    }
}
