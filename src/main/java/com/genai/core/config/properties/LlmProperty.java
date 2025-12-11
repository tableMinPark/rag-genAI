package com.genai.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "engine.llm")
public class LlmProperty {

    private int connectTimeout;

    private int responseTimeout;

    private int readTimeout;

    private int writeTimeout;

    private String host;

    private int port;

    private String path;

    private String modelName;

    private String platform;

    /**
     * LLM 요청 URL 조회
     *
     * @return LLM 요청 URL
     */
    public String getUrl() {
        return String.format("http://%s:%d/%s", host, port, path);
    }
}
