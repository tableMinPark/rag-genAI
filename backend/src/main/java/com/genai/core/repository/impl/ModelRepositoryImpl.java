package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.common.utils.StringUtil;
import com.genai.core.config.instance.LlmInstance;
import com.genai.core.config.properties.LlmInstanceProperty;
import com.genai.core.config.properties.LlmRetryProperty;
import com.genai.core.exception.ModelErrorException;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.response.AnswerResponse;
import com.genai.core.service.module.vo.ConversationVO;
import com.genai.core.type.LlmPlatformType;
import com.genai.core.type.LlmType;
import com.genai.core.utils.ReactiveLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelRepositoryImpl implements ModelRepository {

    private final LlmRetryProperty llmRetryProperty;
    private final Map<LlmType, List<LlmInstance>> llmInstanceMap;
    private final ObjectMapper objectMapper;

    /**
     * LLM Instance 획득
     *
     * @param llmType LLM 타입
     * @return LLM Instance
     */
    private Mono<LlmInstance> acquireInstanceAsync(String requestId, LlmType llmType) {
        return Mono.fromCallable(() -> {
                    LlmType finalLlmType = (llmInstanceMap.get(llmType) == null || llmInstanceMap.get(llmType).isEmpty())
                            ? LlmType.DEFAULT
                            : llmType;

                    // LLM Instance 목록
                    List<LlmInstance> instances = new ArrayList<>(llmInstanceMap.get(finalLlmType));

                    // 랜덤 섞기
                    Collections.shuffle(instances);

                    for (LlmInstance instance : instances) {
                        Optional<Integer> sessionCountOptional = instance.tryAcquire(requestId);

                        if (sessionCountOptional.isPresent()) {
                            return Optional.of(new LlmInstance.AcquireResult(instance, sessionCountOptional.get()));
                        }
                    }

                    return Optional.<LlmInstance.AcquireResult>empty();

                })
                .flatMap(Mono::justOrEmpty)
                .repeatWhenEmpty(repeat -> repeat.delayElements(Duration.ofMillis(llmRetryProperty.getDelayMs())))
                .timeout(Duration.ofMillis(llmRetryProperty.getTimeoutMs()),
                        Mono.error(new ModelErrorException("LLM Instance 획득 실패 (" + llmRetryProperty.getTimeoutMs() + "ms)")))
                .doOnEach(ReactiveLogUtil.debug(ReactiveLogUtil.Message.LLM_INSTANCE_TRY_ACQUIRE_MESSAGE, v -> new Object[]{
                        v.instance().getInstanceId(), v.sessionCount()
                }))
                .map(LlmInstance.AcquireResult::instance);
    }

    /**
     * LLM Instance 반납
     *
     * @param instance LLM Instance
     */
    private Mono<Void> releaseInstance(String requestId, LlmInstance instance) {
        return Mono.fromCallable(() -> instance.release(requestId))
                .doOnEach(ReactiveLogUtil.debug(ReactiveLogUtil.Message.LLM_INSTANCE_RELEASE_MESSAGE, v -> new Object[]{
                        instance.getInstanceId(), v
                }))
                .then();
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 목록
     */
    @Override
    public List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity) {
        return this.generateAnswerSync(query, context, chatState, conversations, promptEntity, LlmType.DEFAULT);
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 목록 Mono
     */
    @Override
    public Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity) {
        return this.generateAnswerAsync(query, context, chatState, conversations, promptEntity, LlmType.DEFAULT);
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 Flux
     */
    @Override
    public Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity) {
        return this.generateStreamAnswerAsync(query, context, chatState, conversations, promptEntity, LlmType.DEFAULT);
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 목록
     */
    @Override
    public List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity, LlmType llmType) {
        return this.generateAnswerAsync(query, context, chatState, conversations, promptEntity, llmType)
                .block();
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 목록 Mono
     */
    @Override
    public Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity, LlmType llmType) {
        String requestId = StringUtil.generateRandomId();
        return Mono.usingWhen(
                acquireInstanceAsync(requestId, llmType),
                instance -> executeAsyncRequest(instance, query, context, chatState, conversations, promptEntity, requestId),
                instance -> releaseInstance(requestId, instance)
        );
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 Flux
     */
    @Override
    public Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity, LlmType llmType) {
        String requestId = StringUtil.generateRandomId();
        return Flux.usingWhen(
                acquireInstanceAsync(requestId, llmType),
                instance -> executeStreamRequest(instance, query, context, chatState, conversations, promptEntity, requestId),
                instance -> releaseInstance(requestId, instance)
        );
    }

    /**
     * 답변 생성 요청 (코어)
     *
     * @param instance      LLM Instance
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 목록 Mono
     */
    private Mono<List<AnswerEntity>> executeAsyncRequest(LlmInstance instance, String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity, String requestId) {
        LlmPlatformType platformType = instance.getPlatformType();
        LlmInstanceProperty llmInstanceProperty = instance.getLlmInstanceProperty();

        Object request = platformType.request(
                llmInstanceProperty, promptEntity.getTemperature(), promptEntity.getTopP(), false,
                promptEntity.getPromptContent(), query, context, chatState, conversations);

        return Mono.just(request)
                .flatMap(requestBody -> instance.getWebClient().post()
                        .uri(llmInstanceProperty.getUrl())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + llmInstanceProperty.getApiKey())
                        .bodyValue(requestBody)
                        .retrieve() // exchangeToMono 대신 간결한 retrieve 사용
                        .onStatus(status -> !status.is2xxSuccessful(), response ->
                                Mono.error(new ModelErrorException("모델 API 요청 실패 (" + response.statusCode() + ")")))
                        .bodyToMono(String.class)
                        .map(json -> parseAnswerResponse(json, platformType, false))
                )
                .doOnEach(ReactiveLogUtil.debug(ReactiveLogUtil.Message.LLM_RESPONSE_BLOCKING_MESSAGE, v -> new Object[]{
                        instance.getInstanceId(), requestId, llmInstanceProperty.getUrl(), StringUtil.writeJson(request), StringUtil.writeJson(v)
                }));
    }

    /**
     * 답변 생성 요청 (코어)
     *
     * @param instance      LLM Instance
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 Flux
     */
    private Flux<AnswerEntity> executeStreamRequest(LlmInstance instance, String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity, String requestId) {
        LlmPlatformType platformType = instance.getPlatformType();
        LlmInstanceProperty llmInstanceProperty = instance.getLlmInstanceProperty();

        Object request = platformType.request(
                llmInstanceProperty, promptEntity.getTemperature(), promptEntity.getTopP(), true,
                promptEntity.getPromptContent(), query, context, chatState, conversations);

        Flux<AnswerEntity> answerEntityFlux = Mono.just(request)
                .flatMapMany(requestBody -> instance.getWebClient().post()
                        .uri(llmInstanceProperty.getUrl())
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + llmInstanceProperty.getApiKey())
                        .bodyValue(requestBody)
                        .retrieve()
                        .onStatus(status -> !status.is2xxSuccessful(), response ->
                                Mono.error(new ModelErrorException("모델 API 요청 실패 (" + response.statusCode() + ")")))
                        .bodyToFlux(String.class)
                        .mapNotNull(json -> json.replaceFirst("^data:", "").trim())
                        .filter(json -> !json.equals("[DONE]") && !json.isEmpty())
                        .flatMapIterable(json -> parseAnswerResponse(json, platformType, true))
                )
                .cache();

        // 스트림 로그 처리 Mono
        Mono<Void> responseMono = answerEntityFlux
                .collectList()
                .flatMap(answerEntities -> Mono.fromCallable(() -> {
                            if (answerEntities.isEmpty()) {
                                return answerEntities;
                            }
                            String id = answerEntities.getFirst().getId();
                            StringBuilder inferenceBuilder = new StringBuilder();
                            StringBuilder answerBuilder = new StringBuilder();
                            String finishReason = null;

                            for (AnswerEntity answerEntity : answerEntities) {
                                if (answerEntity.getIsInference()) {
                                    inferenceBuilder.append(answerEntity.getContent());
                                } else {
                                    answerBuilder.append(answerEntity.getContent());
                                }
                            }

                            return List.of(
                                    new AnswerEntity(id, inferenceBuilder.toString(), finishReason, true),
                                    new AnswerEntity(id, answerBuilder.toString(), finishReason, false)
                            );
                        })
                        .doOnEach(ReactiveLogUtil.debug(ReactiveLogUtil.Message.LLM_RESPONSE_STREAM_MESSAGE, v -> new Object[]{
                                instance.getInstanceId(), requestId, llmInstanceProperty.getUrl(), StringUtil.writeJson(request), StringUtil.writeJson(v)
                        })))
                .then();

        return answerEntityFlux
                .concatWith(responseMono.subscribeOn(Schedulers.boundedElastic()).then(Mono.empty()));
    }

    /**
     * 답변 응답 바디 파싱
     *
     * @param json         답변 응답 JSON 문자열
     * @param platformType 플랫폼 타입
     * @param isStream     스트림 여부
     * @return 답변 엔티티 목록
     */
    private List<AnswerEntity> parseAnswerResponse(String json, LlmPlatformType platformType, boolean isStream) {
        try {
            AnswerResponse responseBody = objectMapper.readValue(json, platformType.getResponseClass());
            List<AnswerEntity> entities = new ArrayList<>();
            String id = responseBody.getId();

            responseBody.getDatas().forEach(choice -> {
                boolean hasReasoning = choice.getReasoningContent() != null && !choice.getReasoningContent().isEmpty();
                boolean hasContent = choice.getContent() != null && !choice.getContent().isEmpty();

                if (hasReasoning) {
                    entities.add(new AnswerEntity(id, choice.getReasoningContent(), choice.getFinishReason(), true));
                }
                if (hasContent || (!isStream && !hasReasoning)) {
                    entities.add(new AnswerEntity(id, choice.getContent(), choice.getFinishReason(), false));
                }
            });
            return entities;
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}