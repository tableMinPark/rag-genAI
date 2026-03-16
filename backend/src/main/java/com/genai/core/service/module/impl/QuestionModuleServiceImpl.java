package com.genai.core.service.module.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.business.constant.QuestionCoreConst;
import com.genai.core.service.module.QuestionModuleService;
import com.genai.core.service.module.constant.QuestionModuleConst;
import com.genai.core.service.module.vo.ConversationVO;
import com.genai.core.service.module.vo.MultiturnConversationVO;
import com.genai.core.service.module.vo.ValidMultiturnVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionModuleServiceImpl implements QuestionModuleService {

    private final ChatDetailRepository chatDetailRepository;
    private final ModelRepository modelRepository;
    private final ObjectMapper objectMapper;

    /**
     * 대화 이력 목록 조회
     *
     * @param chatId 대화 ID
     * @param size   대화 수
     * @return 대화 이력 목록
     */
    @Override
    public Mono<List<ConversationVO>> getConversations(long chatId, int size) {

        Pageable pageable = PageRequest.of(0, size);

        return Mono.fromCallable(() ->  chatDetailRepository
                    .findByChatIdAndAnswerIsNotNullOrderBySysCreateDtDesc(chatId, pageable)
                    .stream()
                    .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                    .map(chatDetailEntity -> ConversationVO.builder()
                            .id(chatDetailEntity.getMsgId())
                            .query(chatDetailEntity.getRewriteQuery())
                            .answer(chatDetailEntity.getAnswer())
                            .build())
                    .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 질의 재작성
     *
     * @param query         질의
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 ID
     * @return 재작성 질의
     */
    @Override
    public Mono<String> rewriteQuery(String query, List<ConversationVO> conversations, String sessionId) {

        if (conversations.isEmpty()) {
            return Mono.just(query);
        }

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionModuleConst.REWRITE_QUERY_PROMPT)
                .temperature(QuestionModuleConst.REWRITE_QUERY_TEMPERATURE)
                .topP(QuestionModuleConst.REWRITE_QUERY_TOP_P)
                .build();

        return Mono.just(conversations)
                .flatMap(targetConversions -> modelRepository.generateAnswerAsync(query, null, null, targetConversions, sessionId, promptEntity))
                .map(answerEntities -> {
                    StringBuilder answerBuilder = new StringBuilder();
                    answerEntities.forEach(answerEntity -> {
                        if (!answerEntity.getIsInference()) {
                            answerBuilder.append(answerEntity.getContent());
                        }
                    });
                    return answerBuilder.toString().trim();
                })
                .map(rewriteQuery -> rewriteQuery.trim().isBlank() ? query : rewriteQuery);
    }

    /**
     * 대화 상태 요약 생성
     *
     * @param query         질의문
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 ID
     * @return 대화 상태 요약
     */
    @Override
    public Mono<String> summaryState(String query, List<ConversationVO> conversations, String sessionId) {

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionModuleConst.CHAT_STATE_UPDATE_PROMPT)
                .temperature(QuestionModuleConst.CHAT_STATE_UPDATE_TEMPERATURE)
                .topP(QuestionModuleConst.CHAT_STATE_UPDATE_TOP_P)
                .build();

        return modelRepository.generateAnswerAsync(query, null, null, conversations, sessionId, promptEntity)
                .map(answerEntities -> {
                    StringBuilder answerBuilder = new StringBuilder();
                    answerEntities.forEach(answerEntity -> {
                        if (!answerEntity.getIsInference()) {
                            answerBuilder.append(answerEntity.getContent());
                        }
                    });
                    return answerBuilder.toString().trim();
                });
    }

    /**
     * 멀티턴 참고 대화 이력 체크
     *
     * @param query         질의문
     * @param chatState     이전 대화 상태 (주제)
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 ID
     * @return 멀티턴 참고 대화 이력 목록
     */
    @Override
    public Mono<MultiturnConversationVO> validMultiturn(String query, String chatState, List<ConversationVO> conversations, String sessionId) {

        if (conversations.isEmpty()) {
            return Mono.just(MultiturnConversationVO.builder()
                    .isChangeTopic(false)
                    .conversations(Collections.emptyList())
                    .build());
        }

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionModuleConst.VALID_MULTITURN_PROMPT)
                .temperature(QuestionModuleConst.VALID_MULTITURN_TEMPERATURE)
                .topP(QuestionModuleConst.VALID_MULTITURN_TOP_P)
                .build();

        String context = "Current Query:\n" + query;

        return Mono.just(conversations)
                .flatMap(targetConversions -> modelRepository.generateAnswerAsync(null, context, chatState, targetConversions, sessionId, promptEntity))
                .map(answerEntities -> {
                    StringBuilder answerBuilder = new StringBuilder();
                    answerEntities.forEach(answerEntity -> {
                        if (!answerEntity.getIsInference()) {
                            answerBuilder.append(answerEntity.getContent());
                        }
                    });
                    return answerBuilder.toString().trim();
                })
                .map(answer -> {
                    try {
                        ValidMultiturnVO validMultiturn = objectMapper.readValue(answer, ValidMultiturnVO.class);

                        if (validMultiturn.getConversationIds() != null) {
                            List<ConversationVO> multiturnConversations = conversations.stream()
                                    .filter(conversationVO -> validMultiturn.getConversationIds().contains(conversationVO.getId()))
                                    .toList();

                            return MultiturnConversationVO.builder()
                                    .isChangeTopic(validMultiturn.isChangeTopic())
                                    .conversations(multiturnConversations.subList(Math.max(0, multiturnConversations.size() - QuestionCoreConst.MULTITURN_TURNS), multiturnConversations.size()))
                                    .build();
                        } else {
                            return MultiturnConversationVO.builder()
                                    .isChangeTopic(false)
                                    .conversations(Collections.emptyList())
                                    .build();
                        }

                    } catch (JsonProcessingException e) {
                        return MultiturnConversationVO.builder()
                                .isChangeTopic(false)
                                .conversations(Collections.emptyList())
                                .build();
                    }
                });
    }
}
