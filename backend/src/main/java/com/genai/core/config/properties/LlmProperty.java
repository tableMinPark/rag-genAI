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

    private String platform;

    private int responseTimeout;

    private int readTimeout;

    private int writeTimeout;

    private int connectTimeout;

    private String host;

    private int port;

    private String path;

    private String modelName;

    private int modelContextLimit;

    private int internalTokenOverhead;

    private int safetyMargin;

    private int minOutputTokens;

    private int maxOutputTokens;

    private String apiKey;

    /**
     * LLM 요청 URL 조회
     *
     * @return LLM 요청 URL
     */
    public String getUrl() {

        StringBuilder url = new StringBuilder();

        if (!host.startsWith("http")) {
            url.append("http://");
        }

        url.append(host);
        url.append(":");
        url.append(port);
        url.append(path.startsWith("/") ? "" : "/");
        url.append(path);

        return url.toString().trim();
    }
}
