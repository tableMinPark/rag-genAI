package com.genai.core.service.business.impl;

import com.genai.core.constant.EmbedConst;
import com.genai.core.config.properties.ChunkProperty;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.CollectionRepository;
import com.genai.core.repository.FileRepository;
import com.genai.core.repository.SourceRepository;
import com.genai.core.repository.entity.*;
import com.genai.core.service.business.EmbedCoreService;
import com.genai.core.type.CollectionType;
import com.genai.global.utils.ExtractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbedCoreServiceImpl implements EmbedCoreService {

    private final FileRepository fileRepository;
    private final SourceRepository sourceRepository;
    private final CollectionRepository collectionRepository;
    private final ExtractUtil extractUtil;
    private final ChunkProperty chunkProperty;

    /**
     * 임베딩 문서 동기화
     *
     * @param collectionType 컬렉션 타입
     * @param fileId 파일 ID
     * @param categoryCode 카테고리 코드
     */
    @Transactional
    @Override
    public void syncEmbedSources(CollectionType collectionType, long fileId, String categoryCode) {

        // 버전 코드 생성
        Long version = System.currentTimeMillis();

        // 컬렉션 존재 여부 확인
        collectionRepository.findCollectionByCollectionId(collectionType.getCollectionId())
                .orElseThrow(() -> new NotFoundException(collectionType.getCollectionId()));

        // 임베딩 대상 파일 목록 조회
        List<FileDetailEntity> fileDetailEntities = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException(String.valueOf(fileId)))
                .getFileDetails();

        // 색인 대상 목록
        List<DocumentEntity> indexDocumentEntities = new ArrayList<>();
        for (FileDetailEntity fileDetailEntity : fileDetailEntities) {
            String fullPath = fileDetailEntity.getUrl();

            // 파일 존재 여부 체크
            if (!new File(fullPath).exists()) {
                throw new NotFoundException(fullPath);
            }

            // SNF 문자열 추출
            String content = extractUtil.extract(fullPath);

            int step = chunkProperty.getTokenSize() - chunkProperty.getOverlapSize();
            List<String> chunks = IntStream.iterate(0, i -> i + step)
                    .limit((content.length() + step - 1) / step)
                    .mapToObj(i -> content.substring(i, Math.min(content.length(), i + chunkProperty.getTokenSize())))
                    .toList();

            // 패시지 목록
            List<PassageEntity> passageEntities = new ArrayList<>();
            for (String chunk : chunks) {
                ChunkEntity chunkEntity = ChunkEntity.builder()
                        .version(version)
                        .title(fileDetailEntity.getFileOriginName())
                        .subTitle("")
                        .thirdTitle("")
                        .content(chunk)
                        .compactContent(chunk)
                        .subContent("")
                        .tokenSize(chunk.length())
                        .compactTokenSize(chunk.length())
                        .build();

                PassageEntity passageEntity = PassageEntity.builder()
                        .version(version)
                        .sortOrder(passageEntities.size())
                        .title(fileDetailEntity.getFileOriginName())
                        .subTitle("")
                        .thirdTitle("")
                        .content(chunk)
                        .subContent("")
                        .tokenSize(chunk.length())
                        .updateState(EmbedConst.EMBED_UPDATE_STATE)
                        .chunks(List.of(chunkEntity))
                        .build();

                passageEntities.add(passageEntity);
            }

            // 현재 문서 DB 등록
            SourceEntity sourceEntity = sourceRepository.save(SourceEntity.builder()
                    .version(version)
                    .sourceType(EmbedConst.EMBED_SOURCE_TYPE)
                    .categoryCode(categoryCode)
                    .name(fileDetailEntity.getFileOriginName())
                    .content(content)
                    .collectionId(collectionType.getCollectionId())
                    .fileDetailId(fileDetailEntity.getFileDetailId())
                    .maxTokenSize(chunkProperty.getTokenSize())
                    .overlapSize(chunkProperty.getOverlapSize())
                    .isAuto(false)
                    .isBatch(false)
                    .passages(passageEntities)
                    .build());

            // 색인 문서 생성
            List<DocumentEntity> documentEntities = new ArrayList<>();
            for (PassageEntity passageEntity : sourceEntity.getPassages()) {
                for (ChunkEntity chunkEntity : passageEntity.getChunks()) {
                    String context = chunkEntity.getTitle() + "\n" +
                            chunkEntity.getSubTitle()       + "\n" +
                            chunkEntity.getThirdTitle()     + "\n" +
                            chunkEntity.getContent()        + "\n" +
                            chunkEntity.getSubContent()     + "\n";

                    documentEntities.add(DocumentEntity.builder()
                            .chunkId(chunkEntity.getChunkId())
                            .passageId(passageEntity.getPassageId())
                            .sourceId(sourceEntity.getSourceId())
                            .fileDetailId(sourceEntity.getFileDetailId())
                            .originFileName(fileDetailEntity.getFileOriginName())
                            .name(sourceEntity.getName())
                            .title(chunkEntity.getTitle())
                            .subTitle(chunkEntity.getSubTitle())
                            .thirdTitle(chunkEntity.getThirdTitle())
                            .compactContent(chunkEntity.getContent())
                            .content(chunkEntity.getContent())
                            .subContent(chunkEntity.getSubContent())
                            .context(context.trim())
                            .url(fileDetailEntity.getUrl())
                            .categoryCode(sourceEntity.getCategoryCode())
                            .sourceType(sourceEntity.getSourceType())
                            .ext(fileDetailEntity.getExt())
                            .sysCreateDt(chunkEntity.getSysCreateDt())
                            .sysModifyDt(chunkEntity.getSysModifyDt())
                            .alias(sourceEntity.getCategoryCode())
                            .build());
                }
            }

            // 벡터 변환
            indexDocumentEntities.addAll(collectionRepository.convertVector(collectionType.getCollectionId(), documentEntities));
        }

        // 멀티플 색인
        collectionRepository.createIndex(collectionType.getCollectionId(), indexDocumentEntities);

        // 현재 문서 파일 상세 ID 목록
        List<Long> fileDetailIds = fileDetailEntities.stream()
                .map(FileDetailEntity::getFileDetailId)
                .toList();

        // 삭제 예정 문서 목록 조회
        List<SourceEntity> deleteSourceEntities =
                sourceRepository.findByCollectionIdAndFileDetailIdNotIn(collectionType.getCollectionId(), fileDetailIds);

        // 삭제 대상 ID 목록
        List<String> deleteDocumentIds = new ArrayList<>();
        deleteSourceEntities.forEach(previousSourceEntity -> {
            previousSourceEntity.getPassages().forEach(passageEntity -> {
                passageEntity.getChunks().forEach(chunkEntity -> {
                    deleteDocumentIds.add(String.valueOf(chunkEntity.getChunkId()));
                });
            });
        });

        // 색인 문서 삭제
        collectionRepository.deleteIndex(collectionType.getCollectionId(), deleteDocumentIds);

        // 대상 문서 삭제
        sourceRepository.deleteAll(deleteSourceEntities);
    }

    /**
     * 임베딩 문서 삭제
     * @param collectionType 컬렉션 타입
     * @param fileId 파일 ID
     */
    @Transactional
    @Override
    public void deleteEmbedSources(CollectionType collectionType, long fileId) {

        // 임베딩 삭제 대상 파일 목록 조회
        List<Long> fileDetailIds = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException(String.valueOf(fileId)))
                .getFileDetails().stream()
                .map(FileDetailEntity::getFileDetailId)
                .toList();

        // 삭제 예정 문서 목록 조회
        List<SourceEntity> sourceEntities =
                sourceRepository.findByCollectionIdAndFileDetailIdIn(collectionType.getCollectionId(), fileDetailIds);

        // 삭제 대상 ID 목록
        List<String> deleteDocumentIds = new ArrayList<>();
        sourceEntities.forEach(sourceEntity -> {
            sourceEntity.getPassages().forEach(passageEntity -> {
                passageEntity.getChunks().forEach(chunkEntity -> {
                    deleteDocumentIds.add(String.valueOf(chunkEntity.getChunkId()));
                });
            });
        });

        // 컬렉션 존재 여부 확인
        collectionRepository.findCollectionByCollectionId(collectionType.getCollectionId()).ifPresent(collectionEntity -> {
            // 색인 문서 삭제
            collectionRepository.deleteIndex(collectionType.getCollectionId(), deleteDocumentIds);
        });

        // 대상 문서 삭제
        sourceRepository.deleteAll(sourceEntities);
    }
}
