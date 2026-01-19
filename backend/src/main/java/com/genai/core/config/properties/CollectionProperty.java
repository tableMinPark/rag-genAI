package com.genai.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "engine.collection")
public class CollectionProperty {

    private int connectTimeout;

    private int responseTimeout;

    private int readTimeout;

    private int writeTimeout;

    private String host;

    private int port;

    /**
     * 컬렉션 요청 URL 조회
     *
     * @return 컬렉션 요청 URL
     */
    public String getUrl() {
        return String.format("http://%s:%d", host, port);
    }
}
