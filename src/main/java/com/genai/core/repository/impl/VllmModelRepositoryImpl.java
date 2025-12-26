package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.config.constant.ModelConst;
import com.genai.core.config.properties.LlmProperty;
import com.genai.core.exception.ModelErrorException;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.request.VllmAnswerRequest;
import com.genai.core.repository.response.VllmAnswerResponse;
import com.genai.core.repository.response.VllmAnswerStreamResponse;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.utils.TokenCalculateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class VllmModelRepositoryImpl implements ModelRepository {

    private final TokenCalculateUtil tokenCalculateUtil;
    private final WebClient webClient;
    private final LlmProperty llmProperty;
    private final ObjectMapper objectMapper;

    public VllmModelRepositoryImpl(
            @Autowired TokenCalculateUtil tokenCalculateUtil,
            @Autowired LlmProperty llmProperty,
            @Qualifier("llmWebClient") WebClient webClient,
            @Autowired ObjectMapper objectMapper
    ) {
        this.tokenCalculateUtil = tokenCalculateUtil;
        this.llmProperty = llmProperty;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
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
    public String generateAnswerStr(String query, String context, String sessionId, PromptEntity promptEntity) {

        StringBuilder answerBuilder = new StringBuilder();
        this.generateAnswer(query, context, null, Collections.emptyList(), sessionId, promptEntity).forEach(answerEntity -> {
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
     * @return 답변 응답 문자열
     */
    @Override
    public String generateAnswerStr(String query, String context, String summaryAnswer, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {

        StringBuilder answerBuilder = new StringBuilder();
        this.generateAnswer(query, context, summaryAnswer, conversations, sessionId, promptEntity).forEach(answerEntity -> {
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
    public List<AnswerEntity> generateAnswer(String query, String context, String summaryAnswer, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {

        int nowTokenSize = tokenCalculateUtil.calculateMaxTokens(promptEntity.getPromptContent(), query, summaryAnswer, conversations, context);

        VllmAnswerRequest requestBody = VllmAnswerRequest.builder()
                .modelName(llmProperty.getModelName())
                .temperature(promptEntity.getTemperature())
                .topP(promptEntity.getTopP())
                .minP(0)
                .topK(20)
                .maxTokens(Math.min(ModelConst.MAXIMUM_TOKEN_SIZE, promptEntity.getMaximumTokens() + nowTokenSize))
                .stream(false)
                .prompt(promptEntity.getPromptContent())
                .summaryAnswer(summaryAnswer)
                .conversations(conversations)
                .context(context)
                .query(query)
                .build();

        ResponseEntity<VllmAnswerResponse> responseBody = webClient.post()
                .uri(llmProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchangeToMono(response -> response
                        .bodyToMono(VllmAnswerResponse.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseBody == null || !responseBody.getStatusCode().is2xxSuccessful()) {
            throw new ModelErrorException("LLM(" + llmProperty.getModelName() + ")");
        }

        List<AnswerEntity> answerEntities = new ArrayList<>();

        if (responseBody.getBody() != null) {
            responseBody.getBody().getChoices().forEach(choice -> {
                String id = responseBody.getBody().getId();
                answerEntities.add(new AnswerEntity(id, choice.getMessage().getReasoningContent(), choice.getFinishReason(), true));
                answerEntities.add(new AnswerEntity(id, choice.getMessage().getContent(), choice.getFinishReason(), false));
            });
        }

        return answerEntities;
    }

    /**
     * 답변 실시간 생성 요청
     *
     * @param query        질의문
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 Flux
     */
    @Override
    public Flux<List<AnswerEntity>> generateStreamAnswer(String query, String summaryAnswer, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {
        return this.generateStreamAnswer(query, null, null, Collections.emptyList(), sessionId, promptEntity);
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
    public Flux<List<AnswerEntity>> generateStreamAnswer(String query, String context, String summaryAnswer, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity) {

        int nowTokenSize = tokenCalculateUtil.calculateMaxTokens(promptEntity.getPromptContent(), query, summaryAnswer, conversations, context);

        VllmAnswerRequest requestBody = VllmAnswerRequest.builder()
                .modelName(llmProperty.getModelName())
                .temperature(promptEntity.getTemperature())
                .topP(promptEntity.getTopP())
                .minP(0)
                .topK(20)
                .maxTokens(Math.min(ModelConst.MAXIMUM_TOKEN_SIZE, promptEntity.getMaximumTokens() + nowTokenSize))
                .stream(true)
                .prompt(promptEntity.getPromptContent())
                .summaryAnswer(summaryAnswer)
                .conversations(conversations)
                .context(context)
                .query(query)
                .build();

        return webClient.post()
                .uri(llmProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .mapNotNull(raw -> raw.replaceFirst("^data:", "").trim())
                .filter(raw -> !raw.equals("[DONE]"))
                .filter(raw -> !raw.isEmpty())
                .mapNotNull(json -> {
                    try {
                        VllmAnswerStreamResponse vllmAnswerStreamResponse = objectMapper.readValue(json, VllmAnswerStreamResponse.class);
                        return vllmAnswerStreamResponse.getChoices().stream()
                                .filter(choice -> {
                                    if (choice.getDelta().getContent() != null) {
                                        return !choice.getDelta().getContent().isEmpty();
                                    } else if (choice.getDelta().getReasoningContent() != null) {
                                        return !choice.getDelta().getReasoningContent().isEmpty();
                                    } else return false;
                                })
                                .map(choice -> {
                                    String id = vllmAnswerStreamResponse.getId();

                                    if (choice.getDelta().getReasoningContent() != null) {
                                        return new AnswerEntity(id, choice.getDelta().getReasoningContent(), choice.getFinishReason(), true);
                                    } else {
                                        return new AnswerEntity(id, choice.getDelta().getContent(), choice.getFinishReason(), false);
                                    }
                                })
                                .toList();
                    } catch (JsonProcessingException e) {
                        return Collections.emptyList();
                    }
                });
    }
}