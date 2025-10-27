package com.genai.repository.request;

import com.genai.service.domain.Rerank;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class RerankRequest {

    private final String query;

    private final String field;

    private final List<Rerank> document;

    @Builder
    public RerankRequest(String query, String field, List<Rerank> document) {
        this.query = query;
        this.field = field;
        this.document = document;
    }
}
