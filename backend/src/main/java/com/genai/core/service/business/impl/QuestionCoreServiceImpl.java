package com.genai.core.service.business.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.common.enums.CoreLogMessage;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
import com.genai.core.service.business.QuestionCoreService;
import com.genai.core.service.business.constant.QuestionCoreConst;
import com.genai.core.service.business.vo.DocumentVO;
import com.genai.core.service.business.vo.QuestionContextVO;
import com.genai.core.service.business.vo.QuestionVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.QuestionModuleService;
import com.genai.core.service.module.vo.ConversationVO;
import com.genai.core.service.module.vo.MultiturnConversationVO;
import com.genai.core.type.CollectionType;
import com.genai.core.type.CollectionTypeFactory;
import com.genai.global.common.utils.ReactiveLogUtil;
import com.genai.global.common.utils.StringUtil;
import com.genai.global.stream.subscriber.StreamEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.regex.Pattern;

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
     * @param userId        사용자 ID
     * @param chatId        대화 ID
     * @param categoryCodes 카테고리 코드 목록
     * @return 답변 VO
     */
    @Override
    public QuestionVO questionAi(String query, String userId, long chatId, long promptId, List<String> categoryCodes) {
        return this.questionByCollectionId(query, userId, chatId, promptId, collectionTypeFactory.ai(), categoryCodes);
    }

    /**
     * AI 질문 & 답변
     *
     * @param query        질의문
     * @param userId       사용자 ID
     * @param chatId       대화 ID
     * @param categoryCode 카테고리 코드
     * @return 답변 VO
     */
    @Override
    public QuestionVO questionMyAi(String query, String userId, long chatId, long promptId, String categoryCode) {
        return this.questionByCollectionId(query, userId, chatId, promptId, collectionTypeFactory.myai(), List.of(categoryCode));
    }

    /**
     * 컬렉션 ID 기준 질문 & 답변
     *
     * @param query          질의문
     * @param userId         세션 ID
     * @param promptId       프롬프트 ID
     * @param collectionType 컬렉션
     * @param chatId         대화 ID
     * @param categoryCodes  검색 필터
     * @return 답변 VO
     */
    @Override
    public QuestionVO questionByCollectionId(String query, String userId, long chatId, long promptId, CollectionType collectionType, List<String> categoryCodes) {

        // 시스템 프롬 프트 조회
        PromptEntity promptEntity = promptRepository.findById(promptId)
                .orElseThrow(() -> new NotFoundException("프롬프트"))
                .setDeveloperPromptContent(QuestionCoreConst.INVALID_ANSWER_PROMPT);

        // 현재 대화 조회
        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 새로운 대화 등록
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        // 이전 대화 상세 내역
        Mono<List<ConversationVO>> conversationMono = questionModuleService.getConversations(chatId, QuestionCoreConst.MULTITURN_TURN_CONVERSATION_COUNT)
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.PREVIOUS_CONVERSATIONS_MESSAGE, v -> new Object[]{StringUtil.writeJson(v)}))
                .cache();

        // 질의 재정의
        Mono<String> rewriteQueryMono = conversationMono
                .flatMap(conversations -> questionModuleService.generateRewriteQuery(query, conversations))
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.REWRITE_QUERY_MESSAGE, v -> new Object[]{v}))
                .cache();

        // 이전 대화 필터링 (멀티턴 대화 상세 목록)
        Mono<MultiturnConversationVO> multiturnConversationMono = Mono.zip(conversationMono, rewriteQueryMono)
                .flatMap(tuple -> questionModuleService.validMultiturn(tuple.getT2(), chatEntity.getState(), tuple.getT1()))
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.MULTITURN_CONVERSATIONS_MESSAGE, v -> new Object[]{
                        v.isChangeTopic(), StringUtil.writeJson(v.getConversations())
                }))
                .cache();

        // 검색 Mono
        Mono<List<Rerank>> rerankFlux = rewriteQueryMono.flatMap(rewriteQuery ->
                        Mono.fromCallable(() -> {
                            // 검색 결과 목록 (key 를 통한 중복 제거)
                            Map<Long, Search<DocumentEntity>> searchEntityMap = new HashMap<>();

                            // 키워드 검색
                            List<Search<DocumentEntity>> keywordSearchEntities = searchRepository.keywordSearch(collectionType, rewriteQuery, QuestionCoreConst.KEYWORD_TOP_K, categoryCodes);
                            keywordSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));

                            // 벡터 검색
                            List<Search<DocumentEntity>> vectorSearchEntities = searchRepository.vectorSearch(collectionType, rewriteQuery, QuestionCoreConst.VECTOR_TOP_K, categoryCodes);
                            vectorSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));

                            // 키워드 검색 결과, 벡터 검색 결과 변환
                            List<Rerank> rerankEntities = searchRepository.rerank(rewriteQuery, searchEntityMap.values().stream()
                                    .filter(searchEntity -> searchEntity.getScore() >= QuestionCoreConst.SEARCH_SCORE_MIN)
                                    .map(searchEntity -> Rerank.builder()
                                            .document(searchEntity.getFields())
                                            .build())
                                    .toList());

                            // 상위 RERANK_TOP_K 개 추출
                            return rerankEntities.subList(0, Math.min(QuestionCoreConst.RERANK_TOP_K, rerankEntities.size()));

                        }).subscribeOn(Schedulers.boundedElastic()))
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.RERANK_MESSAGE, v -> new Object[]{
                        StringUtil.writeJson(v)
                }))
                .cache();

        // 컨텍스트 Mono
        Mono<QuestionContextVO> contextMono = Mono.zip(multiturnConversationMono, conversationMono, rewriteQueryMono, rerankFlux)
                .map(tuple -> QuestionContextVO.builder()
                        .conversations(tuple.getT2())
                        .multiturnConversations(tuple.getT1().getConversations())
                        .isChangeTopic(tuple.getT1().isChangeTopic())
                        .rewriteQuery(tuple.getT3())
                        .reranks(tuple.getT4())
                        .query(query)
                        .build())
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.QUESTION_CONTEXT_MESSAGE, v -> new Object[]{
                        v.isChangeTopic(), StringUtil.writeJson(v.getConversations()), StringUtil.writeJson(v.getMultiturnConversations()), v.getQuery(), v.getRewriteQuery(), StringUtil.writeJson(v.getReranks())
                }))
                .cache();

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();

        // 답변 Entity
        Flux<StreamEvent> answerFlux = contextMono.flatMapMany(ctx -> {
                    List<ConversationVO> multiturnConversations = ctx.getMultiturnConversations();
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
                    return modelRepository.generateStreamAnswerAsync(rewriteQuery, contextBuilder.toString().trim(), chatEntity.getState(), multiturnConversations, promptEntity);
                })
                .doOnNext(answerEntity -> {
                    if (!answerEntity.getIsInference()) {
                        answerAccumulator.append(answerEntity.getContent());
                    }
                })
                .map(answerEntity -> answerEntity.getIsInference()
                        ? StreamEvent.inference(answerEntity.getId(), answerEntity.getContent())
                        : StreamEvent.answer(answerEntity.getId(), answerEntity.getContent())
                );

        // 참고 문서 Mono
        Mono<StreamEvent> referenceMono = rerankFlux
                .map(reranks -> {
                    String answer = answerAccumulator.toString().trim();

                    StringBuilder referencePatternBuilder = new StringBuilder();
                    for (int index = 0; index < QuestionCoreConst.REFERENCE_VALID_PATTERN.size(); index++) {
                        String pattern = QuestionCoreConst.REFERENCE_VALID_PATTERN.get(index);
                        referencePatternBuilder.append("(").append(pattern).append(")");

                        if (index < QuestionCoreConst.REFERENCE_VALID_PATTERN.size() - 1) {
                            referencePatternBuilder.append("|");
                        }
                    }

                    Pattern referencePattern = Pattern.compile(referencePatternBuilder.toString());

                    return referencePattern.matcher(answer).find()
                            ? new ArrayList<DocumentVO>()
                            : reranks.stream()
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
                            .toList();
                })
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.REFERENCE_MESSAGE, v -> new Object[]{
                        StringUtil.writeJson(v)
                }))
                .map(reranks -> StreamEvent.reference(StringUtil.generateRandomId(), reranks));

        // 대화 이력 업데이트
        Mono<Void> chatHistoryMono = contextMono.flatMap(ctx -> {
            String answer = answerAccumulator.toString().trim();
            String rewriteQuery = ctx.getRewriteQuery();
            List<Rerank> rerankEntities = ctx.getReranks();

            // 대화 상세 이력 업데이트
            return Mono.fromRunnable(() -> {
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
                    })
                    .then()
                    .doOnEach(ReactiveLogUtil.info(CoreLogMessage.CHAT_HISTORY_SAVE_MESSAGE, v -> new Object[]{
                            chatId, chatDetailEntity.getMsgId(), query, rewriteQuery, answer
                    }));
        });

        // 대화 상태 요약
        Mono<Void> chatStateMono = contextMono.flatMap(ctx -> {
            String rewriteQuery = ctx.getRewriteQuery();
            boolean isChangeTopic = ctx.isChangeTopic();
            List<ConversationVO> conversations = ctx.getConversations();
            List<ConversationVO> multiturnConversations = ctx.getMultiturnConversations();

            // 대화 상태 요약 (대화 이력이 존재하고 멀티턴 대화 이력이 없는 경우)
            return multiturnConversations.isEmpty() || isChangeTopic
                    ? questionModuleService.generateChatState(rewriteQuery, conversations)
                    .doOnEach(ReactiveLogUtil.info(CoreLogMessage.CHAT_STATE_MESSAGE, v -> new Object[]{
                            chatId, v
                    }))
                    .flatMap(newChatState -> Mono.fromRunnable(() -> chatHistoryModuleService.updateChatState(chatId, newChatState)))
                    : Mono.empty();
        });

        // 스트림 Flux
        Flux<StreamEvent> streamFlux = Flux.concat(answerFlux, referenceMono)
                .concatWith(chatHistoryMono.then(Mono.empty()))
                .doOnCancel(() -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .doOnError(throwable -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .onErrorMap(throwable -> new RuntimeException("스트림 처리 중 예외 발생", throwable));

        return QuestionVO.builder()
                .streamFlux(streamFlux)
                .streamEndMono(chatStateMono)
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }

    /**
     * LLM 질문 & 답변
     *
     * @param query    질의문
     * @param userId   사용자 ID
     * @param chatId   대화 ID
     * @param promptId 프롬프트 ID
     * @return 답변 VO
     */
    @Override
    public QuestionVO questionLlm(String query, String userId, long chatId, long promptId) {

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

        // 이전 대화 상세 내역
        Mono<List<ConversationVO>> conversationMono = questionModuleService.getConversations(chatId, QuestionCoreConst.MULTITURN_TURN_CONVERSATION_COUNT)
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.PREVIOUS_CONVERSATIONS_MESSAGE, v -> new Object[]{StringUtil.writeJson(v)}))
                .cache();

        // 질의 재정의
        Mono<String> rewriteQueryMono = conversationMono
                .flatMap(conversations -> questionModuleService.generateRewriteQuery(query, conversations))
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.REWRITE_QUERY_MESSAGE, v -> new Object[]{v}))
                .cache();

        // 이전 대화 필터링 (멀티턴 대화 상세 목록)
        Mono<MultiturnConversationVO> multiturnConversationMono = Mono.zip(conversationMono, rewriteQueryMono)
                .flatMap(tuple -> questionModuleService.validMultiturn(tuple.getT2(), chatEntity.getState(), tuple.getT1()))
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.MULTITURN_CONVERSATIONS_MESSAGE, v -> new Object[]{
                        v.isChangeTopic(), StringUtil.writeJson(v.getConversations())
                }))
                .cache();

        // 컨텍스트 생성
        Mono<QuestionContextVO> contextMono = Mono.zip(multiturnConversationMono, conversationMono, rewriteQueryMono)
                .map(tuple -> QuestionContextVO.builder()
                        .conversations(tuple.getT2())
                        .multiturnConversations(tuple.getT1().getConversations())
                        .isChangeTopic(tuple.getT1().isChangeTopic())
                        .rewriteQuery(tuple.getT3())
                        .reranks(Collections.emptyList())
                        .query(query)
                        .build())
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.QUESTION_CONTEXT_MESSAGE, v -> new Object[]{
                        v.isChangeTopic(), StringUtil.writeJson(v.getConversations()), StringUtil.writeJson(v.getMultiturnConversations()), v.getQuery(), v.getRewriteQuery(), StringUtil.writeJson(v.getReranks())
                }))
                .cache();

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();

        // 답변 Entity
        Flux<StreamEvent> answerFlux = contextMono
                .flatMapMany(ctx -> {
                    List<ConversationVO> multiturnConversations = ctx.getMultiturnConversations();
                    String rewriteQuery = ctx.getRewriteQuery();

                    return modelRepository.generateStreamAnswerAsync(rewriteQuery, null, chatEntity.getState(), multiturnConversations, promptEntity);
                })
                .doOnNext(answerEntity -> {
                    if (!answerEntity.getIsInference()) {
                        answerAccumulator.append(answerEntity.getContent());
                    }
                })
                .map(answerEntity -> answerEntity.getIsInference()
                        ? StreamEvent.inference(answerEntity.getId(), answerEntity.getContent())
                        : StreamEvent.answer(answerEntity.getId(), answerEntity.getContent())
                );

        // 대화 이력 업데이트
        Mono<Void> chatHistoryMono = contextMono.flatMap(ctx -> {
            String answer = answerAccumulator.toString().trim();
            String rewriteQuery = ctx.getRewriteQuery();

            // 대화 상세 이력 업데이트
            return Mono.fromRunnable(() -> chatHistoryModuleService.updateChatDetail(
                            chatId,
                            chatDetailEntity.getMsgId(),
                            rewriteQuery,
                            answer,
                            Collections.emptyList()
                    ))
                    .then()
                    .doOnEach(ReactiveLogUtil.info(CoreLogMessage.CHAT_HISTORY_SAVE_MESSAGE, v -> new Object[]{
                            chatId, chatDetailEntity.getMsgId(), query, rewriteQuery, answer
                    }));
        });

        // 대화 상태 요약
        Mono<Void> chatStateMono = contextMono.flatMap(ctx -> {
            String rewriteQuery = ctx.getRewriteQuery();
            boolean isChangeTopic = ctx.isChangeTopic();
            List<ConversationVO> conversations = ctx.getConversations();
            List<ConversationVO> multiturnConversations = ctx.getMultiturnConversations();

            // 대화 상태 요약 (대화 이력이 존재하고 멀티턴 대화 이력이 없는 경우)
            return !multiturnConversations.isEmpty() && !isChangeTopic
                    ? Mono.empty()
                    : questionModuleService.generateChatState(rewriteQuery, conversations)
                    .doOnEach(ReactiveLogUtil.info(CoreLogMessage.CHAT_STATE_MESSAGE, v -> new Object[]{
                            chatId, v
                    }))
                    .flatMap(newChatState -> Mono.fromRunnable(() -> chatHistoryModuleService.updateChatState(chatId, newChatState)));
        });

        Flux<StreamEvent> streamFlux = answerFlux
                .concatWith(chatHistoryMono.then(Mono.empty()))
                .doOnCancel(() -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .doOnError(throwable -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .onErrorMap(throwable -> new RuntimeException("스트림 처리 중 예외 발생", throwable));

        return QuestionVO.builder()
                .streamFlux(streamFlux)
                .streamEndMono(chatStateMono)
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }

    /**
     * LLM Simulation 질문 & 답변
     *
     * @param query         질의문
     * @param userId        사용자 ID
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
    public QuestionVO questionSimulation(String query, String userId, long chatId, String context, String promptContent, double temperature, double topP, int maximumTokens) {

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
        Flux<StreamEvent> answerFlux = modelRepository.generateStreamAnswerAsync(query, context, null, Collections.emptyList(), promptEntity)
                .doOnNext(answerEntity -> {
                    if (!answerEntity.getIsInference()) {
                        answerAccumulator.append(answerEntity.getContent());
                    }
                })
                .map(answerEntity -> answerEntity.getIsInference()
                        ? StreamEvent.inference(answerEntity.getId(), answerEntity.getContent())
                        : StreamEvent.answer(answerEntity.getId(), answerEntity.getContent())
                );

        // 대화 이력 업데이트
        Mono<Void> chatHistoryMono = Mono.empty()
                .flatMap(o -> {
                    String answer = answerAccumulator.toString().trim();
                    String rewriteQuery = "";

                    return Mono.fromRunnable(() ->
                                    chatHistoryModuleService.updateChatDetail(
                                            chatId,
                                            chatDetailEntity.getMsgId(),
                                            rewriteQuery,
                                            answer,
                                            Collections.emptyList()
                                    ))
                            .then()
                            .doOnEach(ReactiveLogUtil.info(CoreLogMessage.CHAT_HISTORY_SAVE_MESSAGE, v -> new Object[]{
                                    chatId, chatDetailEntity.getMsgId(), query, rewriteQuery, answer
                            }));
                });

        Flux<StreamEvent> streamFlux = answerFlux
                .concatWith(chatHistoryMono.then(Mono.empty()))
                .doOnCancel(() -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .doOnError(throwable -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .onErrorMap(throwable -> new RuntimeException("스트림 처리 중 예외 발생", throwable));

        return QuestionVO.builder()
                .streamFlux(streamFlux)
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}
