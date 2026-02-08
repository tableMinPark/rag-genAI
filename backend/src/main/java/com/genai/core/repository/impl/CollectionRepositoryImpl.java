package com.genai.core.repository.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.config.properties.CollectionProperty;
import com.genai.core.config.properties.EmbedProperty;
import com.genai.core.config.properties.IndexerProperty;
import com.genai.core.exception.CollectionErrorException;
import com.genai.core.repository.CollectionRepository;
import com.genai.core.repository.entity.CollectionEntity;
import com.genai.core.repository.entity.DocumentEntity;
import com.genai.core.repository.request.CreateIndexBulkRequest;
import com.genai.core.repository.request.DeleteIndexBulkRequest;
import com.genai.core.repository.response.CreateIndexBulkResponse;
import com.genai.core.repository.response.DeleteIndexBulkResponse;
import com.genai.core.repository.response.GetCollectionResponse;
import com.genai.core.repository.vo.ConvertVectorVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CollectionRepositoryImpl implements CollectionRepository {

    private final WebClient collectionWebClient;
    private final WebClient indexerWebClient;
    private final WebClient embedWebClient;
    private final CollectionProperty collectionProperty;
    private final IndexerProperty indexerProperty;
    private final EmbedProperty embedProperty;
    private final ObjectMapper objectMapper;

    public CollectionRepositoryImpl(
            @Qualifier("collectionWebClient") WebClient collectionWebClient,
            @Qualifier("indexerWebClient") WebClient indexerWebClient,
            @Qualifier("embedWebClient") WebClient embedWebClient,
            @Autowired CollectionProperty collectionProperty,
            @Autowired IndexerProperty indexerProperty,
            @Autowired EmbedProperty embedProperty,
            @Autowired ObjectMapper objectMapper
    ) {
        this.collectionWebClient = collectionWebClient;
        this.indexerWebClient = indexerWebClient;
        this.embedWebClient = embedWebClient;
        this.collectionProperty = collectionProperty;
        this.indexerProperty = indexerProperty;
        this.embedProperty = embedProperty;
        this.objectMapper = objectMapper;
    }

    /**
     * 컬렉션 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 컬렉션
     */
    @Override
    public Optional<CollectionEntity> findCollectionByCollectionId(String collectionId) {

        ResponseEntity<Map<String, Object>> responseEntity = collectionWebClient.get()
                .uri(collectionProperty.getUrl() + "/" + collectionId)
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful()) {
            return Optional.empty();
        }

        Map<String, Object> responseBody = responseEntity.getBody();

        if (responseBody == null || !responseBody.containsKey(collectionId)) {
            return Optional.empty();
        }

        GetCollectionResponse getCollectionResponse = objectMapper.convertValue(responseBody.get(collectionId), GetCollectionResponse.class);

        return Optional.of(CollectionEntity.builder()
                .collectionId(collectionId)
                .numOfShards(Integer.parseInt(getCollectionResponse.getSettings().getIndex().getNumberOfShards()))
                .numOfReplication(Integer.parseInt(getCollectionResponse.getSettings().getIndex().getNumberOfReplicas()))
                .fields(getCollectionResponse.getMappings().getProperties().keySet().stream().toList())
                .build());
    }

    /**
     * 벡터 변환
     *
     * @param collectionId     컬렉션 ID
     * @param documentEntities 변환 대상 문서 목록
     * @return 변환 완료 문서 목록
     */
    @Transactional
    @Override
    public List<DocumentEntity> convertVector(String collectionId, List<DocumentEntity> documentEntities) {

        if (documentEntities.isEmpty()) return documentEntities;

        List<ConvertVectorVO> convertVectorVos = documentEntities.stream()
                .map(documentEntity -> ConvertVectorVO.builder()
                        .id(documentEntity.getChunkId())
                        .content(documentEntity.getContext())
                        .build())
                .toList();

        convertVectorVos = embedWebClient.post()
                .uri(embedProperty.getUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(convertVectorVos)
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                })
                                .flatMap(errorBody -> Mono.error(new CollectionErrorException(collectionId)))
                )
                .bodyToMono(new ParameterizedTypeReference<List<ConvertVectorVO>>() {
                })
                .blockOptional()
                .orElseThrow(() -> new CollectionErrorException(collectionId));

        Map<Long, ConvertVectorVO> convertVectorVoMap = convertVectorVos.stream()
                .collect(Collectors.toMap(ConvertVectorVO::getId, convertVectorVo -> convertVectorVo));

        return documentEntities.stream()
                .peek(documentEntity -> {
                    ConvertVectorVO convertVectorVo = convertVectorVoMap.get(documentEntity.getChunkId());
                    List<Float> vector = convertVectorVo != null ? convertVectorVo.getVector() : Collections.emptyList();
                    documentEntity.setContextVector(vector);
                })
                .toList();
    }

    /**
     * 색인 생성
     *
     * @param collectionId     컬렉션 ID
     * @param documentEntities 색인 대상 문서 목록
     */
    @Override
    public void createIndex(String collectionId, List<DocumentEntity> documentEntities) {

        if (documentEntities.isEmpty()) return;

        int batchSize = 500;
        for (int batchIndex = 0; batchIndex < documentEntities.size(); batchIndex += batchSize) {
            StringBuilder requestBodyJsonBuilder = new StringBuilder();

            for (int documentIndex = batchIndex; documentIndex < Math.min(documentEntities.size(), batchIndex + batchSize); documentIndex++) {
                try {
                    DocumentEntity documentEntity = documentEntities.get(documentIndex);

                    CreateIndexBulkRequest createIndexBulkRequest = CreateIndexBulkRequest.builder()
                            .index(CreateIndexBulkRequest.Index.builder()
                                    .collectionId(collectionId)
                                    .id(String.valueOf(documentEntity.getChunkId()))
                                    .build())
                            .build();

                    String createIndexBulkRequestJson = objectMapper.writeValueAsString(createIndexBulkRequest);
                    String documentEntityJson = objectMapper.writeValueAsString(documentEntity);

                    requestBodyJsonBuilder.append(createIndexBulkRequestJson).append("\n");
                    requestBodyJsonBuilder.append(documentEntityJson).append("\n");

                } catch (JsonProcessingException ignored) {
                }
            }

            CreateIndexBulkResponse responseBody = indexerWebClient.put()
                    .uri(indexerProperty.getUrl() + "/_bulk")
                    .contentType(MediaType.APPLICATION_NDJSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBodyJsonBuilder.toString())
                    .retrieve()
                    .onStatus(HttpStatus::isError, response ->
                            response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                    })
                                    .flatMap(errorBody -> Mono.error(new CollectionErrorException(collectionId)))
                    )
                    .bodyToMono(CreateIndexBulkResponse.class)
                    .blockOptional()
                    .orElseThrow(() -> new CollectionErrorException(collectionId));

            if (responseBody.getErrors()) {
                throw new CollectionErrorException(collectionId);
            }
        }
    }

    /**
     * 색인 데이터 삭제
     *
     * @param collectionId 컬렉션 ID
     * @param chunkIds     chunkId 목록
     */
    @Override
    public void deleteIndex(String collectionId, List<String> chunkIds) {

        if (chunkIds.isEmpty()) return;

        int batchSize = 500;
        for (int batchIndex = 0; batchIndex < chunkIds.size(); batchIndex += batchSize) {
            StringBuilder requestBodyJsonBuilder = new StringBuilder();

            for (int chunkIdIndex = batchIndex; chunkIdIndex < Math.min(chunkIds.size(), batchIndex + batchSize); chunkIdIndex++) {
                try {
                    DeleteIndexBulkRequest deleteIndexBulkRequest = DeleteIndexBulkRequest.builder()
                            .delete(DeleteIndexBulkRequest.Delete.builder()
                                    .id(String.valueOf(chunkIds.get(chunkIdIndex)))
                                    .build())
                            .build();

                    String deleteIndexBulkRequestJson = objectMapper.writeValueAsString(deleteIndexBulkRequest);

                    requestBodyJsonBuilder.append(deleteIndexBulkRequestJson).append("\n");

                } catch (JsonProcessingException ignored) {
                }
            }

            DeleteIndexBulkResponse responseBody = indexerWebClient.post()
                    .uri(indexerProperty.getUrl() + "/" + collectionId + "/_bulk")
                    .contentType(MediaType.APPLICATION_NDJSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBodyJsonBuilder.toString())
                    .retrieve()
                    .onStatus(HttpStatus::isError, response ->
                            response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                    })
                                    .flatMap(errorBody -> Mono.error(new CollectionErrorException(collectionId)))
                    )
                    .bodyToMono(DeleteIndexBulkResponse.class)
                    .blockOptional()
                    .orElseThrow(() -> new CollectionErrorException(collectionId));

            if (responseBody.getErrors()) {
                throw new CollectionErrorException(collectionId);
            }
        }
    }
}