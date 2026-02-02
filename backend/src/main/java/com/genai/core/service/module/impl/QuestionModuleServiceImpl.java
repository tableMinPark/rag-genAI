package com.genai.core.service.module.impl;

import com.genai.core.constant.QuestionConst;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.service.module.QuestionModuleService;
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

    @Override
    public Flux<ConversationVO> getConversations(long chatId) {

        Pageable pageable = PageRequest.of(0, QuestionConst.REWRITE_QUERY_TURNS);

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

    @Override
    public Mono<String> rewriteQuery(String query, List<ConversationVO> conversations, String sessionId) {

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionConst.REWRITE_QUERY_PROMPT)
                .temperature(QuestionConst.REWRITE_QUERY_TEMPERATURE)
                .topP(QuestionConst.REWRITE_QUERY_TOP_P)
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

    @Override
    public Mono<String> summaryState(String chatState, List<ConversationVO> conversations, String sessionId) {

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(QuestionConst.SUMMARY_UPDATE_PROMPT)
                .temperature(QuestionConst.SUMMARY_UPDATE_TEMPERATURE)
                .topP(QuestionConst.SUMMARY_UPDATE_TOP_P)
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
