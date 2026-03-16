package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    private Mono<LlmInstance> acquireInstanceAsync(LlmType llmType) {
        return Mono.fromCallable(() -> {
                    LlmType finalLlmType = (llmInstanceMap.get(llmType) == null || llmInstanceMap.get(llmType).isEmpty())
                            ? LlmType.DEFAULT
                            : llmType;

                    return llmInstanceMap.get(finalLlmType).stream()
                            .filter(LlmInstance::tryAcquire)
                            .findAny();

                })
                .flatMap(Mono::justOrEmpty)
                .repeatWhenEmpty(repeat -> repeat.delayElements(Duration.ofMillis(llmRetryProperty.getDelayMs())))
                .timeout(Duration.ofMillis(llmRetryProperty.getTimeoutMs()), Mono.error(new ModelErrorException("LLM(" + llmRetryProperty.getTimeoutMs() + "ms) get llm instance max count reached.")));
    }

    /**
     * LLM Instance 반납
     *
     * @param instance LLM Instance
     */
    private Mono<Void> releaseInstance(LlmInstance instance) {
        return Mono.fromRunnable(() -> {
            instance.release();
            log.debug("[{}] Session released. Remaining capacity: {}", instance.getInstanceId(), instance.getSessionCount().get());
        });
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
    public String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity) {
        return this.generateAnswerSyncStr(query, context, sessionId, promptEntity, LlmType.DEFAULT);
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 목록
     */
    @Override
    public List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {
        return this.generateAnswerSync(query, context, chatState, conversations, sessionId, promptEntity, LlmType.DEFAULT);
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 목록 Mono
     */
    @Override
    public Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {
        return this.generateAnswerAsync(query, context, chatState, conversations, sessionId, promptEntity, LlmType.DEFAULT);
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 Flux
     */
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
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 목록
     */
    @Override
    public List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType) {
        return this.generateAnswerAsync(query, context, chatState, conversations, sessionId, promptEntity, llmType)
                .block();
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 목록 Mono
     */
    @Override
    public Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType) {
        return Mono.usingWhen(
                acquireInstanceAsync(llmType),
                instance -> executeAsyncRequest(instance, query, context, chatState, conversations, promptEntity),
                this::releaseInstance
        );
    }

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 Flux
     */
    @Override
    public Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType) {
        // [핵심] usingWhen: 획득 -> 스트림 통신 -> (스트림 종료/에러 무관) 반납 보장
        return Flux.usingWhen(
                acquireInstanceAsync(llmType),
                instance -> executeStreamRequest(instance, query, context, chatState, conversations, promptEntity),
                this::releaseInstance
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
    private Mono<List<AnswerEntity>> executeAsyncRequest(LlmInstance instance, String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity) {
        LlmPlatformType platformType = instance.getPlatformType();
        LlmInstanceProperty llmInstanceProperty = instance.getLlmInstanceProperty();

        Object requestBody = platformType.request(
                llmInstanceProperty, promptEntity.getTemperature(), promptEntity.getTopP(), false,
                promptEntity.getPromptContent(), query, context, chatState, conversations);

        String requestBodyJson = "";

        try {
            requestBodyJson = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException ignored) {
        }

        log.info("[{}] LLM Request(Async) to {} | {}:{}{}\n{}", instance.getInstanceId(), platformType.name(), llmInstanceProperty.getHost(), llmInstanceProperty.getPort(), llmInstanceProperty.getPath(), requestBodyJson);

        return instance.getWebClient().post()
                .uri(llmInstanceProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + llmInstanceProperty.getApiKey())
                .bodyValue(requestBody)
                .retrieve() // exchangeToMono 대신 간결한 retrieve 사용
                .onStatus(status -> !status.is2xxSuccessful(), response ->
                        Mono.error(new ModelErrorException("LLM(" + llmInstanceProperty.getModelName() + ") API Error")))
                .bodyToMono(String.class)
                .map(json -> parseAnswerResponse(json, platformType, false))
                .onErrorReturn(Collections.emptyList());
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
    private Flux<AnswerEntity> executeStreamRequest(LlmInstance instance, String query, String context, String chatState, List<ConversationVO> conversations, PromptEntity promptEntity) {
        LlmPlatformType platformType = instance.getPlatformType();
        LlmInstanceProperty llmInstanceProperty = instance.getLlmInstanceProperty();

        Object requestBody = platformType.request(
                llmInstanceProperty, promptEntity.getTemperature(), promptEntity.getTopP(), true,
                promptEntity.getPromptContent(), query, context, chatState, conversations);

        String requestBodyJson = "";

        try {
            requestBodyJson = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException ignored) {
        }

        log.info("[{}] LLM Request(Stream) to {} | {}:{}{}\n{}", instance.getInstanceId(), platformType.name(), llmInstanceProperty.getHost(), llmInstanceProperty.getPort(), llmInstanceProperty.getPath(), requestBodyJson);

        return instance.getWebClient().post()
                .uri(llmInstanceProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + llmInstanceProperty.getApiKey())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .mapNotNull(json -> json.replaceFirst("^data:", "").trim())
                .filter(json -> !json.equals("[DONE]") && !json.isEmpty())
                .flatMapIterable(json -> parseAnswerResponse(json, platformType, true))
                .onErrorResume(e -> Flux.empty());
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
            log.error("LLM Response JSON Parsing Error", e);
            return Collections.emptyList();
        }
    }
}