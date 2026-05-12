package com.genai.core.config;

import com.genai.core.common.enums.LlmPlatformType;
import com.genai.core.common.enums.LlmType;
import com.genai.core.config.instance.LlmInstance;
import com.genai.core.config.properties.LlmInstanceProperty;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "engine.llm")
public class LlmModelConfig {

    private List<LlmInstanceProperty> instances;

    @Bean
    public Map<LlmType, List<LlmInstance>> llmInstanceMap() {

        Map<LlmType, List<LlmInstance>> llmInstanceMap = new ConcurrentHashMap<>();

        Arrays.stream(LlmType.values()).forEach(llmType -> llmInstanceMap.put(llmType, new ArrayList<>()));

        instances.stream()
                .sorted((o1, o2) -> {
                    if (o1.getType().equals(o2.getType())) {
                        return o1.getPlatform().compareTo(o2.getPlatform());
                    }
                    return o1.getType().compareTo(o2.getType());
                })
                .forEach(llmInstanceProperty -> {

                    LlmPlatformType platformType = LlmPlatformType.valueOf(llmInstanceProperty.getPlatform().toUpperCase());
                    LlmType llmType = LlmType.valueOf(llmInstanceProperty.getType().toUpperCase());
                    List<LlmInstance> llmInstance = llmInstanceMap.getOrDefault(llmType, new ArrayList<>());

                    int index = llmInstance.size();
                    String instanceId = String.format("%s-%d", llmInstanceProperty.getType(), index);

                    HttpClient httpClient = HttpClient.create()
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, llmInstanceProperty.getConnectTimeout())
                            .responseTimeout(Duration.ofMillis(llmInstanceProperty.getResponseTimeout()))
                            .doOnConnected(conn ->
                                    conn.addHandlerLast(new ReadTimeoutHandler(llmInstanceProperty.getReadTimeout(), TimeUnit.MILLISECONDS))
                                            .addHandlerLast(new WriteTimeoutHandler(llmInstanceProperty.getWriteTimeout(), TimeUnit.MILLISECONDS)));

                    WebClient webClient = WebClient.builder()
                            .clientConnector(new ReactorClientHttpConnector(httpClient))
                            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .build();

                    llmInstance.add(LlmInstance.builder()
                            .instanceId(instanceId)
                            .platformType(platformType)
                            .llmInstanceProperty(llmInstanceProperty)
                            .webClient(webClient)
                            .build());

                    llmInstanceMap.put(llmType, llmInstance);

                    log.info("[model] {} | {} | {} | {}:{}{}", instanceId, llmInstanceProperty.getSessionCount(), platformType.name(), llmInstanceProperty.getHost(), llmInstanceProperty.getPort(), llmInstanceProperty.getPath());
                });

        if (llmInstanceMap.isEmpty()) {
            throw new IllegalStateException("[model] LLM Instance is empty");
        }

        if (llmInstanceMap.get(LlmType.DEFAULT) == null || llmInstanceMap.get(LlmType.DEFAULT).isEmpty()) {
            throw new IllegalStateException("[model] LLM Instance default is required");
        }

        return llmInstanceMap;
    }
}
