package com.genai.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ToString
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "engine.llm.retry")
public class LlmRetryProperty {

    private int timeoutMs = 10;

    private int delayMs = 500;
}
