package com.genai.core.service.impl;

import com.genai.core.config.constant.QuestionConst;
import com.genai.core.config.constant.SearchConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
import com.genai.core.service.ChatHistoryCoreService;
import com.genai.core.service.QuestionCoreService;
import com.genai.core.service.vo.AnswerVO;
import com.genai.core.service.vo.DocumentVO;
import com.genai.core.service.vo.QuestionVO;
import com.genai.core.type.CollectionType;
import com.genai.core.type.CollectionTypeFactory;
import com.genai.core.utils.DecisionDetectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionCoreServiceImpl implements QuestionCoreService {

    private final SearchRepository searchRepository;
    private final ModelRepository modelRepository;
    private final PromptRepository promptRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final CollectionTypeFactory collectionTypeFactory;
    private final ChatHistoryCoreService chatHistoryCoreService;
    private final TransactionTemplate transactionTemplate;

    /**
     * 질문 재작성
     *
     * @param query     질문
     * @param sessionId 세션 ID
     * @return 재작성 질문
     */
    public String rewriteQuery(String query, long chatId, String sessionId) {

        // 이전 대화 목록 조회
        List<ChatDetailEntity> chatDetailEntities = transactionTemplate.execute(status ->
                chatDetailRepository.findByChatIdOrderBySysCreateDtDesc(chatId, PageRequest.of(0, QuestionConst.REWRITE_QUERY_TURNS)).stream()
                        .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                        .toList());

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionConst.REWRITE_QUERY_PROMPT)
                .temperature(QuestionConst.REWRITE_QUERY_TEMPERATURE)
                .topP(QuestionConst.REWRITE_QUERY_TOP_P)
                .maximumTokens(QuestionConst.REWRITE_QUERY_MAXIMUM_TOKENS)
                .build();

        String rewriteQuery = query;
        if (chatDetailEntities != null && !chatDetailEntities.isEmpty()) {
            List<ConversationVO> conversationVos = chatDetailEntities.stream()
                    .map(chatDetailEntity -> ConversationVO.builder()
                            .query(chatDetailEntity.getRewriteQuery())
                            .answer(chatDetailEntity.getAnswer())
                            .build())
                    .toList();

            // 질의 재작성
            rewriteQuery = modelRepository.generateAnswerStr(query, null, null, conversationVos, sessionId, promptEntity);
        }

        return rewriteQuery.trim();
    }

    /**
     * 대화 상태 요약
     *
     * @param sessionId 세션 ID
     * @return 요약 답변 문자열
     */
    public String summaryState(String chatState, long chatId, String sessionId) {

        // 이전 대화 목록 조회
        List<ChatDetailEntity> chatDetailEntities = transactionTemplate.execute(status ->
                chatDetailRepository.findByChatIdOrderBySysCreateDtDesc(chatId, PageRequest.of(0, QuestionConst.SUMMARY_UPDATE_TURNS)).stream()
                .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                .toList());

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionConst.SUMMARY_UPDATE_PROMPT)
                .temperature(QuestionConst.SUMMARY_UPDATE_TEMPERATURE)
                .topP(QuestionConst.SUMMARY_UPDATE_TOP_P)
                .maximumTokens(QuestionConst.SUMMARY_UPDATE_MAXIMUM_TOKENS)
                .build();

        List<ConversationVO> conversationVos = chatDetailEntities == null
            ? Collections.emptyList()
            : chatDetailEntities.stream()
                .map(chatDetailEntity -> ConversationVO.builder()
                        .query(chatDetailEntity.getRewriteQuery())
                        .answer(chatDetailEntity.getAnswer())
                        .build())
                .toList();

        return modelRepository.generateAnswerStr(null, null, chatState, conversationVos, sessionId, promptEntity);
    }

    /**
     * AI 질문 & 답변
     *
     * @param query         질의문
     * @param sessionId     사용자 ID
     * @param chatId        대화 ID
     * @param categoryCodes 카테고리 코드 목록
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionAi(String query, String sessionId, long chatId, long promptId, List<String> categoryCodes) {
        return this.questionByCollectionId(query, sessionId, chatId, promptId, collectionTypeFactory.ai(), categoryCodes);
    }

    /**
     * AI 질문 & 답변
     *
     * @param query        질의문
     * @param sessionId    사용자 ID
     * @param chatId       대화 ID
     * @param categoryCode 카테고리 코드
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionMyAi(String query, String sessionId, long chatId, long promptId, String categoryCode) {
        return this.questionByCollectionId(query, sessionId, chatId, promptId, collectionTypeFactory.myai(), List.of(categoryCode));
    }

    /**
     * 컬렉션 ID 기준 질문 & 답변
     *
     * @param query          질의문
     * @param sessionId      세션 ID
     * @param promptId       프롬프트 ID
     * @param collectionType 컬렉션
     * @param chatId         대화 ID
     * @param categoryCodes  검색 필터
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionByCollectionId(String query, String sessionId, long chatId, long promptId, CollectionType collectionType, List<String> categoryCodes) {

        // 시스템 프롬 프트 조회
        PromptEntity promptEntity = promptRepository.findById(promptId)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 이전 대화 목록 조회
        List<ChatDetailEntity> chatDetailEntities = chatDetailRepository.findByChatIdOrderBySysCreateDtDesc(chatId, PageRequest.of(0, QuestionConst.MULTITURN_TURNS)).stream()
                .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                .toList();

        // 질의 재작성 (멀티턴)
        String rewriteQuery = this.rewriteQuery(query, chatId, sessionId);
        if (rewriteQuery.trim().isBlank()) rewriteQuery = query;

        // ####################################
        // 이전 대화 조회 및 정리
        // ####################################
        // 이전 대화 요약 문자열
        String chatState = chatEntity.getState();
        // 이전 대화 목록 Vo 목록
        List<ConversationVO> conversations = Collections.emptyList();
        if (!chatDetailEntities.isEmpty()) {
            conversations = chatDetailEntities.stream()
                    .map(chatDetailEntity -> ConversationVO.builder()
                            .query(chatDetailEntity.getRewriteQuery())
                            .answer(chatDetailEntity.getAnswer())
                            .build())
                    .toList();
        }

        // ####################################
        // RAG Context 정리
        // ####################################
        // 검색 결과 목록 (key 를 통한 중복 제거)
        Map<String, Search<DocumentEntity>> searchEntityMap = new HashMap<>();
        // 키워드 검색
        List<Search<DocumentEntity>> keywordSearchEntities = searchRepository.keywordSearch(collectionType, rewriteQuery, SearchConst.KEYWORD_TOP_K, sessionId, categoryCodes);
        keywordSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));
        // 벡터 검색
        List<Search<DocumentEntity>> vectorSearchEntities = searchRepository.vectorSearch(collectionType, rewriteQuery, SearchConst.VECTOR_TOP_K, categoryCodes);
        vectorSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));
        // 키워드 검색 결과, 벡터 검색 결과 변환
        List<Rerank> rerankEntities = searchRepository.rerank(rewriteQuery, searchEntityMap.values().stream()
                .map(searchEntity -> Rerank.builder()
                        .document(searchEntity.getFields())
                        .build())
                .toList());
        // 리랭킹
        List<Rerank> topRerankEntities = rerankEntities.stream()
                .filter(rerankEntity -> rerankEntity.getRerankScore() >= SearchConst.RERANK_SCORE_MIN)
                .toList();
        // 상위 RERANK_TOP_K 개 추출
        List<Rerank> finalTopRerankEntities = topRerankEntities
                .subList(0, Math.min(SearchConst.RERANK_TOP_K, topRerankEntities.size()));

        // Context 생성
        StringBuilder contextBuilder = new StringBuilder();
        for (Rerank rerank : finalTopRerankEntities) {
            contextBuilder.append("# ").append(rerank.getDocument().getTitle()).append("\n");
            contextBuilder.append("## ").append(rerank.getDocument().getSubTitle()).append("\n");
            contextBuilder.append("### ").append(rerank.getDocument().getThirdTitle()).append("\n");
            contextBuilder.append(rerank.getDocument().getCompactContent()).append("\n");
            contextBuilder.append(rerank.getDocument().getSubContent()).append("\n");
        }

        // ####################################
        // 답변 요청
        // ####################################
        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .rewriteQuery(rewriteQuery)
                .answer("")
                .build());

        // LLM 답변 스트림 요청
        Flux<List<AnswerVO>> originStream = modelRepository
                .generateStreamAnswer(rewriteQuery, contextBuilder.toString().trim(), chatState, conversations, sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.getIsInference())
                                .build())
                        .toList());

        // ####################################
        // 답변 이후 처리
        // ####################################
        StringBuilder answerBuilder = new StringBuilder();
        Flux<List<AnswerVO>> answerStream = originStream
                .doOnNext(answerVos -> {
                    for (AnswerVO answerVo : answerVos) {
                        if (!answerVo.getIsInference()) {
                            answerBuilder.append(answerVo.getContent());
                        }
                    }
                })
                .doOnComplete(() -> {
                    String answer = answerBuilder.toString().trim();
                    Mono.fromRunnable(() -> {
                        // 참고 문서 목록
                        List<ChatPassageEntity> chatPassageEntities = finalTopRerankEntities.stream()
                                .map(rerankEntity -> {
                                    String context = rerankEntity.getDocument().getTitle() + "\n" +
                                            rerankEntity.getDocument().getSubTitle() + "\n" +
                                            rerankEntity.getDocument().getThirdTitle() + "\n" +
                                            rerankEntity.getDocument().getContent() + "\n" +
                                            rerankEntity.getDocument().getSubContent() + "\n";

                                    context = context.replace("\\n", "\n");

                                    return ChatPassageEntity.builder()
                                            .msgId(chatDetailEntity.getMsgId())
                                            .fileDetailId(rerankEntity.getDocument().getFileDetailId())
                                            .sourceType(rerankEntity.getDocument().getSourceType())
                                            .categoryCode(rerankEntity.getDocument().getCategoryCode())
                                            .content(context)
                                            .build();
                                })
                                .toList();

                        chatHistoryCoreService.updateChatDetail(chatId, chatDetailEntity.getMsgId(), answer, chatPassageEntities);

                        if (DecisionDetectUtil.detect(query, answer)) {
                            String newChatState = summaryState(chatState, chatId, sessionId);
                            chatHistoryCoreService.updateChatState(chatId, newChatState);
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
                })
                .doOnError(e -> log.error("스트림 에러 발생: {}", e.getMessage()));

        return QuestionVO.builder()
                .answerStream(answerStream)
                .documents(finalTopRerankEntities.stream()
                        .map(rerank -> DocumentVO.builder()
                                .title(rerank.getDocument().getTitle())
                                .subTitle(rerank.getDocument().getSubContent())
                                .thirdTitle(rerank.getDocument().getThirdTitle())
                                .content(rerank.getDocument().getContent())
                                .subContent(rerank.getDocument().getSubContent())
                                .originFileName(rerank.getDocument().getOriginFileName())
                                .categoryCode(rerank.getDocument().getCategoryCode())
                                .sourceType(rerank.getDocument().getSourceType())
                                .url(rerank.getDocument().getUrl())
                                .ext(rerank.getDocument().getExt())
                                .build())
                        .toList())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }

    /**
     * LLM 질문 & 답변
     *
     * @param query     질의문
     * @param sessionId 사용자 ID
     * @param chatId    대화 ID
     * @param promptId  프롬프트 ID
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionLlm(String query, String sessionId, long chatId, long promptId) {

        // 시스템 프롬 프트 조회
        PromptEntity promptEntity = promptRepository.findById(promptId)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 이전 대화 목록 조회
        List<ChatDetailEntity> chatDetailEntities = chatDetailRepository.findByChatIdOrderBySysCreateDtDesc(chatId, PageRequest.of(0, QuestionConst.MULTITURN_TURNS)).stream()
                .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                .toList();

        // 질의 재작성 (멀티턴)
        String rewriteQuery = this.rewriteQuery(query, chatId, sessionId);
        if (rewriteQuery.trim().isBlank()) rewriteQuery = query;

        // ####################################
        // 이전 대화 조회 및 정리
        // ####################################;
        // 이전 대화 요약 문자열
        String chatState = chatEntity.getState();
        // 이전 대화 목록 Vo 목록
        List<ConversationVO> conversations = Collections.emptyList();
        if (!chatDetailEntities.isEmpty()) {
            conversations = chatDetailEntities.stream()
                    .map(chatDetailEntity -> ConversationVO.builder()
                            .query(chatDetailEntity.getRewriteQuery())
                            .answer(chatDetailEntity.getAnswer())
                            .build())
                    .toList();
        }

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .rewriteQuery(rewriteQuery)
                .answer("")
                .build());

        // LLM 답변 스트림 요청
        Flux<List<AnswerVO>> originStream = modelRepository
                .generateStreamAnswer(rewriteQuery, chatState, conversations, sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.getIsInference())
                                .build())
                        .toList());

        StringBuilder answerBuilder = new StringBuilder();
        Flux<List<AnswerVO>> answerStream = originStream
                .doOnNext(answerVos -> {
                    for (AnswerVO answerVo : answerVos) {
                        if (!answerVo.getIsInference()) {
                            answerBuilder.append(answerVo.getContent());
                        }
                    }
                })
                .doOnComplete(() -> {
                    String answer = answerBuilder.toString().trim();
                    Mono.fromRunnable(() -> {
                        chatHistoryCoreService.updateChatDetail(chatId, chatDetailEntity.getMsgId(), answer);
                        if (DecisionDetectUtil.detect(query, answer)) {
                            String newChatState = summaryState(chatState, chatId, sessionId);
                            chatHistoryCoreService.updateChatState(chatId, newChatState);
                        }
                    })
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
                })
                .doOnError(e -> log.error("스트림 에러 발생: {}", e.getMessage()));

        return QuestionVO.builder()
                .answerStream(answerStream)
                .documents(Collections.emptyList())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }

    /**
     * LLM Simulation 질문 & 답변
     *
     * @param query         질의문
     * @param sessionId     사용자 ID
     * @param chatId        대화 ID
     * @param context       참고 문서 (Context)
     * @param promptContent 프롬프트 본문
     * @param temperature   창의성
     * @param topP          일관성
     * @param maximumTokens 최대 토큰 수
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionSimulation(String query, String sessionId, long chatId, String context, String promptContent, double temperature, double topP, int maximumTokens) {

        // 시스템 프롬 프트 조회
        PromptEntity promptEntity = PromptEntity.builder()
                .promptId(Long.MIN_VALUE)
                .promptContent(promptContent)
                .temperature(temperature)
                .topP(topP)
                .maximumTokens(maximumTokens)
                .build();

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .rewriteQuery(query)
                .answer("")
                .build());

        // LLM 답변 스트림 요청
        Flux<List<AnswerVO>> originStream = modelRepository
                .generateStreamAnswer(query, context, null, Collections.emptyList(), sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.getIsInference())
                                .build())
                        .toList());

        StringBuilder answerBuilder = new StringBuilder();
        Flux<List<AnswerVO>> answerStream = originStream
                .doOnNext(answerVos -> {
                    for (AnswerVO answerVo : answerVos) {
                        if (!answerVo.getIsInference()) {
                            answerBuilder.append(answerVo.getContent());
                        }
                    }
                })
                .doOnComplete(() -> {
                    String answer = answerBuilder.toString().trim();
                    Mono.fromRunnable(() -> chatHistoryCoreService.updateChatDetail(chatId, chatDetailEntity.getMsgId(), answer))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
                })
                .doOnError(e -> log.error("스트림 에러 발생: {}", e.getMessage()));

        return QuestionVO.builder()
                .answerStream(answerStream)
                .documents(Collections.emptyList())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}
