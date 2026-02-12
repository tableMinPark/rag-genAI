package com.genai.core.repository.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public interface AnswerResponse {

    String getId();

    List<Data> getDatas();

    @Builder
    @Getter
    @AllArgsConstructor
    class Data {
        private final String reasoningContent;
        private final String content;
        private final String finishReason;
    }
}
