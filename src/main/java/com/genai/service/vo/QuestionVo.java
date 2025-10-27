package com.genai.service.vo;

import com.genai.service.domain.Answer;
import com.genai.service.domain.Document;
import lombok.Builder;
import reactor.core.publisher.Flux;

import java.util.List;

@Builder
public record QuestionVo(Flux<List<Answer>> answerStream, List<Document> documents) {}