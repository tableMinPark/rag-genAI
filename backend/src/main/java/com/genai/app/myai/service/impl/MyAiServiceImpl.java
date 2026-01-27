package com.genai.app.myai.service.impl;

import com.genai.app.myai.constant.MyAiConst;
import com.genai.app.myai.repository.ProjectRepository;
import com.genai.app.myai.repository.entity.ProjectEntity;
import com.genai.app.myai.service.MyAiService;
import com.genai.app.myai.service.vo.ProjectVO;
import com.genai.core.config.properties.FileProperty;
import com.genai.core.constant.PromptConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.FileRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.SourceRepository;
import com.genai.core.repository.entity.*;
import com.genai.core.service.business.EmbedCoreService;
import com.genai.core.service.business.vo.FileDetailVO;
import com.genai.core.type.CollectionType;
import com.genai.global.utils.FileUtil;
import com.genai.global.utils.UploadFile;
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
            projectEntityPage = projectRepository.findAllByProjectName(keyword, pageable);
        }

        return PageWrapper.<ProjectVO>builder()
                .content(projectEntityPage.getContent().stream()
                        .map(projectEntity -> {

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
     * @param multipartFiles 임베딩 문서 목록
     */
    @Transactional
    @Override
    public void createProject(String projectName, String projectDesc, MultipartFile[] multipartFiles) {
        // 프롬프트 조회
        PromptEntity promptEntity = promptRepository.findById(PromptConst.QUESTION_MYAI_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("나만의 AI 질의 프롬프트"));

        List<FileDetailEntity> fileDetailEntities = new ArrayList<>();

        if (multipartFiles != null) {
            // 파일 업로드
            List<UploadFile> uploadFiles = new ArrayList<>();
            try {
                for (MultipartFile multipartFile : multipartFiles) {
                    uploadFiles.add(FileUtil.uploadFile(multipartFile, fileProperty.getFileStorePath()));
                }
            } catch (RuntimeException e) {
                for (UploadFile uploadFile : uploadFiles) {
                    FileUtil.deleteFile(uploadFile.getFilePath());
                }
                throw e;
            }

            // 새로운 파일 등록
            fileDetailEntities.addAll(uploadFiles.stream()
                    .map(uploadFile -> FileDetailEntity.builder()
                            .fileOriginName(uploadFile.getOriginFileName())
                            .fileName(uploadFile.getFileName())
                            .ip(uploadFile.getIp())
                            .filePath(uploadFile.getFilePath())
                            .fileSize(uploadFile.getFileSize())
                            .ext(uploadFile.getExt())
                            .url(uploadFile.getUrl())
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
                MyAiConst.MYAI_CATEGORY_CODE(projectEntity.getProjectId()));
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
                MyAiConst.MYAI_CATEGORY_CODE(projectEntity.getProjectId()));

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
            List<UploadFile> uploadFiles = new ArrayList<>();
            try {
                for (MultipartFile multipartFile : multipartFiles) {
                    uploadFiles.add(FileUtil.uploadFile(multipartFile, fileProperty.getFileStorePath()));
                }
            } catch (RuntimeException e) {
                for (UploadFile uploadFile : uploadFiles) {
                    FileUtil.deleteFile(uploadFile.getFilePath());
                }
                throw e;
            }

            // 새로운 파일 등록
            fileEntity.getFileDetails().addAll(uploadFiles.stream()
                    .map(uploadFile -> FileDetailEntity.builder()
                            .fileOriginName(uploadFile.getOriginFileName())
                            .fileName(uploadFile.getFileName())
                            .ip(uploadFile.getIp())
                            .filePath(uploadFile.getFilePath())
                            .fileSize(uploadFile.getFileSize())
                            .ext(uploadFile.getExt())
                            .url(uploadFile.getUrl())
                            .build())
                    .toList());
        }

        // 파일 목록 수정
        fileEntity = fileRepository.save(fileEntity);

        // 새로운 문서 임베딩
        embedCoreService.syncEmbedSources(
                CollectionType.myai(),
                fileEntity.getFileId(),
                MyAiConst.MYAI_CATEGORY_CODE(projectEntity.getProjectId()));

        // 새로운 파일로 업데이트
        projectEntity.setFile(fileEntity);
        projectRepository.save(projectEntity);
    }
}
