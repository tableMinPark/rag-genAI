package com.genai.core.config;

import com.genai.core.config.instance.LlmInstance;
import com.genai.core.config.properties.LlmProperty;
import com.genai.core.type.LlmPlatformType;
import com.genai.core.type.LlmType;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "engine")
public class LlmModelConfig {

    private List<LlmProperty> llm;

    @Bean
    public Map<LlmType, List<LlmInstance>> llmInstanceMap() {

        log.info("LLM Instance : {}", llm.size());

        Map<LlmType, List<LlmInstance>> llmInstanceMap = new ConcurrentHashMap<>();

        AtomicInteger index = new AtomicInteger(0);

        llm.stream()
                .sorted((o1, o2) -> {
                    if (o1.getType().equals(o2.getType())) {
                        return o1.getPlatform().compareTo(o2.getPlatform());
                    }
                    return o1.getType().compareTo(o2.getType());
                })
                .forEach(llmProperty -> {
                    String instanceId = String.format("%s-%d", llmProperty.getType(), index.getAndIncrement());
                    LlmPlatformType platformType = LlmPlatformType.valueOf(llmProperty.getPlatform().toUpperCase());
                    LlmType llmType = LlmType.valueOf(llmProperty.getType().toUpperCase());

                    HttpClient httpClient = HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, llmProperty.getConnectTimeout())
                            .responseTimeout(Duration.ofMillis(llmProperty.getResponseTimeout()))
                            .doOnConnected(conn ->
                                    conn.addHandlerLast(new ReadTimeoutHandler(llmProperty.getReadTimeout(), TimeUnit.MILLISECONDS))
                                            .addHandlerLast(new WriteTimeoutHandler(llmProperty.getWriteTimeout(), TimeUnit.MILLISECONDS)));

                    WebClient webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .build();

                    List<LlmInstance> llmInstance = llmInstanceMap.getOrDefault(llmType, new ArrayList<>());

                    llmInstance.add(LlmInstance.builder()
                            .instanceId(instanceId)
                            .platformType(platformType)
                            .llmProperty(llmProperty)
                            .webClient(webClient)
                            .build());

                    llmInstanceMap.put(llmType, llmInstance);

                    log.info("[{}] {} | {} | {}:{}/{}", index.get() - 1, instanceId, platformType.name(), llmProperty.getHost(), llmProperty.getPort(), llmProperty.getPath());
                });

        if (llmInstanceMap.isEmpty()) {
            throw new IllegalStateException("LLM Instance is empty");
        }
        if (llmInstanceMap.get(LlmType.DEFAULT) == null || llmInstanceMap.get(LlmType.DEFAULT).isEmpty()) {
            throw new IllegalStateException("LLM Instance default is required");
        }

        return llmInstanceMap;
    }
}
