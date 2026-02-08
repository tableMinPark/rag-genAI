package com.genai.core.service.business.vo;

import com.genai.core.service.business.subscriber.StreamEvent;
import lombok.Builder;
import reactor.core.publisher.Flux;

import java.util.List;

@Builder
public record QuestionVO(
    Flux<StreamEvent> answerStream,
    List<DocumentVO> documents,
    long msgId
) {}