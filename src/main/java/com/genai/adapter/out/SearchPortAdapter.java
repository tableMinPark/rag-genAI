package com.genai.adapter.out;

import com.genai.adapter.out.enums.CollectionType;
import com.genai.adapter.out.request.KeywordSearchRequest;
import com.genai.adapter.out.request.RerankRequest;
import com.genai.adapter.out.request.VectorSearchRequest;
import com.genai.adapter.out.response.RerankResponse;
import com.genai.adapter.out.response.SearchResponse;
import com.genai.adapter.out.vo.SortVo;
import com.genai.adapter.out.vo.VectorQueryVo;
import com.genai.application.domain.Document;
import com.genai.application.domain.Law;
import com.genai.application.domain.RerankDocument;
import com.genai.application.port.SearchPort;
import com.genai.constant.ModelConst;
import com.genai.constant.SearchConst;
import com.genai.exception.ModelErrorException;
import com.genai.exception.SearchErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
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

    /**
     * 검색 결과 리랭킹
     *
     * @param query     질의문
     * @param documents 검색 결과 목록
     * @return 리랭킹 검색 결과 목록
     */
    @Override
    public List<RerankDocument<Law>> lawRerank(String query, List<Document<Law>> documents) {

        RerankRequest<Law> requestBody = RerankRequest.<Law>builder()
                .query(query)
                .field(ModelConst.RERANK_FIELD)
                .document(documents)
                .build();

        RerankResponse<Law> responseBody = webClient.post()
                .uri(RERANKER_URL + "/" + RERANKER_MODEL_NAME)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<RerankResponse<Law>>() {
                })
                .block();

        if (responseBody == null) {
            throw new ModelErrorException("RERANKER(" + RERANKER_MODEL_NAME + ")");
        } else {
            StringBuilder builder = new StringBuilder();
            responseBody.getData().forEach(document ->
                    builder.append(String.format("+ %.4f", document.getRerankScore()))
                            .append(" | ")
                            .append(document.getFields().getContext().replace("\n", "\\n"))
                            .append("\n"));
            log.info("##### 리랭킹 결과 {} 개 | {} #####\n{}", responseBody.getData().size(), query, builder.toString().trim());
        }

        return responseBody.getData();
    }

    /**
     * 법령 컬렉션 키워드 검색 요청
     *
     * @param query     질의문
     * @param topK      top K
     * @param sessionId 세션 식별자
     * @return 키워드 검색 결과 목록
     */
    @Override
    public List<Document<Law>> lawKeywordSearch(String query, int topK, String sessionId) {

        CollectionType collectionType = CollectionType.LAW;

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

        SearchResponse<Law> responseBody = webClient.post()
                .uri(SEARCH_URL + "/" + collectionType.getCollectionId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SearchResponse<Law>>() {})
                .block();

        if (responseBody == null) {
            throw new SearchErrorException("키워드/" + collectionType.getCollectionName());
        } else {
            StringBuilder builder = new StringBuilder();
            responseBody.getDocument().forEach(document ->
                    builder.append(String.format("+ %.4f", document.getScore()))
                            .append(" | ")
                            .append(document.getFields().getContext().replace("\n", "\\n"))
                            .append("\n"));
            log.info("##### 키워드 검색 결과 {} 개 | {}\n{} #####", responseBody.getDocument().size(), query, builder.toString().trim());
        }

        return responseBody.getDocument();
    }

    /**
     * 법령 컬렉션 벡터 검색 요청
     *
     * @param query 질의문
     * @param topK  top K
     * @return 벡터 검색 결과 목록
     */
    @Override
    public List<Document<Law>> lawVectorSearch(String query, int topK) {

        CollectionType collectionType = CollectionType.LAW;

        List<VectorQueryVo> vectorQueries = collectionType.getVectorSearchFields().stream()
                .map(field -> new VectorQueryVo(field, query, topK))
                .toList();

        VectorSearchRequest requestBody = VectorSearchRequest.builder()
                .vectorQuery(vectorQueries)
                .build();

        SearchResponse<Law> responseBody = webClient.post()
                .uri(SEARCH_URL + "/" + collectionType.getCollectionId())
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SearchResponse<Law>>() {})
                .block();

        if (responseBody == null) {
            throw new SearchErrorException("벡터/" + collectionType.getCollectionName());
        } else {
            StringBuilder builder = new StringBuilder();
            responseBody.getDocument().forEach(document ->
                    builder.append(String.format("+ %.4f", document.getScore()))
                            .append(" | ")
                            .append(document.getFields().getContext().replace("\n", "\\n"))
                            .append("\n"));
            log.info("##### 벡터 검색 결과 {} 개 | {}\n{} #####", responseBody.getDocument().size(), query, builder.toString().trim());
        }

        return responseBody.getDocument();
    }
}