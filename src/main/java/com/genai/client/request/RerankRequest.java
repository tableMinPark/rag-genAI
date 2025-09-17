package com.genai.client.request;

import com.genai.client.vo.DocumentVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
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
