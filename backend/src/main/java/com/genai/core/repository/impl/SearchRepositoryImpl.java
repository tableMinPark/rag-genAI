package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.config.properties.EmbedProperty;
import com.genai.core.config.properties.RerankerProperty;
import com.genai.core.config.properties.SearchProperty;
import com.genai.core.exception.SearchErrorException;
import com.genai.core.repository.SearchRepository;
import com.genai.core.repository.entity.DocumentEntity;
import com.genai.core.repository.request.KeywordSearchRequest;
import com.genai.core.repository.request.RerankRequest;
import com.genai.core.repository.request.VectorSearchRequest;
import com.genai.core.repository.response.RerankResponse;
import com.genai.core.repository.response.SearchResponse;
import com.genai.core.repository.vo.ConvertVectorVO;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
import com.genai.core.type.CollectionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SearchRepositoryImpl implements SearchRepository {

    private final WebClient searchWebClient;
    private final WebClient rerankerWebClient;
    private final WebClient embedWebClient;
    private final SearchProperty searchProperty;
    private final RerankerProperty rerankerProperty;
    private final EmbedProperty embedProperty;
    private final ObjectMapper objectMapper;

    public SearchRepositoryImpl(
            @Qualifier("searchWebClient") WebClient searchWebClient,
            @Qualifier("rerankerWebClient") WebClient rerankerWebClient,
            @Qualifier("embedWebClient") WebClient embedWebClient,
            @Autowired SearchProperty searchProperty,
            @Autowired RerankerProperty rerankerProperty,
            @Autowired EmbedProperty embedProperty,
            @Autowired ObjectMapper objectMapper
    ) {
        this.searchWebClient = searchWebClient;
        this.rerankerWebClient = rerankerWebClient;
        this.embedWebClient = embedWebClient;
        this.searchProperty = searchProperty;
        this.rerankerProperty = rerankerProperty;
        this.embedProperty = embedProperty;
        this.objectMapper = objectMapper;
    }

    /**
     * 키워드 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param query          질의문
     * @param topK           top K
     * @param sessionId      세션 식별자
     * @param aliases          필터 코드 목록
     * @return 키워드 검색 결과 목록
     */
    @Override
    public <T extends DocumentEntity> List<Search<T>> keywordSearch(CollectionType collectionType, String query, int topK, String sessionId, List<String> aliases) {

        KeywordSearchRequest keywordSearchRequest = KeywordSearchRequest.builder()
                .size(topK)
                .sort(List.of(KeywordSearchRequest.sort(KeywordSearchRequest.SortField._score, "desc")))
                .query(KeywordSearchRequest.query(KeywordSearchRequest.QueryType.best_fields, query, collectionType.getKeywordSearchFields(), aliases))
                .build();

        ResponseEntity<String> responseEntity = searchWebClient.post()
                .uri(searchProperty.getUrl() + "/" + collectionType.getCollectionId() + "/_search")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(keywordSearchRequest)
                .exchangeToMono(response -> response
                        .bodyToMono(String.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        // 응답 체크
        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new SearchErrorException("keyword/" + collectionType.getCollectionId());
        }

        try {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(SearchResponse.class, collectionType.getMappingClass());
            SearchResponse<T> responseBody = objectMapper.readValue(responseEntity.getBody(), type);

            // 응답 바디 체크
            if (responseBody == null) {
                throw new SearchErrorException("keyword/" + collectionType.getCollectionId());
            }

            return responseBody.getResult().hits();

        } catch (JsonProcessingException e) {
            throw new SearchErrorException("keyword/" + collectionType.getCollectionId());
        }
    }

    /**
     * 벡터 검색 요청
     *
     * @param collectionType 컬렉션 타입
     * @param query          질의문
     * @param topK           top K
     * @param aliases          필터 코드 목록
     * @return 벡터 검색 결과 목록
     */
    @Override
    public <T extends DocumentEntity> List<Search<T>> vectorSearch(CollectionType collectionType, String query, int topK, List<String> aliases) {

        ConvertVectorVO convertVectorVO = ConvertVectorVO.builder()
                .id(Long.MIN_VALUE)
                .content(query)
                .build();

        convertVectorVO = embedWebClient.post()
                .uri(embedProperty.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(convertVectorVO))
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .flatMap(errorBody -> Mono.error(new SearchErrorException("vector/" + collectionType.getCollectionId())))
                )
                .bodyToMono(new ParameterizedTypeReference<List<ConvertVectorVO>>() {})
                .blockOptional()
                .orElseThrow(() -> new SearchErrorException("vector/" + collectionType.getCollectionId()))
                .getFirst();

        VectorSearchRequest vectorSearchRequest = VectorSearchRequest.builder()
                .size(topK)
                .query(VectorSearchRequest.query(
                        collectionType.getVectorSearchFields(),
                        convertVectorVO.getVector(),
                        aliases))
                .build();

        ResponseEntity<String> responseEntity = searchWebClient.post()
                .uri(searchProperty.getUrl() + "/" + collectionType.getCollectionId() + "/_search")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(vectorSearchRequest)
                .exchangeToMono(response -> response
                        .bodyToMono(String.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        // 응답 체크
        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new SearchErrorException("vector/" + collectionType.getCollectionId());
        }

        try {
            JavaType type = objectMapper.getTypeFactory().constructParametricType(SearchResponse.class, collectionType.getMappingClass());
            SearchResponse<T> responseBody = objectMapper.readValue(responseEntity.getBody(), type);

            // 응답 바디 체크
            if (responseBody == null) {
                throw new SearchErrorException("vector/" + collectionType.getCollectionId());
            }

            return responseBody.getResult().hits();

        } catch (JsonProcessingException e) {
            throw new SearchErrorException("vector/" + collectionType.getCollectionId());
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

        if (documents.isEmpty()) return documents;

        RerankRequest requestBody = RerankRequest.builder()
                .query(query)
                .documents(documents.stream()
                        .map(rerank -> RerankRequest.Document.builder()
                                .id(String.valueOf(rerank.getDocument().getChunkId()))
                                .content(rerank.getDocument().getContext())
                                .build())
                        .toList())
                .build();

        ResponseEntity<RerankResponse> responseEntity = rerankerWebClient.post()
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
            throw new SearchErrorException("reranker");
        }

        RerankResponse responseBody = responseEntity.getBody();

        // 응답 바디 체크
        if (responseBody == null) {
            throw new SearchErrorException("reranker");
        }

        Map<Long, Rerank> documentMap = documents.stream()
                .collect(Collectors.toMap(rerank -> rerank.getDocument().getChunkId(), document -> document));

        List<Rerank> rerankDocuments = new ArrayList<>();
        responseBody.getDocuments().forEach(rerankDocumentResponse -> {
            Long chunkId = Long.parseLong(rerankDocumentResponse.id());
            if (documentMap.containsKey(chunkId)) {
                Rerank rerankDocument = documentMap.get(chunkId);
                rerankDocument.setRerankScore(rerankDocumentResponse.score());
                rerankDocuments.add(rerankDocument);
            }
        });

        return rerankDocuments;
    }
}