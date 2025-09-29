package com.genai.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.application.domain.*;
import com.genai.application.port.ModelPort;
import com.genai.application.port.PromptPort;
import com.genai.application.port.SearchPort;
import com.genai.constant.SearchConst;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final SearchPort searchPort;

    private final ModelPort modelPort;

    private final PromptPort promptPort;

    private final ObjectMapper objectMapper;

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
        Map<String, Document<Law>> documentMap = new HashMap<>();

        // 키워드 검색
        searchPort.lawKeywordSearch(query, SearchConst.KEYWORD_TOP_K, sessionId)
                .forEach(lawDocument -> documentMap.put(lawDocument.getFields().getDocId(), lawDocument));

        // 벡터 검색
        searchPort.lawVectorSearch(query, SearchConst.VECTOR_TOP_K)
                .forEach(lawDocument -> documentMap.put(lawDocument.getFields().getDocId(), lawDocument));

        // 키워드 검색 결과, 벡터 검색 결과 리랭킹
        List<RerankDocument<Law>> rerankDocuments = modelPort.lawRerank(query, documentMap.values().stream().toList()).stream()
                .filter(document -> document.getRerankScore() >= SearchConst.RERANK_SCORE_MIN)
                .toList();

        // Context 생성
        StringBuilder contextBuilder = new StringBuilder();
        List<Context> contexts = new ArrayList<>();
        for (RerankDocument<Law> rerankDocument : rerankDocuments) {
            Context context = Context.builder()
                    .title(rerankDocument.getFields().getTitle())
                    .subTitle(rerankDocument.getFields().getSubTitle())
                    .thirdTitle(rerankDocument.getFields().getThirdTitle())
                    .content(rerankDocument.getFields().getContent())
                    .subContent(rerankDocument.getFields().getSubContent())
                    .build();

            contexts.add(context);
            contextBuilder.append(rerankDocument.getFields().getContext());
        }

        // 상위 RERANK_TOP_K 개 추출
        contexts = contexts.subList(0, Math.min(SearchConst.RERANK_TOP_K, contexts.size()));

        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(contexts);
        } catch (JsonProcessingException e) {
            contextJson = contextBuilder.toString();
        }

        return modelPort.generateStreamAnswer(query, contextJson.trim(), sessionId, prompt)
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
