package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.client.LlmClient;
import com.genai.core.config.properties.LlmProperty;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.request.OpenAIAnswerRequest;
import com.genai.core.repository.response.OpenAIAnswerResponse;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.utils.TokenCalculateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAIModelRepositoryImpl implements ModelRepository {

    private final LlmClient llmClient;
    private final TokenCalculateUtil tokenCalculateUtil;
    private final LlmProperty llmProperty;
    private final ObjectMapper objectMapper;

    /**
     * 답변 생성 요청
     *
     * @param query        질의문
     * @param context      검색 결과 데이터
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 응답 문자열
     */
    @Override
    public String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity) {

        StringBuilder answerBuilder = new StringBuilder();
        this.generateAnswerSync(query, context, null, Collections.emptyList(), sessionId, promptEntity).forEach(answerEntity -> {
            if (!answerEntity.getIsInference()) {
                answerBuilder.append(answerEntity.getContent());
            }
        });

        return answerBuilder.toString().trim();
    }

    /**
     * 답변 생성 요청
     *
     * @param query        질의문
     * @param context      검색 결과 데이터
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 응답
     */
    @Override
    public List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {

        int maxTokens = tokenCalculateUtil.calculateMaxTokens(promptEntity.getPromptContent(), query, chatState, conversations, context);

        OpenAIAnswerRequest requestBody = OpenAIAnswerRequest.builder()
                .modelName(llmProperty.getModelName())
                .temperature(promptEntity.getTemperature())
                .topP(promptEntity.getTopP())
                .maxTokens(maxTokens)
                .stream(false)
                .prompt(promptEntity.getPromptContent())
                .chatState(chatState)
                .conversations(conversations)
                .context(context)
                .query(query)
                .build();

        try {
            String responseBodyJson = llmClient.generateAnswerSync(requestBody);

            OpenAIAnswerResponse responseBody = objectMapper.readValue(responseBodyJson, OpenAIAnswerResponse.class);

            List<AnswerEntity> answerEntities = new ArrayList<>();

            responseBody.getChoices().forEach(choice -> {
                String id = responseBody.getId();
                answerEntities.add(new AnswerEntity(id, choice.getData().getReasoningContent(), choice.getFinishReason(), true));
                answerEntities.add(new AnswerEntity(id, choice.getData().getContent(), choice.getFinishReason(), false));
            });

            return answerEntities;

        } catch (JsonProcessingException ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {

        int maxTokens = tokenCalculateUtil.calculateMaxTokens(promptEntity.getPromptContent(), query, chatState, conversations, context);

        OpenAIAnswerRequest requestBody = OpenAIAnswerRequest.builder()
                .modelName(llmProperty.getModelName())
                .temperature(promptEntity.getTemperature())
                .topP(promptEntity.getTopP())
                .maxTokens(maxTokens)
                .stream(false)
                .prompt(promptEntity.getPromptContent())
                .chatState(chatState)
                .conversations(conversations)
                .context(context)
                .query(query)
                .build();

            return llmClient.generateAnswerAsync(requestBody)
                    .map(responseBodyJson -> {
                        try {
                            List<AnswerEntity> answerEntities = new ArrayList<>();
                            OpenAIAnswerResponse responseBody = objectMapper.readValue(responseBodyJson, OpenAIAnswerResponse.class);

                            responseBody.getChoices().forEach(choice -> {
                                String id = responseBody.getId();
                                answerEntities.add(new AnswerEntity(id, choice.getData().getReasoningContent(), choice.getFinishReason(), true));
                                answerEntities.add(new AnswerEntity(id, choice.getData().getContent(), choice.getFinishReason(), false));
                            });

                            return answerEntities;

                        } catch (JsonProcessingException ignored) {
                            return Collections.emptyList();
                        }
                    });
    }

    /**
     * 답변 실시간 생성 요청
     *
     * @param query        질의문
     * @param context      검색 결과 데이터
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 Flux
     */
    @Override
    public Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {

        int maxTokens = tokenCalculateUtil.calculateMaxTokens(promptEntity.getPromptContent(), query, chatState, conversations, context);

        OpenAIAnswerRequest requestBody = OpenAIAnswerRequest.builder()
                .modelName(llmProperty.getModelName())
                .temperature(promptEntity.getTemperature())
                .topP(promptEntity.getTopP())
                .maxTokens(maxTokens)
                .stream(true)
                .prompt(promptEntity.getPromptContent())
                .chatState(chatState)
                .conversations(conversations)
                .context(context)
                .query(query)
                .build();

        return llmClient.generateStreamAnswerAsync(requestBody)
                .flatMapIterable(responseBodyJson -> {
                    try {
                        OpenAIAnswerResponse responseBody = objectMapper.readValue(responseBodyJson, OpenAIAnswerResponse.class);

                        return responseBody.getChoices().stream()
                                .filter(choice -> {
                                    if (choice.getData().getContent() != null) {
                                        return !choice.getData().getContent().isEmpty();
                                    } else if (choice.getData().getReasoningContent() != null) {
                                        return !choice.getData().getReasoningContent().isEmpty();
                                    } else return false;
                                })
                                .map(choice -> {
                                    String id = responseBody.getId();

                                    if (choice.getData().getReasoningContent() != null) {
                                        return new AnswerEntity(id, choice.getData().getReasoningContent(), choice.getFinishReason(), true);
                                    } else {
                                        return new AnswerEntity(id, choice.getData().getContent(), choice.getFinishReason(), false);
                                    }
                                })
                                .toList();

                    } catch (JsonProcessingException ignored) {
                        return Collections.emptyList();
                    }
                });
    }
}