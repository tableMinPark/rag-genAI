package com.genai.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.global.constant.ModelConst;
import com.genai.global.constant.SearchConst;
import com.genai.global.enums.MenuType;
import com.genai.repository.ChatHistoryRepository;
import com.genai.repository.ModelRepository;
import com.genai.repository.PromptRepository;
import com.genai.repository.SearchRepository;
import com.genai.service.ChatService;
import com.genai.service.domain.*;
import com.genai.service.vo.QuestionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    private final SearchRepository searchRepository;

    private final ModelRepository modelRepository;

    private final PromptRepository promptRepository;

    private final ChatHistoryRepository chatHistoryRepository;

    private final ObjectMapper objectMapper;

    @Autowired
    public ChatServiceImpl(
            SearchRepository searchRepository,
            @Qualifier("QwenModelRepositoryImpl") ModelRepository modelRepository,
            PromptRepository promptRepository,
            ObjectMapper objectMapper,
            ChatHistoryRepository chatHistoryRepository
    ) {
        this.searchRepository = searchRepository;
        this.modelRepository = modelRepository;
        this.promptRepository = promptRepository;
        this.objectMapper = objectMapper;
        this.chatHistoryRepository = chatHistoryRepository;
    }

    /**
     * RAG 질의
     *
     * @param menuType 컬렉션 타입
     * @param query          질의문
     * @param sessionId      세션 식별자
     */
    @Override
    public QuestionVo questionRagUseCase(String query, String sessionId, MenuType menuType) {

        // 시스템 프롬 프트 조회
        Prompt prompt = promptRepository.getPrompt(menuType.getPromptCode());

        // 검색 결과 목록 (key 를 통한 중복 제거)
        Map<String, Search<Document>> searchDocumentMap = new HashMap<>();

        // 키워드 검색
        List<Search<Document>> keywordSearchDocuments = searchRepository.keywordSearch(menuType, menuType.getCollectionId(), query, SearchConst.KEYWORD_TOP_K, sessionId);
        keywordSearchDocuments.forEach(lawDocument -> searchDocumentMap.put(lawDocument.getFields().getDocId(), lawDocument));

        // 벡터 검색
        List<Search<Document>> vectorSearchDocuments = searchRepository.vectorSearch(menuType, menuType.getCollectionId(), query, SearchConst.VECTOR_TOP_K);
        vectorSearchDocuments.forEach(lawDocument -> searchDocumentMap.put(lawDocument.getFields().getDocId(), lawDocument));

        // 키워드 검색 결과, 벡터 검색 결과 리랭킹
        List<Rerank> reranks = searchRepository.rerank(query, searchDocumentMap.values().stream()
                .map(searchDocument -> Rerank.builder()
                        .document(searchDocument.getFields())
                        .build())
                .toList());

        // 리랭킹 스코어 기준 필터링
        List<Rerank> filteredReranks = reranks.stream()
                .filter(document -> document.getRerankScore() >= SearchConst.RERANK_SCORE_MIN)
                .toList();

        // 상위 RERANK_TOP_K 개 추출
        List<Rerank> topReranks = filteredReranks.subList(0, Math.min(SearchConst.RERANK_TOP_K, filteredReranks.size()));

        // Context 생성
        List<Context> contexts = reranks.stream()
                .map(rerank -> {
                    String subContent = rerank.getDocument().getSubContent();
                    // 부가 본문 압축 (문자열 자름)
                    String subContentCompress = subContent.substring(0, Math.min(subContent.length(), ModelConst.SUB_CONTENT_MAX_TOKEN_SIZE));

                    return Context.builder()
                            .title(rerank.getDocument().getTitle())
                            .subTitle(rerank.getDocument().getSubTitle())
                            .thirdTitle(rerank.getDocument().getThirdTitle())
                            .content(rerank.getDocument().getContent())
                            .subContent(subContentCompress)
                            .build();
                })
                .toList();

        // Context Json 직렬화
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(contexts);
        } catch (JsonProcessingException e) {
            // 직렬화 실패 경우
            StringBuilder contextBuilder = new StringBuilder();
            for (Rerank rerank : reranks) {
                contextBuilder.append(rerank.getDocument().getContext());
            }
            contextJson = contextBuilder.toString();
        }

        // LLM 답변 스트림 요청
        // hot stream 으로 변경 (답변 전송 + ChatService 내 로그 수집 목적)
        Flux<List<Answer>> answerStream = modelRepository
                .generateStreamAnswer(query, contextJson.trim(), sessionId, prompt)
                .share();

        StringBuilder inferenceBuilder = new StringBuilder();
        StringBuilder answerBuilder = new StringBuilder();
        answerStream.subscribe(answers -> {
            for (Answer answer : answers) {
                if (answer.isInference()) {
                    inferenceBuilder.append(answer.getContent());
                } else {
                    answerBuilder.append(answer.getContent());
                }
            }
        }, throwable -> {
        }, () -> chatHistoryRepository.registerRagChatHistory(RagChatHistory.builder()
                .menuType(menuType)
                .keywordSearchDocuments(keywordSearchDocuments)
                .vectorSearchDocuments(vectorSearchDocuments)
                .reranks(reranks)
                .topReranks(topReranks)
                .contexts(contexts)
                .promptCode(menuType.getPromptCode())
                .query(query)
                .inference(inferenceBuilder.toString().trim())
                .answer(answerBuilder.toString().trim())
                .sessionId(sessionId)
                .build()));

        return QuestionVo.builder()
                .answerStream(answerStream)
                .documents(topReranks.stream().map(Rerank::getDocument).toList())
                .build();
    }

    /**
     * LLM 질의
     *
     * @param query         질의문
     * @param context       참고 문서
     * @param promptContext 시스템 프롬 프트
     * @param sessionId     세션 식별자
     * @param maxToken      최대 토큰 수
     * @param temperature   창의성 조정 값
     * @param topP          응답 가변성 조정 값
     */
    @Override
    public QuestionVo questionUseCase(String query, String sessionId, String context, String promptContext, int maxToken, double temperature, double topP) {

        // 시스템 프롬 프트 조회
        Prompt prompt = promptRepository.getPrompt(ModelConst.LLM_DEFAULT_PROMPT_CODE);

        // 시스템 프롬 프트 조정
        prompt.setParameter(promptContext, temperature, topP, 0, 20, maxToken);

        // LLM 답변 스트림 요청
        // hot stream 으로 변경 (답변 전송 + ChatService 내 로그 수집 목적)
        Flux<List<Answer>> answerStream = modelRepository
                .generateStreamAnswer(query, context, sessionId, prompt)
                .share();

        StringBuilder inferenceBuilder = new StringBuilder();
        StringBuilder answerBuilder = new StringBuilder();
        answerStream.subscribe(answers -> {
            for (Answer answer : answers) {
                if (answer.isInference()) {
                    inferenceBuilder.append(answer.getContent());
                } else {
                    answerBuilder.append(answer.getContent());
                }
            }
        }, throwable -> {
        }, () -> chatHistoryRepository.registerChatHistory(ChatHistory.builder()
                .promptCode(ModelConst.LLM_DEFAULT_PROMPT_CODE)
                .query(query)
                .inference(inferenceBuilder.toString().trim())
                .answer(answerBuilder.toString().trim())
                .sessionId(sessionId)
                .build()));

        return QuestionVo.builder()
                .answerStream(answerStream)
                .documents(Collections.emptyList())
                .build();
    }
}
