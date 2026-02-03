package com.genai.core.service.business.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.constant.QuestionConst;
import com.genai.core.constant.SearchConst;
import com.genai.core.constant.StreamConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
import com.genai.core.service.business.QuestionCoreService;
import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.DocumentVO;
import com.genai.core.service.business.vo.QuestionContextVO;
import com.genai.core.service.business.vo.QuestionVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.QuestionModuleService;
import com.genai.core.type.CollectionType;
import com.genai.core.type.CollectionTypeFactory;
import com.genai.global.utils.DecisionDetectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionCoreServiceImpl implements QuestionCoreService {

    private final QuestionModuleService questionModuleService;
    private final ChatDetailRepository chatDetailRepository;
    private final SearchRepository searchRepository;
    private final ModelRepository modelRepository;
    private final PromptRepository promptRepository;
    private final ChatRepository chatRepository;
    private final CollectionTypeFactory collectionTypeFactory;
    private final ChatHistoryModuleService chatHistoryModuleService;
    private final ObjectMapper objectMapper;

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
    @Override
    public QuestionVO questionByCollectionId(String query, String sessionId, long chatId, long promptId, CollectionType collectionType, List<String> categoryCodes) {

        // 시스템 프롬 프트 조회
        PromptEntity promptEntity = promptRepository.findById(promptId)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        // 현재 대화 조회
        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 새로운 대화 등록
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        // 이전 대화 요약 문자열
        String chatState = chatEntity.getState() == null ? "" : chatEntity.getState();

        // 이전 대화 상세 내역
        Mono<List<ConversationVO>> conversationMono = questionModuleService.getConversations(chatId)
                .collectList()
                .doOnSubscribe(s -> log.info("🔥 [1] conversationMono 실제 실행 (DB 조회)"))
                .cache()
                .doOnSuccess(s -> log.info("✅ conversationMono 완료"))
                ;

        // 질의 재정의
        Mono<String> rewriteQueryMono = conversationMono
                .flatMap(conversations -> questionModuleService.rewriteQuery(query, conversations, sessionId))
                .doOnSubscribe(s -> log.info("🔥 [2] rewriteQueryMono 실제 실행 (LLM 요청)"))
                .cache()
                .doOnSuccess(s -> log.info("✅ rewriteQueryMono 완료"))
                ;

        // 검색
        Mono<List<Rerank>> rerankFlux = rewriteQueryMono.flatMap(rewriteQuery ->
                Mono.fromCallable(() -> {
                    // 검색 결과 목록 (key 를 통한 중복 제거)
                    Map<Long, Search<DocumentEntity>> searchEntityMap = new HashMap<>();

                    // 키워드 검색
                    List<Search<DocumentEntity>> keywordSearchEntities = searchRepository.keywordSearch(collectionType, rewriteQuery, SearchConst.KEYWORD_TOP_K, sessionId, categoryCodes);
                    keywordSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));

                    // 벡터 검색
                    List<Search<DocumentEntity>> vectorSearchEntities = searchRepository.vectorSearch(collectionType, rewriteQuery, SearchConst.VECTOR_TOP_K, categoryCodes);
                    vectorSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));

                    // 키워드 검색 결과, 벡터 검색 결과 변환
                    List<Rerank> rerankEntities = searchRepository.rerank(rewriteQuery, searchEntityMap.values().stream()
                            .filter(searchEntity -> searchEntity.getScore() >= SearchConst.SEARCH_SCORE_MIN)
                            .map(searchEntity -> Rerank.builder()
                                    .document(searchEntity.getFields())
                                    .build())
                            .toList());

                    // 상위 RERANK_TOP_K 개 추출
                    return rerankEntities.subList(0, Math.min(SearchConst.RERANK_TOP_K, rerankEntities.size()));

                }).subscribeOn(Schedulers.boundedElastic()))
                .doOnSubscribe(s -> log.info("🔥 [3] rerankMono 실제 실행 (벡터/키워드 검색)"))
                .cache()
                .doOnSuccess(s -> log.info("✅ rerankMono 완료"))
                ;

        Mono<QuestionContextVO> contextMono = Mono.zip(conversationMono, rewriteQueryMono, rerankFlux)
                .map(tuple -> QuestionContextVO.builder()
                        .conversations(tuple.getT1())
                        .query(query)
                        .rewriteQuery(tuple.getT2())
                        .reranks(tuple.getT3())
                        .build())
                .doOnSubscribe(s -> log.info("🔥 [4] contextMono 조합 시작"))
                .cache()
                .doOnSuccess(s -> log.info("✅ contextMono 완료"))
                ;

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();

        // 답변 Entity
        Flux<StreamEvent> answerFlux = contextMono.flatMapMany(ctx -> {
                    List<ConversationVO> conversations = ctx.getConversations();
                    String rewriteQuery = ctx.getRewriteQuery();
                    List<Rerank> rerankEntities = ctx.getReranks();

                    StringBuilder contextBuilder = new StringBuilder();
                    for (Rerank rerank : rerankEntities) {
                        contextBuilder.append("# ").append(rerank.getDocument().getTitle()).append("\n");
                        contextBuilder.append("## ").append(rerank.getDocument().getSubTitle()).append("\n");
                        contextBuilder.append("### ").append(rerank.getDocument().getThirdTitle()).append("\n");
                        contextBuilder.append(rerank.getDocument().getCompactContent()).append("\n");
                        contextBuilder.append(rerank.getDocument().getSubContent()).append("\n");
                    }

                    // 답변 요청
                    return modelRepository.generateStreamAnswerAsync(rewriteQuery, contextBuilder.toString().trim(), chatState, conversations, sessionId, promptEntity);
                })
                .doOnNext(answerEntity -> {
                    if (!answerEntity.getIsInference()) {
                        answerAccumulator.append(answerEntity.getContent());
                    }
                })
                .doOnSubscribe(s -> log.info("🔥 [5] 답변 생성 스트림 시작"))
                .map(answerEntity -> StreamEvent.builder()
                        .id(answerEntity.getId())
                        .content(answerEntity.getContent())
                        .event(answerEntity.getIsInference() ? StreamConst.Event.INFERENCE : StreamConst.Event.ANSWER)
                        .build())
                .doOnComplete(() -> log.info("✅ answerStreamFlux 완료"))
                ;

        // 참고 문서 Flux
        Mono<StreamEvent> referenceMono = rerankFlux
                .map(reranks -> {
                    String answer = answerAccumulator.toString().trim();

                    StringBuilder referencePatternBuilder = new StringBuilder();
                    for (int index = 0; index < QuestionConst.REFERENCE_VALID_PATTERN.size(); index++) {
                        String pattern = QuestionConst.REFERENCE_VALID_PATTERN.get(index);
                        referencePatternBuilder.append("(").append(pattern).append(")");

                        if (index < QuestionConst.REFERENCE_VALID_PATTERN.size() - 1) {
                            referencePatternBuilder.append("|");
                        }
                    }

                    Pattern referencePattern = Pattern.compile(referencePatternBuilder.toString());

                    String content = "{}";

                    if (!referencePattern.matcher(answer).find()) {
                        try {
                            content = objectMapper.writeValueAsString(reranks.stream()
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
                                    .toList());

                        } catch (JsonProcessingException ignored) {
                        }
                    }

                    return StreamEvent.builder()
                            .id(sessionId)
                            .event(StreamConst.Event.REFERENCE)
                            .content(content)
                            .build();
                });

        // 대화 이력 업데이트
        Mono<Void> chatHistoryMono = contextMono.flatMap(ctx -> {
            String answer = answerAccumulator.toString().trim();
            List<ConversationVO> conversations = ctx.getConversations();
            String rewriteQuery = ctx.getRewriteQuery();
            List<Rerank> rerankEntities = ctx.getReranks();

                // 1. 대화 상태 요약
                Mono<Void> summaryMono = !DecisionDetectUtil.detect(query, answer)
                    ? Mono.empty()
                    : questionModuleService.summaryState(chatState, conversations, sessionId).flatMap(newChatState ->
                        Mono.fromRunnable(() -> chatHistoryModuleService.updateChatState(chatId, newChatState)));

                // 2. passage + answer 저장
                Mono<Void> saveMono = Mono.fromRunnable(() -> {
                    List<ChatPassageEntity> chatPassageEntities = rerankEntities.stream()
                        .map(rerank -> {
                            String context =
                                    rerank.getDocument().getTitle() + "\n" +
                                    rerank.getDocument().getSubTitle() + "\n" +
                                    rerank.getDocument().getThirdTitle() + "\n" +
                                    rerank.getDocument().getContent() + "\n" +
                                    rerank.getDocument().getSubContent() + "\n";

                            context = context.replace("\\n", "\n");

                            return ChatPassageEntity.builder()
                                    .msgId(chatDetailEntity.getMsgId())
                                    .fileDetailId(rerank.getDocument().getFileDetailId())
                                    .sourceType(rerank.getDocument().getSourceType())
                                    .categoryCode(rerank.getDocument().getCategoryCode())
                                    .content(context)
                                    .build();
                        })
                        .toList();

                        chatHistoryModuleService.updateChatDetail(
                                chatId,
                                chatDetailEntity.getMsgId(),
                                rewriteQuery,
                                answer,
                                chatPassageEntities
                        );
                    });

                return Mono.when(summaryMono, saveMono).subscribeOn(Schedulers.boundedElastic());
            });

        Flux<StreamEvent> answerStream = Flux
                .concat(answerFlux, referenceMono)
                .concatWith(chatHistoryMono.then(Mono.empty()));

        return QuestionVO.builder()
                .answerStream(answerStream)
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

        // 새로운 대화 등록
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        // 이전 대화 요약 문자열
        String chatState = chatEntity.getState() == null ? "" : chatEntity.getState();

        // 이전 대화 상세 내역
        Mono<List<ConversationVO>> conversationMono = questionModuleService.getConversations(chatId)
                .collectList()
                .doOnSubscribe(s -> log.info("🔥 [1] conversationMono 실제 실행 (DB 조회)"))
                .cache()
                .doOnSuccess(s -> log.info("✅ conversationMono 완료"))
                ;

        // 질의 재정의
        Mono<String> rewriteQueryMono = conversationMono
                .flatMap(conversations -> questionModuleService.rewriteQuery(query, conversations, sessionId))
                .doOnSubscribe(s -> log.info("🔥 [2] rewriteQueryMono 실제 실행 (LLM 요청)"))
                .cache()
                .doOnSuccess(s -> log.info("✅ rewriteQueryMono 완료"))
                ;

        Mono<QuestionContextVO> contextMono = Mono.zip(conversationMono, rewriteQueryMono)
                .map(tuple -> QuestionContextVO.builder()
                        .conversations(tuple.getT1())
                        .query(query)
                        .rewriteQuery(tuple.getT2())
                        .reranks(Collections.emptyList())
                        .build())
                .doOnSubscribe(s -> log.info("🔥 [3] contextMono 조합 시작"))
                .cache()
                .doOnSuccess(s -> log.info("✅ contextMono 완료"))
                ;

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();

        // 답변 Entity
        Flux<StreamEvent> answerFlux = contextMono
                .flatMapMany(ctx -> {
                    List<ConversationVO> conversations = ctx.getConversations();
                    String rewriteQuery = ctx.getRewriteQuery();

                    return modelRepository.generateStreamAnswerAsync(rewriteQuery, null, chatState, conversations, sessionId, promptEntity);
                })
                .doOnNext(answerEntity -> {
                    if (!answerEntity.getIsInference()) {
                        answerAccumulator.append(answerEntity.getContent());
                    }
                })
                .doOnSubscribe(s -> log.info("🔥 [4] 답변 생성 스트림 시작"))
                .map(answerEntity -> StreamEvent.builder()
                        .id(answerEntity.getId())
                        .content(answerEntity.getContent())
                        .event(answerEntity.getIsInference() ? StreamConst.Event.INFERENCE : StreamConst.Event.ANSWER)
                        .build())
                .doOnComplete(() -> log.info("✅ answerStreamFlux 완료"));

        // 대화 이력 업데이트
        Mono<Void> chatHistoryMono = contextMono.flatMap(ctx -> {
            String answer = answerAccumulator.toString().trim();
            List<ConversationVO> conversations = ctx.getConversations();
            String rewriteQuery = ctx.getRewriteQuery();

            // 1. 대화 상태 요약
            Mono<Void> summaryMono = !DecisionDetectUtil.detect(query, answer)
                    ? Mono.empty()
                    : questionModuleService.summaryState(chatState, conversations, sessionId).flatMap(newChatState ->
                            Mono.fromRunnable(() -> chatHistoryModuleService.updateChatState(chatId, newChatState)));

            Mono<Void> saveMono = Mono.fromRunnable(() -> chatHistoryModuleService.updateChatDetail(
                    chatId,
                    chatDetailEntity.getMsgId(),
                    rewriteQuery,
                    answer,
                    Collections.emptyList()
            ));

            return Mono.when(summaryMono, saveMono).subscribeOn(Schedulers.boundedElastic());
        });

        Flux<StreamEvent> answerStream = answerFlux.concatWith(chatHistoryMono.then(Mono.empty()));

        return QuestionVO.builder()
                .answerStream(answerStream)
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
                .build();

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .rewriteQuery(query)
                .build());

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();

        // 답변 Flux
        Flux<StreamEvent> answerStream = modelRepository.generateStreamAnswerAsync(query, context, null, Collections.emptyList(), sessionId, promptEntity)
                .doOnNext(answerEntity -> {
                    if (!answerEntity.getIsInference()) {
                        answerAccumulator.append(answerEntity.getContent());
                    }
                })
                .doOnComplete(() -> {
                    // 대화 이력 업데이트
                    String answer = answerAccumulator.toString().trim();
                    chatHistoryModuleService.updateChatDetail(
                            chatId,
                            chatDetailEntity.getMsgId(),
                            "",
                            answer,
                            Collections.emptyList()
                    );
                })
                .map(answerEntity -> StreamEvent.builder()
                        .id(answerEntity.getId())
                        .content(answerEntity.getContent())
                        .event(answerEntity.getIsInference() ? StreamConst.Event.INFERENCE : StreamConst.Event.ANSWER)
                        .build());

        return QuestionVO.builder()
                .answerStream(answerStream)
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}
