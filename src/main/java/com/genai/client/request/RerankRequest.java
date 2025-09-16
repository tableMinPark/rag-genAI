package com.genai.client.request;

import com.genai.client.vo.DocumentVo;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RerankRequest<T> {

    private final String query;

    private final String field;

    private final List<DocumentVo<T>> document;

    public RerankRequest(String query, List<DocumentVo<T>> document) {
        this.query = query;
        this.field = "context";
        this.document = document;
    }
}
