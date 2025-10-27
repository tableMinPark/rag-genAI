package com.genai.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.repository.ModelRepository;
import com.genai.repository.request.QwenAnswerRequest;
import com.genai.repository.response.QwenAnswerResponse;
import com.genai.service.domain.Answer;
import com.genai.service.domain.Prompt;
import com.genai.global.constant.ModelConst;
import com.genai.global.exception.ModelErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Collections;
import java.util.List;

@Slf4j
@Component("QwenModelRepositoryImpl")
@RequiredArgsConstructor
public class QwenModelRepositoryImpl implements ModelRepository {

    @Value("${engine.llm.qwen3.url}")
    private String LLM_URL;

    @Value("${engine.llm.qwen3.model-name}")
    private String LLM_MODEL_NAME;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    /**
     * 답변 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param sessionId 세션 식별자
     * @param prompt    프롬 프트
     * @return 답변 응답
     */
    @Override
    public List<Answer> generateAnswer(String query, String context, String sessionId, Prompt prompt) {

        QwenAnswerRequest requestBody = QwenAnswerRequest.builder()
                .modelName(LLM_MODEL_NAME)
                .prompt(prompt.getContext())
                .userInput(query)
                .temperature(prompt.getTemperature())
                .topP(prompt.getTopP())
                .maxTokens(prompt.getMaxTokens())
                .stream(false)
                .context(context)
                .build();

        log.info("LLM 답변 요청({}) | {}", sessionId, requestBody);

        ResponseEntity<QwenAnswerResponse> responseBody = webClient.post()
                .uri(LLM_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(QwenAnswerResponse.class)
                .block();


        if (responseBody == null || !responseBody.getStatusCode().is2xxSuccessful()) {
            throw new ModelErrorException("LLM(" + LLM_MODEL_NAME + ")");
        }

        return responseBody.getBody() == null
                ? Collections.emptyList()
                : responseBody.getBody().getChoices().stream()
                .map(choice -> {
                    String id = responseBody.getBody().getId();

                    if (choice.getDelta().getReasoningContent() != null) {
                        return new Answer(id, choice.getDelta().getReasoningContent(), choice.getFinishReason(), true);
                    } else {
                        return new Answer(id, choice.getDelta().getContent(), choice.getFinishReason(), false);
                    }
                })
                .toList();
    }

    /**
     * 답변 실시간 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param sessionId 세션 식별자
     * @param prompt    프롬 프트
     * @return 답변 Flux
     */
    @Override
    public Flux<List<Answer>> generateStreamAnswer(String query, String context, String sessionId, Prompt prompt) {

        QwenAnswerRequest requestBody = QwenAnswerRequest.builder()
                .modelName(LLM_MODEL_NAME)
                .prompt(prompt.getContext())
                .userInput(query)
                .temperature(prompt.getTemperature())
                .topP(prompt.getTopP())
                .minP(prompt.getMinP())
                .topK(prompt.getTopK())
                .maxTokens(prompt.getMaxTokens())
                .stream(true)
                .context(context)
                .build();

        log.info("LLM 답변 요청({}) | {}", sessionId, requestBody);

        StringBuffer jsonBuffer = new StringBuffer();

        return webClient.post()
                .uri(LLM_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                // 버퍼 변환
                .map(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                })
                // 스트림 종료 메시지 필터링
                .filter(dataBuffer -> !dataBuffer.trim().equals("[DONE]"))
                // 버퍼 적재
                .flatMapSequential(chunk -> {
                    List<String> blocks = new ArrayList<>();
                    jsonBuffer.append(chunk);

                    int idx;
                    while ((idx = jsonBuffer.indexOf(ModelConst.JSON_END_MARKER)) != -1) {
                        int endIdx = idx + ModelConst.JSON_END_MARKER.length();
                        String block = jsonBuffer.substring(0, endIdx).trim();
                        jsonBuffer.delete(0, endIdx);
                        blocks.add(block);
                    }
                    return Flux.fromIterable(blocks);
                })
                // JSON 문자열 문법 검증
                .concatWith(Mono.defer(() -> {
                    String remaining = jsonBuffer.toString().trim();
                    if (remaining.endsWith(ModelConst.JSON_END_MARKER.trim())) {
                        return Mono.just(remaining);
                    } else if (!remaining.replaceFirst("^data: ", "").trim().startsWith("[DONE]")) {
                        log.warn("Split json string detected: {}", remaining);
                    }
                    return Mono.empty();
                }))
                // JSON 문자열 역직렬화
                .mapNotNull(block -> {
                    block = block.replaceFirst("^data: ", "");
                    try {
                        return objectMapper.readValue(block, QwenAnswerResponse.class);
                    } catch (IOException e) {
                        log.warn("Failed to parse block, ignoring: {}", block, e);
                        return null;
                    }
                })
                // Answer 도메인 정규화
                .mapNotNull(qwenAnswerResponse -> qwenAnswerResponse.getChoices().stream()
                        .map(choice -> {
                            String id = qwenAnswerResponse.getId();

                            if (choice.getDelta().getReasoningContent() != null) {
                                return new Answer(id, choice.getDelta().getReasoningContent(), choice.getFinishReason(), true);
                            } else {
                                return new Answer(id, choice.getDelta().getContent(), choice.getFinishReason(), false);
                            }
                        })
                        .toList());
    }
}