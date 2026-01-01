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

    private String path;

    /**
     * 컬렉션 조회 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 컬렉션 조회 URL
     */
    public String getSelectUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s", host, port, path, collectionId);
    }

    /**
     * 컬렉션 생성 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 컬렉션 생성 URL
     */
    public String getCreateUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s", host, port, path, collectionId);
    }

    /**
     * 컬렉션 삭제 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 컬렉션 삭제 URL
     */
    public String getDeleteUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s/delete", host, port, path, collectionId);
    }
}
