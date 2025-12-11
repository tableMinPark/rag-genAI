package com.genai.core.service.impl;

import com.genai.core.config.constant.EmbedConst;
import com.genai.core.config.properties.EmbedProperty;
import com.genai.core.repository.CollectionRepository;
import com.genai.core.repository.FileRepository;
import com.genai.core.repository.SourceRepository;
import com.genai.core.repository.entity.*;
import com.genai.core.service.EmbedCoreService;
import com.genai.core.service.vo.SourceVO;
import com.genai.core.type.CollectionType;
import com.genai.core.type.CollectionTypeFactory;
import com.genai.core.utils.ExtractUtil;
import com.genai.exception.NotFoundException;
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
    private final EmbedProperty embedProperty;
    private final CollectionTypeFactory collectionTypeFactory;

    /**
     * 나만의 AI 문서 임베딩
     *
     * @param projectId 프로젝트 ID
     * @param fileId    파일 ID
     */
    @Transactional
    @Override
    public void embedMyAiSource(long projectId, long fileId) throws NotFoundException {

        CollectionType collectionType = collectionTypeFactory.myai();

        // 임베딩 대상 파일 목록 조회
        List<FileDetailEntity> fileDetailEntities = fileRepository.findAllByFileId(fileId)
                .orElseThrow(() -> new NotFoundException(String.valueOf(fileId)))
                .getFileDetails();

        List<SourceVO> sourceVos = fileDetailEntities.stream()
                .map(fileDetailEntity -> {
                    String fullPath = fileDetailEntity.getUrl();

                    // 파일 존재 여부 체크
                    if (!new File(fullPath).exists()) {
                        throw new NotFoundException(fullPath);
                    }

                    // SNF 문자열 추출
                    String content = extractUtil.extract(fullPath);

                    return SourceVO.builder()
                            .fileDetailId(fileDetailEntity.getFileDetailId())
                            .fileOriginName(fileDetailEntity.getFileOriginName())
                            .url(fileDetailEntity.getUrl())
                            .ext(fileDetailEntity.getExt())
                            .sourceType(EmbedConst.MYAI_SOURCE_TYPE)
                            .categoryCode(EmbedConst.MYAI_CATEGORY_CODE(projectId))
                            .content(content)
                            .build();

                })
                .toList();

        // 임베딩
        this.embedSources(collectionType, sourceVos);
    }

    /**
     * 나만의 AI 문서 삭제
     *
     * @param projectId 프로젝트 ID
     */
    @Transactional
    @Override
    public void deleteEmbeddedMyAiSource(long projectId) throws NotFoundException {

        CollectionType collectionType = collectionTypeFactory.myai();

        List<SourceEntity> sourceEntities = sourceRepository.findByCollectionIdAndCategoryCode(collectionType.getCollectionId(), EmbedConst.MYAI_CATEGORY_CODE(projectId));

        List<String> documentIds = new ArrayList<>();
        sourceEntities.forEach(sourceEntity -> {
            sourceEntity.getPassages().forEach(passageEntity -> {
                passageEntity.getChunks().forEach(chunkEntity -> {
                   documentIds.add(String.valueOf(chunkEntity.getChunkId()));
               });
            });
        });

        // 컬렉션 존재 여부 확인
        collectionRepository.findCollectionByCollectionId(collectionType.getCollectionId()).ifPresent(collectionEntity -> {
            // 색인 문서 삭제
            collectionRepository.deleteIndex(collectionType.getCollectionId(), documentIds);
        });
    }

    /**
     * 문서 정적 임베딩
     *
     * @param collectionType 컬렉션
     * @param sourceVos      임베딩 문서 Vo 목록
     */
    @Transactional
    @Override
    public void embedSources(CollectionType collectionType, List<SourceVO> sourceVos) {
        // 컬렉션 존재 여부 확인
        collectionRepository.findCollectionByCollectionId(collectionType.getCollectionId())
                .orElseThrow(() -> new NotFoundException(collectionType.getCollectionId()));

        Long version = System.currentTimeMillis();

        for (SourceVO sourceVo : sourceVos) {
            String content = sourceVo.getContent();

            // TODO: 토큰 기준 청킹 로직 수정 필요
            int step = embedProperty.getTokenSize() - embedProperty.getOverlapSize();
            List<String> chunks = IntStream.iterate(0, i -> i + step)
                    .limit((content.length() + step - 1) / step)
                    .mapToObj(i -> content.substring(i, Math.min(content.length(), i + embedProperty.getTokenSize())))
                    .toList();

            List<PassageEntity> passageEntities = new ArrayList<>();

            for (String chunk : chunks) {
                ChunkEntity chunkEntity = ChunkEntity.builder()
                        .version(version)
                        .title(sourceVo.getFileOriginName())
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
                        .title(sourceVo.getFileOriginName())
                        .subTitle("")
                        .thirdTitle("")
                        .content(chunk)
                        .subContent("")
                        .tokenSize(chunk.length())
                        .updateState("UPDATE-STATE-INSERT")
                        .chunks(List.of(chunkEntity))
                        .build();

                passageEntities.add(passageEntity);
            }

            SourceEntity sourceEntity = SourceEntity.builder()
                    .version(version)
                    .sourceType(sourceVo.getSourceType())
                    .categoryCode(sourceVo.getCategoryCode())
                    .name(sourceVo.getFileOriginName())
                    .content(content)
                    .collectionId(collectionType.getCollectionId())
                    .fileDetailId(sourceVo.getFileDetailId())
                    .maxTokenSize(embedProperty.getTokenSize())
                    .overlapSize(embedProperty.getOverlapSize())
                    .isAuto(false)
                    .passages(passageEntities)
                    .build();

            // 현재 문서 DB 등록
            SourceEntity persistSourceEntity = sourceRepository.save(sourceEntity);

            // 색인 문서 생성
            List<DocumentEntity> documentEntities = new ArrayList<>();
            persistSourceEntity.getPassages().forEach(passageEntity -> {
                passageEntity.getChunks().forEach(chunkEntity -> {

                    String context = chunkEntity.getTitle() + "\n" +
                            chunkEntity.getSubTitle()       + "\n" +
                            chunkEntity.getThirdTitle()     + "\n" +
                            chunkEntity.getContent()        + "\n" +
                            chunkEntity.getSubContent()     + "\n";

                    documentEntities.add(DocumentEntity.builder()
                            .chunkId(String.valueOf(chunkEntity.getChunkId()))
                            .sourceId(String.valueOf(sourceEntity.getSourceId()))
                            .name(sourceEntity.getName())
                            .title(chunkEntity.getTitle())
                            .subTitle(chunkEntity.getSubTitle())
                            .thirdTitle(chunkEntity.getThirdTitle())
                            .compactContent(chunkEntity.getContent())
                            .content(chunkEntity.getContent())
                            .subContent(chunkEntity.getSubContent())
                            .categoryCode(sourceEntity.getCategoryCode())
                            .sourceType(sourceEntity.getSourceType())
                            .version(sourceEntity.getVersion())
                            .tokenSize(chunkEntity.getTokenSize())
                            .fileDetailId(sourceEntity.getFileDetailId())
                            .originFileName(sourceVo.getFileOriginName())
                            .url(sourceVo.getUrl())
                            .ext(sourceVo.getExt())
                            .sysCreateDt(chunkEntity.getSysCreateDt())
                            .sysModifyDt(chunkEntity.getSysModifyDt())
                            .alias(sourceEntity.getCategoryCode())
                            .context(context.trim())
                            .build());
                });
            });

            // 삭제 및 삭제된 문서 목록 조회
            List<SourceEntity> previousSourceEntities = sourceRepository.findByCollectionIdAndCategoryCodeAndVersion(
                    collectionType.getCollectionId(), persistSourceEntity.getCategoryCode(), persistSourceEntity.getVersion());

            List<String> documentIds = new ArrayList<>();
            previousSourceEntities.forEach(previousSourceEntity -> {
                previousSourceEntity.getPassages().forEach(passageEntity -> {
                    passageEntity.getChunks().forEach(chunkEntity -> {
                        documentIds.add(String.valueOf(chunkEntity.getChunkId()));
                    });
                });
            });

            // 벡터 변환
            List<DocumentEntity> convertVectorDocumentEntities = collectionRepository.convertVector(collectionType.getCollectionId(), documentEntities);

            // 멀티플 색인
            collectionRepository.createIndex(collectionType.getCollectionId(), convertVectorDocumentEntities, false);

            // 색인 문서 삭제
            collectionRepository.deleteIndex(collectionType.getCollectionId(), documentIds);
        }
    }
}
