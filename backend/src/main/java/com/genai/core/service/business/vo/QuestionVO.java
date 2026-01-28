package com.genai.core.service.business.vo;

import lombok.Builder;
import reactor.core.publisher.Flux;

import java.util.List;

@Builder
public record QuestionVO(
    Flux<List<StreamEventVO>> answerStream,
    List<DocumentVO> documents,
    long msgId
) {}