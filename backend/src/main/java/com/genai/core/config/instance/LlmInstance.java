package com.genai.core.config.instance;

import com.genai.core.config.properties.LlmProperty;
import com.genai.core.type.LlmPlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;

@Builder
@Getter
@AllArgsConstructor
public class LlmInstance {

    private final String instanceId;

    private final LlmPlatformType platformType;

    private final LlmProperty llmProperty;

    private final WebClient webClient;
}
