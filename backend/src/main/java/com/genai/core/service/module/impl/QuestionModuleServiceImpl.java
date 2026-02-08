package com.genai.core.service.module.impl;

import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.service.module.QuestionModuleService;
import com.genai.core.service.module.constant.QuestionModuleConst;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionModuleServiceImpl implements QuestionModuleService {

    private final ChatDetailRepository chatDetailRepository;
    private final ModelRepository modelRepository;

    /**
     * 대화 이력 목록 조회
     *
     * @param chatId 대화 ID
     * @param size   대화 수
     * @return 대화 이력 목록
     */
    @Override
    public Flux<ConversationVO> getConversations(long chatId, int size) {

        Pageable pageable = PageRequest.of(0, size);

        return Mono.fromCallable(() ->
                        chatDetailRepository
                                .findByChatIdAndAnswerIsNotNullOrderBySysCreateDtDesc(chatId, pageable)
                                .stream()
                                .sorted(Comparator.comparing(ChatDetailEntity::getSysCreateDt))
                                .map(chatDetailEntity -> ConversationVO.builder()
                                        .query(chatDetailEntity.getRewriteQuery())
                                        .answer(chatDetailEntity.getAnswer())
                                        .build())
                                .toList()
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(Flux::fromIterable);
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
     * @param chatState     이전 대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 ID
     * @return 대화 상태 요약
     */
    @Override
    public Mono<String> summaryState(String chatState, List<ConversationVO> conversations, String sessionId) {

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionModuleConst.CHAT_STATE_UPDATE_PROMPT)
                .temperature(QuestionModuleConst.CHAT_STATE_UPDATE_TEMPERATURE)
                .topP(QuestionModuleConst.CHAT_STATE_UPDATE_TOP_P)
                .build();

        return modelRepository.generateAnswerAsync(null, null, null, conversations, sessionId, promptEntity)
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
}
