package com.genai.core.service.vo;

import lombok.Builder;
import reactor.core.publisher.Flux;

import java.util.List;

@Builder
public record QuestionVO(Flux<List<AnswerVO>> answerStream, List<DocumentVO> documents, long msgId) {}