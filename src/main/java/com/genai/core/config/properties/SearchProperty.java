package com.genai.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "engine.search")
public class SearchProperty {

    private int connectTimeout;

    private int responseTimeout;

    private int readTimeout;

    private int writeTimeout;

    private String host;

    private int port;

    private String path;

    /**
     * 검색 요청 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 검색 요청 URL
     */
    public String getUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s", host, port, path, collectionId);
    }
}
