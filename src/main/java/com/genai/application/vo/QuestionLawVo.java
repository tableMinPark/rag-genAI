package com.genai.application.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import reactor.core.publisher.Flux;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class QuestionLawVo {

    private final Flux<String> answerStream;

    private final List<ReferenceDocumentVo> documents;
}
