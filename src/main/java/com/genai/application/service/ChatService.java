package com.genai.application.service;

import com.genai.application.domain.Document;
import com.genai.application.domain.Law;
import com.genai.application.domain.Prompt;
import com.genai.application.domain.RerankDocument;
import com.genai.application.port.ModelPort;
import com.genai.application.port.PromptPort;
import com.genai.application.port.SearchPort;
import com.genai.constant.SearchConst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SearchPort searchPort;

    private final ModelPort modelPort;

    private final PromptPort promptPort;

    /**
     * 법령 질의
     *
     * @param query     질의문
     * @param sessionId 세션 식별자
     */
    public Flux<String> questionLawUseCase(String query, String sessionId) {

        // 프롬 프트 조회
        Prompt prompt = promptPort.getPrompt("PROM-001");

        // 검색 결과 목록
        List<Document<Law>> documents = new ArrayList<>();

        // 키워드 검색
        documents.addAll(searchPort.lawKeywordSearch(query, SearchConst.KEYWORD_TOP_K, sessionId));

        // 벡터 검색
        documents.addAll(searchPort.lawVectorSearch(query, SearchConst.VECTOR_TOP_K));

        // 키워드 검색 결과, 벡터 검색 결과 리랭킹
        List<RerankDocument<Law>> rerank = modelPort.lawRerank(query, documents);

        // Context 생성
        StringBuilder contextBuilder = new StringBuilder();
        rerank.stream()
                .filter(document -> document.getRerankScore() >= SearchConst.RERANK_SCORE_MIN)
                .forEach(document -> contextBuilder.append(document.getFields().getContext()).append("\n"));

        return modelPort.generateStreamAnswer(query, contextBuilder.toString().trim(), sessionId, prompt)
                .map(answers -> {
                    StringBuilder answerBuilder = new StringBuilder();
                    answers.forEach(answer -> answerBuilder.append(answer.getContent()));
                    return answerBuilder.toString();
                });
    }

    /**
     * LLM 테스트  질의
     *
     * @param query     질의문
     * @param sessionId 세션 식별자
     */
    public Flux<String> questionUseCase(String query, String context, String promptContext, String sessionId) {

        // 프롬 프트 조회
        Prompt prompt = Prompt.builder()
                .promptCode("TEST-PROMPT-CODE")
                .promptName("테스트 시스템 프롬 프트")
                .context(promptContext)
                .temperature(0.01)
                .topP(0.9)
                .maxTokens(4096)
                .build();

        return modelPort.generateStreamAnswer(query, context, sessionId, prompt)
                .map(answers -> {
                    StringBuilder answerBuilder = new StringBuilder();
                    answers.forEach(answer -> answerBuilder.append(answer.getContent()));
                    return answerBuilder.toString();
                });
    }
}
