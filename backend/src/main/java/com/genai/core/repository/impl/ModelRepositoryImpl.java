package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.config.instance.LlmInstance;
import com.genai.core.config.properties.LlmProperty;
import com.genai.core.exception.ModelErrorException;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.response.AnswerResponse;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.type.LlmPlatformType;
import com.genai.core.type.LlmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRepositoryImpl implements ModelRepository {
;
    private final Map<LlmType, List<LlmInstance>> llmInstanceMap;
    private final ObjectMapper objectMapper;
    private final Map<LlmType, AtomicInteger> llmInstanceCounterMap = Arrays.stream(LlmType.values())
            .map(llmType -> new AbstractMap.SimpleEntry<>(llmType, new AtomicInteger(0)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private LlmInstance getInstance(LlmType llmType) {

        List<LlmInstance> instances = llmInstanceMap.get(llmType);
        if (instances == null || instances.isEmpty()) {
            return getInstance(LlmType.DEFAULT);
        } else {
            int instanceIndex = llmInstanceCounterMap.get(llmType).getAndUpdate(current -> (current + 1) % instances.size());
            return instances.get(instanceIndex);
        }
    }

    @Override
    public String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity) {
        return this.generateAnswerSyncStr(query, context, sessionId, promptEntity, LlmType.DEFAULT);
    }

    @Override
    public List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {
        return this.generateAnswerSync(query, context, chatState, conversations, sessionId, promptEntity, LlmType.DEFAULT);
    }

    @Override
    public Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {
        return this.generateAnswerAsync(query, context, chatState, conversations, sessionId, promptEntity, LlmType.DEFAULT);
    }

    @Override
    public Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {
        return this.generateStreamAnswerAsync(query, context, chatState, conversations, sessionId, promptEntity, LlmType.DEFAULT);
    }

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
    public String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity, LlmType llmType) {

        StringBuilder answerBuilder = new StringBuilder();
        this.generateAnswerSync(query, context, null, Collections.emptyList(), sessionId, promptEntity, llmType).forEach(answerEntity -> {
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
    public List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType) {

        LlmInstance instance = getInstance(llmType);
        LlmPlatformType platformType = instance.getPlatformType();
        LlmProperty llmProperty = instance.getLlmProperty();
        WebClient webClient = instance.getWebClient();

        Object requestBody = platformType.request(
                llmProperty, promptEntity.getTemperature(), promptEntity.getTopP(), false,
                promptEntity.getPromptContent(), query, context, chatState, conversations);

        log.info("[{}] LLM Request to {} | {}:{}/{}", instance.getInstanceId(), platformType.name(), llmProperty.getHost(), llmProperty.getPort(), llmProperty.getPath());

        try {
            ResponseEntity<String> responseEntity = webClient.post()
                    .uri(llmProperty.getUrl())
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + llmProperty.getApiKey())
                    .bodyValue(requestBody)
                    .exchangeToMono(response -> response
                            .bodyToMono(String.class)
                            .map(body -> new ResponseEntity<>(body, response.statusCode())))
                    .block();

            if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                throw new ModelErrorException("LLM(" + llmProperty.getModelName() + ")");
            }

            AnswerResponse responseBody = objectMapper.readValue(responseEntity.getBody(), platformType.getResponseClass());

            List<AnswerEntity> answerEntities = new ArrayList<>();

            responseBody.getDatas().forEach(choice -> {
                String id = responseBody.getId();
                answerEntities.add(new AnswerEntity(id, choice.getReasoningContent(), choice.getFinishReason(), true));
                answerEntities.add(new AnswerEntity(id, choice.getContent(), choice.getFinishReason(), false));
            });

            return answerEntities;

        } catch (JsonProcessingException ignored) {
            return Collections.emptyList();
        }
    }

    @Override
    public Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType) {

        LlmInstance instance = getInstance(llmType);
        LlmPlatformType platformType = instance.getPlatformType();
        LlmProperty llmProperty = instance.getLlmProperty();
        WebClient webClient = instance.getWebClient();

        Object requestBody = platformType.request(
                llmProperty, promptEntity.getTemperature(), promptEntity.getTopP(), false,
                promptEntity.getPromptContent(), query, context, chatState, conversations);

        log.info("[{}] LLM Request to {} | {}:{}/{}", instance.getInstanceId(), platformType.name(), llmProperty.getHost(), llmProperty.getPort(), llmProperty.getPath());

        return webClient.post()
                .uri(llmProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + llmProperty.getApiKey())
                .bodyValue(requestBody)
                .exchangeToMono(response -> response
                        .bodyToMono(String.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .handle((responseEntity, sink) -> {
                    if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                        sink.error(new ModelErrorException("LLM(" + llmProperty.getModelName() + ") | " + responseEntity));
                    } else {

                        try {
                            List<AnswerEntity> answerEntities = new ArrayList<>();
                            AnswerResponse responseBody = objectMapper.readValue(responseEntity.getBody(), platformType.getResponseClass());

                            responseBody.getDatas().forEach(choice -> {
                                String id = responseBody.getId();
                                answerEntities.add(new AnswerEntity(id, choice.getReasoningContent(), choice.getFinishReason(), true));
                                answerEntities.add(new AnswerEntity(id, choice.getContent(), choice.getFinishReason(), false));
                            });

                            sink.next(answerEntities);

                        } catch (JsonProcessingException ignored) {
                            sink.next(Collections.emptyList());
                        }
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
    public Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType) {

        LlmInstance instance = getInstance(llmType);
        LlmPlatformType platformType = instance.getPlatformType();
        LlmProperty llmProperty = instance.getLlmProperty();
        WebClient webClient = instance.getWebClient();

        Object requestBody = platformType.request(
                llmProperty, promptEntity.getTemperature(), promptEntity.getTopP(), true,
                promptEntity.getPromptContent(), query, context, chatState, conversations);

        log.info("[{}] LLM Request to {} | {}:{}/{}", instance.getInstanceId(), platformType.name(), llmProperty.getHost(), llmProperty.getPort(), llmProperty.getPath());

        return webClient.post()
                .uri(llmProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + llmProperty.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .mapNotNull(json -> json.replaceFirst("^data:", "").trim())
                .filter(json -> !json.equals("[DONE]"))
                .filter(json -> !json.isEmpty())
                .flatMapIterable(responseBodyJson -> {
                    try {
                        AnswerResponse responseBody = objectMapper.readValue(responseBodyJson, platformType.getResponseClass());

                        return responseBody.getDatas().stream()
                                .filter(choice -> {
                                    if (choice.getContent() != null) {
                                        return !choice.getContent().isEmpty();
                                    } else if (choice.getReasoningContent() != null) {
                                        return !choice.getReasoningContent().isEmpty();
                                    } else return false;
                                })
                                .map(choice -> {
                                    String id = responseBody.getId();

                                    if (choice.getReasoningContent() != null) {
                                        return new AnswerEntity(id, choice.getReasoningContent(), choice.getFinishReason(), true);
                                    } else {
                                        return new AnswerEntity(id, choice.getContent(), choice.getFinishReason(), false);
                                    }
                                })
                                .toList();

                    } catch (JsonProcessingException ignored) {
                        return Collections.emptyList();
                    }
                });
    }
}