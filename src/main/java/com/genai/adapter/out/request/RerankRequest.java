package com.genai.adapter.out.request;

import com.genai.application.domain.Document;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class RerankRequest<T> {

    private final String query;

    private final String field;

    private final List<Document<T>> document;

    @Builder
    public RerankRequest(String query, String field, List<Document<T>> document) {
        this.query = query;
        this.field = field;
        this.document = document;
    }
}
