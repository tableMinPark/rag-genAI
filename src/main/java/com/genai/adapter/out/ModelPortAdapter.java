package com.genai.adapter.out;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.adapter.out.request.AnswerRequest;
import com.genai.adapter.out.request.RerankRequest;
import com.genai.adapter.out.response.AnswerResponse;
import com.genai.adapter.out.response.RerankResponse;
import com.genai.application.domain.*;
import com.genai.application.port.ModelPort;
import com.genai.constant.ModelConst;
import com.genai.exception.ModelErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
@Component
@RequiredArgsConstructor
public class ModelPortAdapter implements ModelPort {

    @Value("${engine.reranker.url}")
    private String RERANKER_URL;

    @Value("${engine.reranker.model-name}")
    private String RERANKER_MODEL_NAME;

    @Value("${engine.llm.url}")
    private String LLM_URL;

    @Value("${engine.llm.model-name}")
    private String LLM_MODEL_NAME;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    /**
     * 검색 결과 리랭킹
     *
     * @param query     질의문
     * @param documents 검색 결과 목록
     * @return 리랭킹 검색 결과 목록
     */
    @Override
    public List<RerankDocument<Law>> lawRerank(String query, List<Document<Law>> documents) {

        RerankRequest<Law> requestBody = RerankRequest.<Law>builder()
                .query(query)
                .field(ModelConst.RERANK_FIELD)
                .document(documents)
                .build();

        RerankResponse<Law> responseBody = webClient.post()
                .uri(RERANKER_URL + "/" + RERANKER_MODEL_NAME)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<RerankResponse<Law>>() {
                })
                .block();

        if (responseBody == null) {
            throw new ModelErrorException("RERANKER(" + RERANKER_MODEL_NAME + ")");
        } else {
            StringBuilder builder = new StringBuilder();
            responseBody.getData().forEach(document ->
                    builder.append(String.format("%.4f", document.getRerankScore()))
                            .append(" | ")
                            .append(document.getFields().getContext().replace("\n", "\\n"))
                            .append("\n"));
            log.info("\n[ 리랭킹 결과 {} 개 ]\n{}", responseBody.getData().size(), builder.toString().trim());
        }

        return responseBody.getData();
    }

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

        AnswerRequest requestBody = AnswerRequest.builder()
                .modelName(LLM_MODEL_NAME)
                .userId(sessionId)
                .userInput(query)
                .prompt(prompt.getContext())
                .temperature(prompt.getTemperature())
                .topP(prompt.getTopP())
                .maxTokens(prompt.getMaxTokens())
                .multiTurn(ModelConst.LLM_MULTITURN)
                .stream(false)
                .context(context)
                .build();

        ResponseEntity<AnswerResponse> responseBody = webClient.post()
                .uri(LLM_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(AnswerResponse.class)
                .block();

        if (responseBody == null || !responseBody.getStatusCode().is2xxSuccessful()) {
            throw new ModelErrorException("LLM(" + LLM_MODEL_NAME + ")");
        }

        return responseBody.getBody() != null
                ? responseBody.getBody().getData()
                : Collections.emptyList();
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

        AnswerRequest requestBody = AnswerRequest.builder()
                .modelName(LLM_MODEL_NAME)
                .userId(sessionId)
                .userInput(query)
                .prompt(prompt.getContext())
                .temperature(prompt.getTemperature())
                .topP(prompt.getTopP())
                .maxTokens(prompt.getMaxTokens())
                .multiTurn(ModelConst.LLM_MULTITURN)
                .stream(true)
                .context(context)
                .build();

        StringBuffer jsonBuffer = new StringBuffer();

        return webClient.post()
                .uri(LLM_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(DataBuffer.class)
                .map(dataBuffer -> {
                    try {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        return new String(bytes, StandardCharsets.UTF_8);
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                })
                .flatMapSequential(chunk -> {
                    List<String> blocks = new ArrayList<>();
                    jsonBuffer.append(chunk);

                    int idx;
                    while ((idx = jsonBuffer.indexOf(ModelConst.LLM_RESPONSE_END_MARKER)) != -1) {
                        int endIdx = idx + ModelConst.LLM_RESPONSE_END_MARKER.length();
                        String block = jsonBuffer.substring(0, endIdx).trim();
                        jsonBuffer.delete(0, endIdx);
                        blocks.add(block);
                    }
                    return Flux.fromIterable(blocks);
                })
                .concatWith(Mono.defer(() -> {
                    String remaining = jsonBuffer.toString().trim();
                    if (remaining.endsWith(ModelConst.LLM_RESPONSE_END_MARKER.trim())) {
                        return Mono.just(remaining);
                    } else {
                        log.warn("Split json string detected: {}", remaining);
                    }
                    return Mono.empty();
                }))
                .mapNotNull(block -> {
                    try {
                        return objectMapper.readValue(block, AnswerResponse.class);
                    } catch (IOException e) {
                        log.warn("Failed to parse block, ignoring: {}", block, e);
                        return null;
                    }
                })
                .mapNotNull(AnswerResponse::getData);
    }
}