package com.genai.core.repository.impl;

import com.genai.core.config.properties.IndexerProperty;
import com.genai.core.config.properties.CollectionProperty;
import com.genai.core.repository.CollectionRepository;
import com.genai.core.repository.entity.CollectionEntity;
import com.genai.core.repository.entity.DocumentEntity;
import com.genai.core.repository.entity.IndexEntity;
import com.genai.core.repository.request.CreateCollectionRequest;
import com.genai.core.repository.response.GetCollectionResponse;
import com.genai.core.repository.response.GetIndexResponse;
import com.genai.core.repository.vo.ConvertVectorVO;
import com.genai.core.exception.CollectionErrorException;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CollectionRepositoryImpl implements CollectionRepository {

    private final WebClient webClient;
    private final CollectionProperty collectionProperty;
    private final IndexerProperty indexerProperty;

    public CollectionRepositoryImpl(
            @Qualifier("collectionWebClient") WebClient webClient,
            @Autowired CollectionProperty collectionProperty,
            @Autowired IndexerProperty indexerProperty
    ) {
        this.webClient = webClient;
        this.collectionProperty = collectionProperty;
        this.indexerProperty = indexerProperty;
    }

    /**
     * 컬렉션 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 컬렉션
     */
    @Override
    public Optional<CollectionEntity> findCollectionByCollectionId(String collectionId) {

        ResponseEntity<GetCollectionResponse> responseEntity = webClient.get()
                .uri(collectionProperty.getSelectUrl(collectionId))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response
                        .bodyToMono(GetCollectionResponse.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful()) {
            return Optional.empty();
        }

        GetCollectionResponse responseBody = responseEntity.getBody();

        if (responseBody == null) {
            return Optional.empty();
        }

        return Optional.of(
                CollectionEntity.builder()
                        .collectionId(responseBody.getCollectionId())
                        .numOfShards(responseBody.getNumOfShards())
                        .numOfReplication(responseBody.getNumOfReplication())
                        .numOfIndexThreads(responseBody.getNumOfIndexThreads())
                        .searchRoutingStrategy(responseBody.getSearchRoutingStrategy())
                        .recentQuery(responseBody.getRecentQuery())
                        .maxQueryCacheCount(responseBody.getMaxQueryCacheCount())
                        .maxQueryCacheRamBytesUsed(responseBody.getMaxQueryCacheRamBytesUsed())
                        .mappingTableId(responseBody.getMappingTableId())
                        .fields(responseBody.getFields())
                        .indexFields(responseBody.getIndexFields())
                        .advancedSettings(responseBody.getAdvancedSettings())
                        .build()
        );
    }

    /**
     * 컬렉션 등록
     *
     * @param collectionId     컬렉션 ID
     * @param collectionEntity 컬렉션
     * @return 컬렉션
     */
    @Override
    public CollectionEntity createCollection(String collectionId, CollectionEntity collectionEntity) {

        // 컬렉션 생성 요청 바디
        CreateCollectionRequest requestBody = CreateCollectionRequest.builder()
                .numOfShards(collectionEntity.getNumOfShards())
                .numOfReplication(collectionEntity.getNumOfReplication())
                .numOfIndexThreads(collectionEntity.getNumOfIndexThreads())
                .searchRoutingStrategy(collectionEntity.getSearchRoutingStrategy())
                .recentQuery(collectionEntity.getRecentQuery())
                .maxQueryCacheCount(collectionEntity.getMaxQueryCacheCount())
                .maxQueryCacheRamBytesUsed(collectionEntity.getMaxQueryCacheRamBytesUsed())
                .mappingTableId(collectionEntity.getMappingTableId())
                .fields(collectionEntity.getFields())
                .indexFields(collectionEntity.getIndexFields())
                .advancedSettings(collectionEntity.getAdvancedSettings())
                .build();

        ResponseEntity<GetCollectionResponse> responseEntity = webClient.post()
                .uri(collectionProperty.getCreateUrl(collectionId))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchangeToMono(response -> response
                        .bodyToMono(GetCollectionResponse.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new CollectionErrorException(collectionId);
        }

        GetCollectionResponse responseBody = responseEntity.getBody();

        if (responseBody == null) {
            throw new CollectionErrorException(collectionId);
        }

        return CollectionEntity.builder()
                .collectionId(responseBody.getCollectionId())
                .numOfShards(responseBody.getNumOfShards())
                .numOfReplication(responseBody.getNumOfReplication())
                .numOfIndexThreads(responseBody.getNumOfIndexThreads())
                .searchRoutingStrategy(responseBody.getSearchRoutingStrategy())
                .recentQuery(responseBody.getRecentQuery())
                .maxQueryCacheCount(responseBody.getMaxQueryCacheCount())
                .maxQueryCacheRamBytesUsed(responseBody.getMaxQueryCacheRamBytesUsed())
                .mappingTableId(responseBody.getMappingTableId())
                .fields(responseBody.getFields())
                .indexFields(responseBody.getIndexFields())
                .advancedSettings(responseBody.getAdvancedSettings())
                .build();
    }

    /**
     * 컬렉션 삭제
     *
     * @param collectionId 컬렉션 ID
     */
    @Override
    public void deleteCollection(String collectionId) {

        ResponseEntity<Map<String, Object>> responseEntity = webClient.post()
                .uri(collectionProperty.getDeleteUrl(collectionId))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new CollectionErrorException(collectionId);
        }
    }

    /**
     * 색인 정보 조회
     *
     * @param collectionId 컬렉션 ID
     * @return 색인 엔티티
     */
    @Override
    public Optional<IndexEntity> findIndexByCollectionId(String collectionId) {

        ResponseEntity<GetIndexResponse> responseEntity = webClient.get()
                .uri(indexerProperty.getSelectUrl(collectionId))
                .accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> response
                        .bodyToMono(GetIndexResponse.class)
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful()) {
            return Optional.empty();
        }

        GetIndexResponse responseBody = responseEntity.getBody();

        if (responseBody == null) {
            return Optional.empty();
        }

        return Optional.of(
                IndexEntity.builder()
                        .totalDocs(responseBody.getTotalDocs())
                        .totalSize(responseBody.getTotalSize())
                        .enableShards(responseBody.getEnableShards())
                        .disableShards(responseBody.getDisableShards())
                        .build()
        );
    }

    /**
     * 벡터 변환
     *
     * @param collectionId     컬렉션 ID
     * @param documentEntities 변환 대상 문서 목록
     * @return 변환 완료 문서 목록
     */
    @Override
    public List<DocumentEntity> convertVector(String collectionId, List<DocumentEntity> documentEntities) {

        List<ConvertVectorVO> convertVectorVos = documentEntities.stream()
                .map(documentEntity -> ConvertVectorVO.builder()
                        .chunkId(documentEntity.getChunkId())
                        .context(documentEntity.getContext())
                        .contextVector(documentEntity.getContextVector())
                        .build())
                .toList();

        // TODO: 최적화 작업 필요 (nested exception is org.springframework.core.io.buffer.DataBufferLimitException: Exceeded limit on max bytes to buffer : 10485760)
        convertVectorVos = webClient.post()
                .uri(indexerProperty.getConvertVectorUrl(collectionId))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(convertVectorVos)
                .retrieve()
                .onStatus(HttpStatus::isError, response ->
                        response.bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                                .flatMap(errorBody -> Mono.error(new CollectionErrorException(collectionId)))
                )
                .bodyToMono(new ParameterizedTypeReference<List<ConvertVectorVO>>() {})
                .blockOptional()
                .orElseThrow(() -> new CollectionErrorException(collectionId));

        Map<String, ConvertVectorVO> convertVectorVoMap = convertVectorVos.stream()
                .collect(Collectors.toMap(ConvertVectorVO::getChunkId, convertVectorVo -> convertVectorVo));

        return documentEntities.stream()
                .peek(documentEntity -> {
                    ConvertVectorVO convertVectorVo = convertVectorVoMap.get(documentEntity.getChunkId());
                    String convertVector = convertVectorVo != null ? convertVectorVo.getContextVector() : "";
                    documentEntity.setContextVector(convertVector);
                })
                .toList();
    }

    /**
     * 색인 생성
     *
     * @param collectionId     컬렉션 ID
     * @param documentEntities 색인 대상 문서 목록
     * @param isStatic         정적 색인 여부
     */
    @Override
    public void createIndex(String collectionId, List<DocumentEntity> documentEntities, boolean isStatic) {

        // 정적 색인 전환
        if (isStatic) {
            ResponseEntity<Map<String, Object>> responseEntity = webClient.post()
                    .uri(indexerProperty.getReadyStaticUrl(collectionId))
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .exchangeToMono(response -> response
                            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                            })
                            .map(body -> new ResponseEntity<>(body, response.statusCode())))
                    .block();

            if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
                throw new CollectionErrorException(collectionId);
            }
        }

        ResponseEntity<Map<String, Object>> responseEntity = webClient.post()
                .uri(indexerProperty.getCreateIndexUrl(collectionId))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(documentEntities)
                .exchangeToMono(response -> response
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseEntity == null || !responseEntity.getStatusCode().is2xxSuccessful() || responseEntity.getBody() == null) {
            throw new CollectionErrorException(collectionId);
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

        ResponseEntity<Map<String, Object>> responseEntity = webClient.post()
                .uri(indexerProperty.getDeleteIndexUrl(collectionId))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(chunkIds)
                .exchangeToMono(response -> response
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .map(body -> new ResponseEntity<>(body, response.statusCode())))
                .block();

        if (responseEntity == null || responseEntity.getBody() == null) {
            throw new CollectionErrorException(collectionId);
        }
    }
}