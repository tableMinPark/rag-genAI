package com.genai.client;

import com.genai.client.request.KeywordSearchRequest;
import com.genai.client.request.VectorSearchRequest;
import com.genai.client.response.SearchResponse;
import com.genai.entity.CollectionType;
import com.genai.entity.LawEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchClientImpl implements SearchClient {

    @Value("${engine.search.url}")
    private String SEARCH_URL;

    private final WebClient restClient;

    /**
     * 법령 컬렉션 키워드 검색 요청
     *
     * @param query     질의문
     * @param topK      top K
     * @param sessionId 세션 식별자
     * @return 키워드 검색 결과 목록
     */
    @Override
    public SearchResponse<LawEntity> lawKeywordSearch(String query, int topK, String sessionId) {

        KeywordSearchRequest requestBody = new KeywordSearchRequest(query, topK, sessionId);

        SearchResponse<LawEntity> responseBody = restClient.post()
                .uri(SEARCH_URL + "/" + CollectionType.LAW.getCollectionName())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SearchResponse<LawEntity>>() {})
                .block();

        if (responseBody == null) {
            throw new RuntimeException();
        } else {
            StringBuilder builder = new StringBuilder();
            responseBody.getDocument().forEach(document ->
                    builder.append(String.format("%.4f", document.getScore()))
                            .append(" | ")
                            .append(document.getFields().getContext().replace("\n", "\\n"))
                            .append("\n"));
            log.info("\n[ 키워드 검색 결과 {} 개 ]\n{}", responseBody.getDocument().size(), builder.toString().trim());
        }

        return responseBody;
    }

    /**
     * 법령 컬렉션 벡터 검색 요청
     *
     * @param query 질의문
     * @param topK  top K
     * @return 벡터 검색 결과 목록
     */
    @Override
    public SearchResponse<LawEntity> lawVectorSearch(String query, int topK) {

        VectorSearchRequest requestBody = new VectorSearchRequest(query, topK);

        SearchResponse<LawEntity> responseBody = restClient.post()
                .uri(SEARCH_URL + "/" + CollectionType.LAW.getCollectionName())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SearchResponse<LawEntity>>() {})
                .block();

        if (responseBody == null) {
            throw new RuntimeException();
        } else {
            StringBuilder builder = new StringBuilder();
            responseBody.getDocument().forEach(document ->
                    builder.append(String.format("%.4f", document.getScore()))
                            .append(" | ")
                            .append(document.getFields().getContext().replace("\n", "\\n"))
                            .append("\n"));
            log.info("\n[ 벡터 검색 결과 {} 개 ]\n{}", responseBody.getDocument().size(), builder.toString().trim());
        }

        return responseBody;
    }
}