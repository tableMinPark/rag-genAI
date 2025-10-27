package com.genai.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.global.constant.ModelConst;
import com.genai.global.constant.SearchConst;
import com.genai.global.enums.CollectionType;
import com.genai.global.exception.ModelErrorException;
import com.genai.global.exception.SearchErrorException;
import com.genai.repository.SearchRepository;
import com.genai.repository.request.KeywordSearchRequest;
import com.genai.repository.request.RerankRequest;
import com.genai.repository.request.VectorSearchRequest;
import com.genai.repository.response.RerankResponse;
import com.genai.repository.response.SearchResponse;
import com.genai.repository.vo.SortVo;
import com.genai.repository.vo.VectorQueryVo;
import com.genai.service.domain.Document;
import com.genai.service.domain.Rerank;
import com.genai.service.domain.Search;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchRepositoryImpl implements SearchRepository {

    @Value("${engine.reranker.url}")
    private String RERANKER_URL;

    @Value("${engine.reranker.model-name}")
    private String RERANKER_MODEL_NAME;

    @Value("${engine.search.url}")
    private String SEARCH_URL;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    /**
     * 검색 결과 리랭킹
     *
     * @param query     질의문
     * @param documents 검색 문서 목록
     * @return 리랭킹 문서 목록
     */
    @Override
    public List<Rerank> rerank(String query, List<Rerank> documents) {

        RerankRequest requestBody = RerankRequest.builder()
                .query(query)
                .field(ModelConst.RERANK_FIELD)
                .document(documents)
                .build();

        RerankResponse responseBody = webClient.post()
                .uri(RERANKER_URL + "/" + RERANKER_MODEL_NAME)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(RerankResponse.class)
                .block();

        if (responseBody == null) {
            throw new ModelErrorException("RERANKER/" + RERANKER_MODEL_NAME);
        }

        return responseBody.getData();
    }

    /**
     * 컬렉션 키워드 검색 요청
     *
     * @param query     질의문
     * @param topK      top K
     * @param sessionId 세션 식별자
     * @return 키워드 검색 결과 목록
     */
    @Override
    public <T extends Document> List<Search<T>> keywordSearch(CollectionType collectionType, String collectionId, String query, int topK, String sessionId) {

        List<SortVo> sorting = collectionType.getSortFields().stream()
                .map(field -> new SortVo(field, false))
                .toList();

        KeywordSearchRequest requestBody = KeywordSearchRequest.builder()
                .filterQuery("<alias:match:" + collectionType.getAlias() + ">")
                .commonQuery(query)
                .topK(topK)
                .sessionInfo(List.of(sessionId))
                .searchField(collectionType.getKeywordSearchFields())
                .sorting(sorting)
                .hideQueryLog(SearchConst.HIDE_QUERY_LOG)
                .useSynonym(SearchConst.USE_SYNONYM)
                .synonymExpansion(SearchConst.SYNONYM_EXPANSION)
                .build();

        String json = webClient.post()
                .uri(SEARCH_URL + "/" + collectionType.getCollectionId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JavaType type = objectMapper.getTypeFactory().constructParametricType(SearchResponse.class, collectionType.getMappingClass());

        try {
            SearchResponse<T> responseBody = objectMapper.readValue(json, type);

            if (responseBody == null) {
                throw new SearchErrorException("KEYWORD/" + collectionType.getCollectionName());
            }

            return responseBody.getDocument();

        } catch (JsonProcessingException e) {
            throw new SearchErrorException("KEYWORD/" + collectionType.getCollectionName());
        }
    }

    /**
     * 컬렉션 벡터 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param collectionId   컬렉션 ID
     * @param query          질의문
     * @param topK           top K
     * @return 벡터 검색 결과 목록
     */
    @Override
    public <T extends Document> List<Search<T>> vectorSearch(CollectionType collectionType, String collectionId, String query, int topK) {

        List<VectorQueryVo> vectorQueries = collectionType.getVectorSearchFields().stream()
                .map(field -> new VectorQueryVo(field, query, topK))
                .toList();

        VectorSearchRequest requestBody = VectorSearchRequest.builder()
                .filterQuery("<alias:match:" + collectionType.getAlias() + ">")
                .vectorQuery(vectorQueries)
                .build();

        String json = webClient.post()
                .uri(SEARCH_URL + "/" + collectionType.getCollectionId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JavaType type = objectMapper.getTypeFactory().constructParametricType(SearchResponse.class, collectionType.getMappingClass());

        try {
            SearchResponse<T> responseBody = objectMapper.readValue(json, type);

            if (responseBody == null) {
                throw new SearchErrorException("VECTOR/" + collectionType.getCollectionName());
            }

            return responseBody.getDocument();

        } catch (JsonProcessingException e) {
            throw new SearchErrorException("VECTOR/" + collectionType.getCollectionName());
        }
    }
}