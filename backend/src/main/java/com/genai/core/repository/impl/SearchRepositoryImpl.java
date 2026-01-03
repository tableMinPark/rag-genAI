package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.constant.SearchConst;
import com.genai.core.config.properties.RerankerProperty;
import com.genai.core.config.properties.SearchProperty;
import com.genai.core.repository.SearchRepository;
import com.genai.core.repository.entity.DocumentEntity;
import com.genai.core.repository.request.KeywordSearchRequest;
import com.genai.core.repository.request.RerankRequest;
import com.genai.core.repository.request.VectorSearchRequest;
import com.genai.core.repository.response.RerankResponse;
import com.genai.core.repository.response.SearchResponse;
import com.genai.core.repository.vo.SearchSortVO;
import com.genai.core.repository.vo.SearchVectorQueryVO;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
import com.genai.core.type.CollectionType;
import com.genai.core.exception.SearchErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
public class SearchRepositoryImpl implements SearchRepository {

    private final WebClient webClient;
    private final SearchProperty searchProperty;
    private final RerankerProperty rerankerProperty;
    private final ObjectMapper objectMapper;

    public SearchRepositoryImpl(
            @Qualifier("searchWebClient") WebClient webClient,
            @Autowired SearchProperty searchProperty,
            @Autowired RerankerProperty rerankerProperty,
            @Autowired ObjectMapper objectMapper
    ) {
        this.webClient = webClient;
        this.searchProperty = searchProperty;
        this.rerankerProperty = rerankerProperty;
        this.objectMapper = objectMapper;
    }

    /**
     * 키워드 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param query          질의문
     * @param topK           top K
     * @param sessionId      세션 식별자
     * @param alias          필터 코드 목록
     * @return 키워드 검색 결과 목록
     */
    @Override
    public <T extends DocumentEntity> List<Search<T>> keywordSearch(CollectionType collectionType, String query, int topK, String sessionId, List<String> alias) {

        List<SearchSortVO> sorting = collectionType.getSortFields().stream()
                .map(field -> new SearchSortVO(field, false))
                .toList();

        StringBuilder filterQueryBuilder = new StringBuilder();

        if (!alias.isEmpty()) {
            filterQueryBuilder.append("<alias:in:");
            alias.forEach(s -> filterQueryBuilder.append(" ").append(s));
            filterQueryBuilder.append(">");
        }

        KeywordSearchRequest requestBody = KeywordSearchRequest.builder()
                .filterQuery(filterQueryBuilder.toString().trim().isBlank() ? null : filterQueryBuilder.toString())
                .commonQuery(query)
                .topK(topK)
                .sessionInfo(List.of(sessionId))
                .searchField(collectionType.getKeywordSearchFields())
                .sorting(sorting)
                .hideQueryLog(SearchConst.HIDE_QUERY_LOG)
                .useSynonym(SearchConst.USE_SYNONYM)
                .synonymExpansion(SearchConst.SYNONYM_EXPANSION)
                .build();

        ResponseEntity<String> responseEntity = webClient.post()
                .uri(searchProperty.getUrl(collectionType.getCollectionId()))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchangeToMono(response -> response
                        .bodyToMono(String.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        // 응답 체크
        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new SearchErrorException("KEYWORD/" + collectionType.getCollectionId());
        }

        try {
            // 역직렬화
            JavaType type = objectMapper.getTypeFactory().constructParametricType(SearchResponse.class, collectionType.getMappingClass());
            SearchResponse<T> responseBody = objectMapper.readValue(responseEntity.getBody(), type);

            // 응답 바디 체크
            if (responseBody == null) {
                throw new SearchErrorException("KEYWORD/" + collectionType.getCollectionId());
            }

            return responseBody.getDocument();

        } catch (JsonProcessingException e) {
            throw new SearchErrorException("KEYWORD/" + collectionType.getCollectionId());
        }
    }

    /**
     * 벡터 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param query          질의문
     * @param topK           top K
     * @param alias          필터 코드 목록
     * @return 벡터 검색 결과 목록
     */
    @Override
    public <T extends DocumentEntity> List<Search<T>> vectorSearch(CollectionType collectionType, String query, int topK, List<String> alias) {

        List<SearchVectorQueryVO> vectorQueryVos = collectionType.getVectorSearchFields().stream()
                .map(field -> new SearchVectorQueryVO(field, query, topK))
                .toList();

        StringBuilder filterQueryBuilder = new StringBuilder();

        if (!alias.isEmpty()) {
            filterQueryBuilder.append("<alias:in:");
            alias.forEach(s -> filterQueryBuilder.append(" ").append(s));
            filterQueryBuilder.append(">");
        }

        VectorSearchRequest requestBody = VectorSearchRequest.builder()
                .filterQuery(filterQueryBuilder.toString().trim().isBlank() ? null : filterQueryBuilder.toString())
                .vectorQuery(vectorQueryVos)
                .build();

        ResponseEntity<String> responseEntity = webClient.post()
                .uri(searchProperty.getUrl(collectionType.getCollectionId()))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchangeToMono(response -> response
                        .bodyToMono(String.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        // 응답 체크
        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new SearchErrorException("VECTOR/" + collectionType.getCollectionId());
        }

        try {
            // 역직렬화
            JavaType type = objectMapper.getTypeFactory().constructParametricType(SearchResponse.class, collectionType.getMappingClass());
            SearchResponse<T> responseBody = objectMapper.readValue(responseEntity.getBody(), type);

            // 응답 바디 체크
            if (responseBody == null) {
                throw new SearchErrorException("VECTOR/" + collectionType.getCollectionId());
            }

            return responseBody.getDocument();

        } catch (JsonProcessingException e) {
            throw new SearchErrorException("VECTOR/" + collectionType.getCollectionId());
        }
    }

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
                .field(SearchConst.RERANK_FIELD)
                .document(documents)
                .build();

        ResponseEntity<RerankResponse> responseEntity = webClient.post()
                .uri(rerankerProperty.getUrl())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchangeToMono(response -> response
                        .bodyToMono(RerankResponse.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        // 응답 체크
        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new SearchErrorException("RERANKER/" + rerankerProperty.getModelName());
        }

        RerankResponse responseBody = responseEntity.getBody();

        // 응답 바디 체크
        if (responseBody == null) {
            throw new SearchErrorException("RERANKER/" + rerankerProperty.getModelName());
        }

        return responseBody.getData();
    }
}