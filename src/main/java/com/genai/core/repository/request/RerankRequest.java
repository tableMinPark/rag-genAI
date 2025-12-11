package com.genai.core.repository.request;

import com.genai.core.repository.wrapper.Rerank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class RerankRequest {

    private final String query;

    private final String field;

    private final List<Rerank> document;
}
