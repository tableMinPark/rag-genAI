package com.genai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.client.request.AnswerRequest;
import com.genai.client.request.RerankRequest;
import com.genai.client.response.AnswerResponse;
import com.genai.client.response.RerankResponse;
import com.genai.client.vo.DocumentVo;
import com.genai.constant.ModelConst;
import com.genai.entity.LawEntity;
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
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelClientImpl implements ModelClient {

    @Value("${engine.reranker.url}")
    private String RERANKER_URL;

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
    public RerankResponse<LawEntity> lawRerank(String query, List<DocumentVo<LawEntity>> documents) {

        RerankRequest<LawEntity> requestBody = new RerankRequest<>(query, documents);

        RerankResponse<LawEntity> responseBody = webClient.post()
                .uri(RERANKER_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<RerankResponse<LawEntity>>() {
                })
                .block();

        if (responseBody == null) {
            throw new RuntimeException();
        } else {
            StringBuilder builder = new StringBuilder();
            responseBody.getData().forEach(document ->
                    builder.append(String.format("%.4f", document.getRerankScore()))
                            .append(" | ")
                            .append(document.getFields().getContext().replace("\n", "\\n"))
                            .append("\n"));
            log.info("\n[ 리랭킹 결과 {} 개 ]\n{}", responseBody.getData().size(), builder.toString().trim());
        }

        return responseBody;
    }

    /**
     * 답변 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param prompt    시스템 프롬 프트
     * @param sessionId 세션 식별자
     * @return 답변 응답
     */
    @Override
    public AnswerResponse generateAnswer(String query, String context, String prompt, String sessionId) {

        AnswerRequest requestBody = new AnswerRequest(prompt, LLM_MODEL_NAME, sessionId, query, context, false);

        ResponseEntity<AnswerResponse> responseBody = webClient.post()
                .uri(LLM_URL)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(AnswerResponse.class)
                .block();

        if (responseBody == null || !responseBody.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException();
        }

        return responseBody.getBody();
    }

    /**
     * 답변 실시간 생성 요청
     *
     * @param query     질의문
     * @param context   검색 결과 데이터
     * @param prompt    시스템 프롬 프트
     * @param sessionId 세션 식별자
     * @return 답변 Flux
     */
    @Override
    public Flux<AnswerResponse> generateStreamAnswer(String query, String context, String prompt, String sessionId) {

        AnswerRequest requestBody = new AnswerRequest(prompt, LLM_MODEL_NAME, sessionId, query, context, true);

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
                    if (remaining.endsWith("}")) {
                        return Mono.just(remaining);
                    } else {
                        log.warn("split json string detected: {}", remaining);
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
                });
    }
}