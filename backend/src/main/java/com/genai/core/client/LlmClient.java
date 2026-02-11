package com.genai.core.client;

import com.genai.core.config.properties.LlmProperty;
import com.genai.core.exception.ModelErrorException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LlmClient {

    private final WebClient webClient;
    private final LlmProperty llmProperty;

    public LlmClient(@Qualifier("llmWebClient") WebClient webClient, @Autowired LlmProperty llmProperty) {
        this.webClient = webClient;
        this.llmProperty = llmProperty;
    }

    public String generateAnswerSync(Object requestBody) {

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

        return responseEntity.getBody();
    }

    public Mono<String> generateAnswerAsync(Object requestBody) {

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
                        sink.next(responseEntity.getBody());
                    }
                });
    }

    public Flux<String> generateStreamAnswerAsync(Object requestBody) {

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
                .filter(json -> !json.isEmpty());
    }
}
