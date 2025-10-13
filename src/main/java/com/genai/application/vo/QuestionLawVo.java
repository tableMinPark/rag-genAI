package com.genai.application.vo;

import com.genai.application.domain.Answer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import reactor.core.publisher.Flux;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class QuestionLawVo {

    private final Flux<List<Answer>> answerStream;

    private final List<ReferenceDocumentVo> documents;
}
