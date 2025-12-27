package com.genai.core.service.impl;

import com.genai.core.config.constant.QuestionConst;
import com.genai.core.config.constant.SearchConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
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
import reactor.core.publisher.Flux;
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
    private final ChatPassageRepository chatPassageRepository;
    private final CollectionTypeFactory collectionTypeFactory;

    /**
     * 질문 재작성
     *
     * @param query     질문
     * @param sessionId 세션 ID
     * @return 재작성 질문
     */
    @Transactional
    public String rewriteQuery(String query, long chatId, String sessionId) {

        // 이전 대화 목록 조회
        List<ChatDetailEntity> chatDetailEntities = chatDetailRepository.findByChatIdOrderBySysCreateDtDesc(chatId, PageRequest.of(0, QuestionConst.REWRITE_QUERY_TURNS)).stream()
                .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                .toList();

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionConst.REWRITE_QUERY_PROMPT)
                .temperature(QuestionConst.REWRITE_QUERY_TEMPERATURE)
                .topP(QuestionConst.REWRITE_QUERY_TOP_P)
                .maximumTokens(QuestionConst.REWRITE_QUERY_MAXIMUM_TOKENS)
                .build();

        String rewriteQuery = query;
        if (!chatDetailEntities.isEmpty()) {
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
     * @param sessionId        세션 ID
     * @return 요약 답변 문자열
     */
    @Transactional
    public String summaryState(String chatState, long chatId, String sessionId) {

        // 이전 대화 목록 조회
        List<ChatDetailEntity> chatDetailEntities = chatDetailRepository.findByChatIdOrderBySysCreateDtDesc(chatId, PageRequest.of(0, QuestionConst.SUMMARY_UPDATE_TURNS)).stream()
                .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                .toList();

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionConst.SUMMARY_UPDATE_PROMPT)
                .temperature(QuestionConst.SUMMARY_UPDATE_TEMPERATURE)
                .topP(QuestionConst.SUMMARY_UPDATE_TOP_P)
                .maximumTokens(QuestionConst.SUMMARY_UPDATE_MAXIMUM_TOKENS)
                .build();

        List<ConversationVO> conversationVos = chatDetailEntities.stream()
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
        String chatState = chatEntity.getState();;
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
        // LLM 답변 스트림 요청 (Hot Stream)
        Flux<List<AnswerVO>> answerStream = modelRepository
                .generateStreamAnswer(rewriteQuery, contextBuilder.toString().trim(), chatState, conversations, sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.getIsInference())
                                .build())
                        .toList())
                // TODO: 학 스트림 구독자 제한 기능 작동 안함
                // TODO: 최소 2명으로 설정 했으나, 1명이 cancel 되어도 기존 구독은 계속 진행 (upstream 중지 안됨)
                .publish()
                .refCount(2);

        // ####################################
        // 답변 이전 처리
        // ####################################
        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .rewriteQuery(rewriteQuery)
                .answer("")
                .build());

        // ####################################
        // 답변 이후 처리
        // ####################################
        StringBuilder answerBuilder = new StringBuilder();
        answerStream
                .onErrorContinue((throwable, o) -> log.error("대화 이력 스트림 실시간 예외 발생 : {}", throwable.getMessage()))
                .subscribe(answerVos -> {
                    for (AnswerVO answerVo : answerVos) {
                        if (!answerVo.getIsInference()) {
                            answerBuilder.append(answerVo.getContent());
                        }
                    }
                }, throwable -> log.error("대화 이력 스트림 비정상 종료 : {}", throwable.getMessage()), () -> {
                    String answer = answerBuilder.toString().trim();
                    // 답변 등록
                    chatDetailEntity.setAnswer(answer);
                    // 대화 이력 저장
                    chatDetailRepository.save(chatDetailEntity);
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
                    // 참고 문서 등록
                    chatPassageRepository.saveAll(chatPassageEntities);

                    if (DecisionDetectUtil.detect(query, answer)) {
                        String newChatState = this.summaryState(chatState, chatId, sessionId);
                        if (!newChatState.isBlank()) {
                            chatEntity.setState(newChatState);
                            chatRepository.save(chatEntity);
                        }
                    }
                    log.debug("대화 이력 스트림 정상 종료");
                });

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
        Flux<List<AnswerVO>> answerStream = modelRepository
                .generateStreamAnswer(rewriteQuery, chatState, conversations, sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.getIsInference())
                                .build())
                        .toList())
                .publish()
                .refCount(2);

        StringBuilder answerBuilder = new StringBuilder();
        answerStream
                .onErrorContinue((throwable, o) -> log.error("대화 이력 스트림 실시간 예외 발생 : {}", throwable.getMessage()))
                .publishOn(Schedulers.boundedElastic())
                .subscribe(answerVos -> {
                    for (AnswerVO answerVo : answerVos) {
                        if (!answerVo.getIsInference()) {
                            answerBuilder.append(answerVo.getContent());
                        }
                    }
                }, throwable -> log.error("대화 이력 스트림 비정상 종료 : {}", throwable.getMessage()), () -> {
                    String answer = answerBuilder.toString().trim();

                    // 답변 등록
                    chatDetailEntity.setAnswer(answer);
                    // 대화 이력 저장
                    chatDetailRepository.save(chatDetailEntity);

                    if (DecisionDetectUtil.detect(query, answer)) {
                        String newChatState = this.summaryState(chatState, chatId, sessionId);
                        if (!newChatState.isBlank()) {
                            chatEntity.setState(newChatState);
                            chatRepository.save(chatEntity);
                        }
                    }
                    log.debug("대화 이력 스트림 정상 종료");
                });

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
        Flux<List<AnswerVO>> answerStream = modelRepository
                .generateStreamAnswer(query, context, null, Collections.emptyList(), sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.getIsInference())
                                .build())
                        .toList())
                .publish()
                .refCount(2);

        StringBuilder answerBuilder = new StringBuilder();
        answerStream
                .onErrorContinue((throwable, o) -> log.error("대화 이력 스트림 실시간 예외 발생 : {}", throwable.getMessage()))
                .subscribe(answerVos -> {
                    for (AnswerVO answerVo : answerVos) {
                        if (!answerVo.getIsInference()) {
                            answerBuilder.append(answerVo.getContent());
                        }
                    }
                }, throwable -> log.error("대화 이력 스트림 비정상 종료 : {}", throwable.getMessage()), () -> {
                    // 답변 저장
                    String answer = answerBuilder.toString().trim();
                    chatDetailEntity.setAnswer(answer);
                    chatDetailRepository.save(chatDetailEntity);
                    log.debug("대화 이력 스트림 정상 종료");
                });

        return QuestionVO.builder()
                .answerStream(answerStream)
                .documents(Collections.emptyList())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}
