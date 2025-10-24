package com.genai.adapter.out;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.adapter.out.request.KeywordSearchRequest;
import com.genai.adapter.out.request.RerankRequest;
import com.genai.adapter.out.request.VectorSearchRequest;
import com.genai.adapter.out.response.RerankResponse;
import com.genai.adapter.out.response.SearchResponse;
import com.genai.adapter.out.vo.SortVo;
import com.genai.adapter.out.vo.VectorQueryVo;
import com.genai.application.domain.Document;
import com.genai.application.domain.Rerank;
import com.genai.application.domain.Search;
import com.genai.application.enums.CollectionType;
import com.genai.application.port.SearchPort;
import com.genai.constant.ModelConst;
import com.genai.constant.SearchConst;
import com.genai.exception.ModelErrorException;
import com.genai.exception.SearchErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SearchPortAdapter implements SearchPort {

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