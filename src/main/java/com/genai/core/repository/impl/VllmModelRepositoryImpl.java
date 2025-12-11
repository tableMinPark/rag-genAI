package com.genai.core.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.config.constant.ModelConst;
import com.genai.core.config.properties.LlmProperty;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.request.VllmAnswerRequest;
import com.genai.core.repository.response.VllmAnswerStreamResponse;
import com.genai.core.repository.response.VllmAnswerResponse;
import com.genai.exception.ModelErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class VllmModelRepositoryImpl implements ModelRepository {

    private final WebClient webClient;
    private final LlmProperty llmProperty;
    private final ObjectMapper objectMapper;

    public VllmModelRepositoryImpl(
            @Autowired LlmProperty llmProperty,
            @Qualifier("llmWebClient") WebClient webClient,
            @Autowired ObjectMapper objectMapper
    ) {
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
     * @return 답변 응답
     */
    @Override
    public List<AnswerEntity> generateAnswer(String query, String context, String sessionId, PromptEntity promptEntity) {

        VllmAnswerRequest requestBody = VllmAnswerRequest.builder()
                .modelName(llmProperty.getModelName())
                .prompt(promptEntity.getPromptContent())
                .userInput(query)
                .temperature(promptEntity.getTemperature())
                .topP(promptEntity.getTopP())
                .maxTokens(promptEntity.getMaximumTokens())
                .stream(false)
                .context(context)
                .build();

        log.info("LLM 답변 요청({}) | {}", sessionId, requestBody);

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
    public Flux<List<AnswerEntity>> generateStreamAnswer(String query, String sessionId, PromptEntity promptEntity) {
        return this.generateStreamAnswer(query, null, sessionId, promptEntity);
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
    public Flux<List<AnswerEntity>> generateStreamAnswer(String query, String context, String sessionId, PromptEntity promptEntity) {

        VllmAnswerRequest requestBody = VllmAnswerRequest.builder()
                .modelName(llmProperty.getModelName())
                .prompt(promptEntity.getPromptContent())
                .userInput(query)
                .temperature(promptEntity.getTemperature())
                .topP(promptEntity.getTopP())
                .minP(0)
                .topK(20)
                .maxTokens(promptEntity.getMaximumTokens())
                .stream(true)
                .context(context)
                .build();

        log.info("LLM 답변 요청({}) | {}", sessionId, requestBody);

        StringBuffer jsonBuffer = new StringBuffer();

        return webClient.post()
                .uri(llmProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(VllmAnswerStreamResponse.class)
                .mapNotNull(vllmAnswerStreamResponse -> vllmAnswerStreamResponse.getChoices().stream()
                        .map(choice -> {
                            String id = vllmAnswerStreamResponse.getId();

                            if (choice.getDelta().getReasoningContent() != null) {
                                return new AnswerEntity(id, choice.getDelta().getReasoningContent(), choice.getFinishReason(), true);
                            } else {
                                return new AnswerEntity(id, choice.getDelta().getContent(), choice.getFinishReason(), false);
                            }
                        })
                        .toList());
//                .bodyToFlux(DataBuffer.class)
//                // 버퍼 변환
//                .map(dataBuffer -> {
//                    try {
//                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                        dataBuffer.read(bytes);
//                        return new String(bytes, StandardCharsets.UTF_8);
//                    } finally {
//                        DataBufferUtils.release(dataBuffer);
//                    }
//                })
//                // 스트림 종료 메시지 필터링
//                .filter(dataBuffer -> !dataBuffer.trim().equals("[DONE]"))
//                // 버퍼 적재
//                .flatMapSequential(chunk -> {
//                    List<String> blocks = new ArrayList<>();
//                    jsonBuffer.append(chunk);
//
//                    int idx;
//                    while ((idx = jsonBuffer.indexOf(ModelConst.JSON_END_MARKER)) != -1) {
//                        int endIdx = idx + ModelConst.JSON_END_MARKER.length();
//                        String block = jsonBuffer.substring(0, endIdx).trim();
//                        jsonBuffer.delete(0, endIdx);
//                        blocks.add(block);
//                    }
//                    return Flux.fromIterable(blocks);
//                })
//                // JSON 문자열 문법 검증
//                .concatWith(Mono.defer(() -> {
//                    String remaining = jsonBuffer.toString().trim();
//                    if (remaining.endsWith(ModelConst.JSON_END_MARKER.trim())) {
//                        return Mono.just(remaining);
//                    } else if (!remaining.replaceFirst("^data: ", "").trim().startsWith("[DONE]")) {
//                        log.warn("Split json string detected: {}", remaining);
//                    }
//                    return Mono.empty();
//                }))
//                // JSON 문자열 역직렬화
//                .mapNotNull(block -> {
//                    block = block.replaceFirst("^data: ", "");
//                    try {
//                        return objectMapper.readValue(block, VllmAnswerStreamResponse.class);
//                    } catch (IOException e) {
//                        log.warn("Failed to parse block, ignoring: {}", block, e);
//                        return null;
//                    }
//                })
//                // Answer 도메인 정규화
//                .mapNotNull(vllmAnswerStreamResponse -> vllmAnswerStreamResponse.getChoices().stream()
//                        .map(choice -> {
//                            String id = vllmAnswerStreamResponse.getId();
//
//                            if (choice.getDelta().getReasoningContent() != null) {
//                                return new AnswerEntity(id, choice.getDelta().getReasoningContent(), choice.getFinishReason(), true);
//                            } else {
//                                return new AnswerEntity(id, choice.getDelta().getContent(), choice.getFinishReason(), false);
//                            }
//                        })
//                        .toList());
    }
}