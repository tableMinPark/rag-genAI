package com.genai.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "engine.indexer")
public class IndexerProperty {

    private int connectTimeout;

    private int responseTimeout;

    private int readTimeout;

    private int writeTimeout;

    private String host;

    private int port;

    private String path;

    /**
     * 색인 정보 조회 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 색인 URL
     */
    public String getSelectUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s", host, port, path, collectionId);
    }

    /**
     * 벡터 변환 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 벡터 변환 URL
     */
    public String getConvertVectorUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s/convert-vector", host, port, path, collectionId);
    }

    /**
     * 색인 등록 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 색인 URL
     */
    public String getCreateIndexUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s", host, port, path, collectionId);
    }

    /**
     * 색인 문서 삭제 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 색인 URL
     */
    public String getDeleteIndexUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s/delete", host, port, path, collectionId);
    }

    /**
     * 정적 색인 준비 URL 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 정적 색인 준비 URL
     */
    public String getReadyStaticUrl(String collectionId) {
        return String.format("http://%s:%d/%s/%s/static", host, port, path, collectionId);
    }
}
