package com.genai.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.application.domain.*;
import com.genai.application.port.ModelPort;
import com.genai.application.port.PromptPort;
import com.genai.application.port.SearchPort;
import com.genai.application.vo.QuestionLawVo;
import com.genai.application.vo.ReferenceDocumentVo;
import com.genai.constant.SearchConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final SearchPort searchPort;

    private final ModelPort modelPort;

    private final PromptPort promptPort;

    private final ObjectMapper objectMapper;

    public ChatService(
            @Autowired SearchPort searchPort,
            @Qualifier("QwenModelPortAdapter") ModelPort modelPort,
            @Autowired PromptPort promptPort,
            @Autowired ObjectMapper objectMapper
    ) {
        this.searchPort = searchPort;
        this.modelPort = modelPort;
        this.promptPort = promptPort;
        this.objectMapper = objectMapper;
    }

    /**
     * 법령 질의
     *
     * @param query     질의문
     * @param sessionId 세션 식별자
     */
    public QuestionLawVo questionLawUseCase(String query, String sessionId) {

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
        List<RerankDocument<Law>> rerankDocuments = searchPort.lawRerank(query, documentMap.values().stream().toList()).stream()
                .filter(document -> document.getRerankScore() >= SearchConst.RERANK_SCORE_MIN)
                .toList();

        // 상위 RERANK_TOP_K 개 추출
        rerankDocuments = rerankDocuments.subList(0, Math.min(SearchConst.RERANK_TOP_K, rerankDocuments.size()));

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

        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(contexts);
        } catch (JsonProcessingException e) {
            contextJson = contextBuilder.toString();
        }

        Flux<List<Answer>> answerStream = modelPort.generateStreamAnswer(query, contextJson.trim(), sessionId, prompt);

        return QuestionLawVo.builder()
                .answerStream(answerStream)
                .documents(rerankDocuments.stream()
                        .map(rerankDocument -> ReferenceDocumentVo.builder()
                                .title(rerankDocument.getFields().getTitle())
                                .subTitle(rerankDocument.getFields().getSubTitle())
                                .thirdTitle(rerankDocument.getFields().getThirdTitle())
                                .content(rerankDocument.getFields().getContent())
                                .subContent(rerankDocument.getFields().getSubContent())
                                .filePath(rerankDocument.getFields().getFilePath())
                                .url(rerankDocument.getFields().getUrl())
                                .docType(rerankDocument.getFields().getDocType())
                                .categoryCode(rerankDocument.getFields().getCategoryCode())
                                .build())
                        .toList())
                .build();
    }

    /**
     * LLM 테스트  질의
     *
     * @param query     질의문
     * @param sessionId 세션 식별자
     */
    public Flux<List<Answer>> questionUseCase(String query, String context, String promptContext, String sessionId, int maxToken, double temperature, double topP) {

        // 프롬 프트 조회
        Prompt prompt = Prompt.builder()
                .promptCode("TEST-PROMPT-CODE")
                .promptName("테스트 시스템 프롬 프트")
                .context(promptContext)
                .temperature(temperature)
                .topP(topP)
                .maxTokens(maxToken)
                .build();

        return modelPort.generateStreamAnswer(query, context, sessionId, prompt);
    }
}
