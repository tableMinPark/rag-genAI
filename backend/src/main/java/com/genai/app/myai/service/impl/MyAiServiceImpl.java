package com.genai.app.myai.service.impl;

import com.genai.app.myai.constant.MyAiConst;
import com.genai.app.myai.repository.ProjectRepository;
import com.genai.app.myai.repository.entity.ProjectEntity;
import com.genai.app.myai.service.MyAiService;
import com.genai.app.myai.service.vo.ProjectVO;
import com.genai.core.config.properties.FileProperty;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.service.business.EmbedCoreService;
import com.genai.core.service.business.PromptCoreService;
import com.genai.core.service.business.vo.FileDetailVO;
import com.genai.core.type.CollectionType;
import com.genai.common.utils.FileUtil;
import com.genai.common.vo.UploadFileVO;
import com.genai.global.wrapper.PageWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyAiServiceImpl implements MyAiService {

    private final ProjectRepository projectRepository;
    private final EmbedCoreService embedCoreService;
    private final FileRepository fileRepository;
    private final FileProperty fileProperty;
    private final PromptRepository promptRepository;
    private final SourceRepository sourceRepository;
    private final ChunkRepository chunkRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final PromptCoreService promptCoreService;

    /**
     * 프로젝트 조회
     *
     * @param projectId 프로젝트 ID
     * @return 프로젝트
     */
    @Transactional(readOnly = true)
    @Override
    public ProjectVO getProject(long projectId) {

        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트"));

        List<Long> fileDetailIds = projectEntity.getFile().getFileDetails().stream()
                .map(FileDetailEntity::getFileDetailId)
                .toList();

        List<SourceEntity> sourceEntities = sourceRepository.findByFileDetailIdIn(fileDetailIds);

        int sourceCount = sourceEntities.size();
        int chunkCount = 0;
        for (SourceEntity sourceEntity : sourceEntities) {
            for (PassageEntity passageEntity : sourceEntity.getPassages()) {
                chunkCount += passageEntity.getChunks().size();
            }
        }

        return ProjectVO.builder()
                .projectId(projectEntity.getProjectId())
                .projectName(projectEntity.getProjectName())
                .projectDesc(projectEntity.getProjectDesc())
                .sysCreateDt(projectEntity.getSysCreateDt())
                .sysModifyDt(projectEntity.getSysModifyDt())
                .fileId(projectEntity.getFile().getFileId())
                .promptId(projectEntity.getPrompt().getPromptId())
                .sourceCount(sourceCount)
                .chunkCount(chunkCount)
                .build();
    }

    /**
     * 프로젝트 목록 조회
     *
     * @param page    페이지
     * @param size    사이즈
     * @param keyword 키워드
     * @return 프로젝트 목록
     */
    @Transactional(readOnly = true)
    @Override
    public PageWrapper<ProjectVO> getProjects(int page, int size, String keyword) {

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "projectId"));

        Page<ProjectEntity> projectEntityPage;

        if (keyword == null) {
            projectEntityPage = projectRepository.findAll(pageable);
        } else {
            projectEntityPage = projectRepository.findAllByProjectNameLike("%" + keyword.replace(" ", "%") + "%", pageable);
        }

        return PageWrapper.<ProjectVO>builder()
                .content(projectEntityPage.getContent().stream()
                        .map(projectEntity -> {

                            List<Long> fileDetailIds = projectEntity.getFile().getFileDetails().stream()
                                    .map(FileDetailEntity::getFileDetailId)
                                    .toList();

                            List<SourceEntity> sourceEntities = sourceRepository.findByFileDetailIdIn(fileDetailIds);
                            List<Long> sourceIds = sourceEntities.stream().map(SourceEntity::getSourceId).toList();

                            int sourceCount = sourceEntities.size();
                            int chunkCount = chunkRepository.countBySourceIdIn(sourceIds);

                            return ProjectVO.builder()
                                    .projectId(projectEntity.getProjectId())
                                    .projectName(projectEntity.getProjectName())
                                    .projectDesc(projectEntity.getProjectDesc())
                                    .sysCreateDt(projectEntity.getSysCreateDt())
                                    .sysModifyDt(projectEntity.getSysModifyDt())
                                    .fileId(projectEntity.getFile().getFileId())
                                    .promptId(projectEntity.getPrompt().getPromptId())
                                    .sourceCount(sourceCount)
                                    .chunkCount(chunkCount)
                                    .build();

                        })
                        .toList())
                .isLast(projectEntityPage.isLast())
                .pageNo(projectEntityPage.getNumber() + 1)
                .pageSize(projectEntityPage.getSize())
                .totalCount(projectEntityPage.getTotalElements())
                .totalPages(projectEntityPage.getTotalPages())
                .build();
    }

    /**
     * 프로젝트 생성
     *
     * @param projectName    프로젝트명
     * @param projectDesc    프로젝트 설명
     * @param roleCode       역할 코드
     * @param toneCode       답변 톤 코드
     * @param styleCode      답변 스타일 코드
     * @param multipartFiles 임베딩 문서 목록
     */
    @Transactional
    @Override
    public void createProject(String projectName, String projectDesc, String roleCode, String toneCode, String styleCode, MultipartFile[] multipartFiles) {
        // 프롬프트 등록
        CommonCodeEntity role = commonCodeRepository.findByCode(roleCode)
                .orElseThrow(() -> new NotFoundException("역할 코드"));

        CommonCodeEntity tone = commonCodeRepository.findByCode(toneCode)
                .orElseThrow(() -> new NotFoundException("톤 코드"));

        CommonCodeEntity style = commonCodeRepository.findByCode(styleCode)
                .orElseThrow(() -> new NotFoundException("스타일 코드"));

        String promptContent = promptCoreService.generateMyAiPrompt(role.getCodeName(), tone.getCodeName(), style.getCodeName());

        PromptEntity promptEntity = promptRepository.save(PromptEntity.builder()
                .promptName("'" + projectName + "' 프로젝트 프롬프트")
                .promptContent(promptContent)
                .temperature(0.7D)
                .topP(0.95D)
                .build());

        List<FileDetailEntity> fileDetailEntities = new ArrayList<>();

        if (multipartFiles != null) {
            // 파일 업로드
            List<UploadFileVO> uploadFiles = new ArrayList<>();
            try {
                for (MultipartFile multipartFile : multipartFiles) {
                    uploadFiles.add(FileUtil.uploadFile(multipartFile, fileProperty.getFileStorePath()));
                }
            } catch (RuntimeException e) {
                for (UploadFileVO uploadFile : uploadFiles) {
                    FileUtil.deleteFile(uploadFile.getFilePath());
                }
                throw e;
            }

            // 새로운 파일 등록
            fileDetailEntities.addAll(uploadFiles.stream()
                    .map(uploadFileVO -> FileDetailEntity.builder()
                            .fileOriginName(uploadFileVO.getOriginFileName())
                            .fileName(uploadFileVO.getFileName())
                            .ip(uploadFileVO.getIp())
                            .filePath(uploadFileVO.getFilePath())
                            .fileSize(uploadFileVO.getFileSize())
                            .ext(uploadFileVO.getExt())
                            .url(uploadFileVO.getUrl())
                            .build())
                    .toList());
        }

        // 파일 목록 등록
        FileEntity fileEntity = fileRepository.save(FileEntity.builder()
                .fileDetails(fileDetailEntities)
                .build());

        // 프로젝트 등록
        ProjectEntity projectEntity = projectRepository.save(ProjectEntity.builder()
                .projectName(projectName)
                .projectDesc(projectDesc)
                .file(fileEntity)
                .prompt(promptEntity)
                .build());

        // 문서 임베딩
        embedCoreService.syncEmbedSources(
                CollectionType.myai(),
                fileEntity.getFileId(),
                MyAiConst.categoryCode(projectEntity.getProjectId()));
    }

    /**
     * 프로젝트 삭제
     *
     * @param projectId 프로젝트 ID
     */
    @Transactional
    @Override
    public void deleteProject(long projectId) {

        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트"));

        // 임베딩 문서 삭제
        embedCoreService.deleteEmbedSources(
                CollectionType.myai(),
                projectEntity.getFile().getFileId(),
                MyAiConst.categoryCode(projectEntity.getProjectId()));

        // 프로젝트 삭제
        projectRepository.deleteById(projectId);
    }

    /**
     * 프로젝트 임베딩 문서 목록 조회
     *
     * @param projectId 프로젝트 ID
     * @return 임베딩 문서 목록
     */
    @Transactional(readOnly = true)
    @Override
    public List<FileDetailVO> getProjectSources(long projectId) {

        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트"));

        return projectEntity.getFile().getFileDetails().stream()
                .map(fileDetailEntity -> FileDetailVO.builder()
                        .fileDetailId(fileDetailEntity.getFileDetailId())
                        .fileOriginName(fileDetailEntity.getFileOriginName())
                        .ext(fileDetailEntity.getExt())
                        .fileSize(fileDetailEntity.getFileSize())
                        .build())
                .toList();
    }

    /**
     * 프로젝트 임베딩 문서 업데이트
     *
     * @param projectId           프로젝트 ID
     * @param multipartFiles      임베딩 문서 목록
     * @param deleteFileDetailIds 삭제 파일 상세 ID 목록
     */
    @Transactional
    @Override
    public void updateProjectSources(long projectId, MultipartFile[] multipartFiles, List<Long> deleteFileDetailIds) {

        ProjectEntity projectEntity = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("프로젝트"));

        FileEntity fileEntity = projectEntity.getFile();

        // 삭제 대상 파일 상세 삭제
        fileEntity.getFileDetails()
                .removeIf(fileDetailEntity -> deleteFileDetailIds.contains(fileDetailEntity.getFileDetailId()));

        if (multipartFiles != null) {
            // 파일 업로드
            List<UploadFileVO> uploadFileVOS = new ArrayList<>();
            try {
                for (MultipartFile multipartFile : multipartFiles) {
                    uploadFileVOS.add(FileUtil.uploadFile(multipartFile, fileProperty.getFileStorePath()));
                }
            } catch (RuntimeException e) {
                for (UploadFileVO uploadFileVO : uploadFileVOS) {
                    FileUtil.deleteFile(uploadFileVO.getFilePath());
                }
                throw e;
            }

            // 새로운 파일 등록
            fileEntity.getFileDetails().addAll(uploadFileVOS.stream()
                    .map(uploadFileVO -> FileDetailEntity.builder()
                            .fileOriginName(uploadFileVO.getOriginFileName())
                            .fileName(uploadFileVO.getFileName())
                            .ip(uploadFileVO.getIp())
                            .filePath(uploadFileVO.getFilePath())
                            .fileSize(uploadFileVO.getFileSize())
                            .ext(uploadFileVO.getExt())
                            .url(uploadFileVO.getUrl())
                            .build())
                    .toList());
        }

        // 파일 목록 수정
        fileEntity = fileRepository.save(fileEntity);

        // 새로운 문서 임베딩
        embedCoreService.syncEmbedSources(
                CollectionType.myai(),
                fileEntity.getFileId(),
                MyAiConst.categoryCode(projectEntity.getProjectId()));

        // 새로운 파일로 업데이트
        projectEntity.setFile(fileEntity);
        projectRepository.save(projectEntity);
    }
}