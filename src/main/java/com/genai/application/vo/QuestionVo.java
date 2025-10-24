package com.genai.application.vo;

import com.genai.application.domain.Answer;
import com.genai.application.domain.Document;
import lombok.Builder;
import reactor.core.publisher.Flux;

import java.util.List;

@Builder
public record QuestionVo(Flux<List<Answer>> answerStream, List<Document> documents) {}