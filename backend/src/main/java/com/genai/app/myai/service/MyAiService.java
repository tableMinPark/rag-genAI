package com.genai.app.myai.service;

import com.genai.core.service.business.vo.FileDetailVO;
import com.genai.global.wrapper.PageWrapper;
import com.genai.app.myai.service.vo.ProjectVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MyAiService {

    /**
     * 프로젝트 조회
     *
     * @param projectId 프로젝트 ID
     * @return 프로젝트
     */
    ProjectVO getProject(long projectId);

    /**
     * 프로젝트 목록 조회
     *
     * @param page    페이지
     * @param size    사이즈
     * @param keyword 키워드
     * @return 프로젝트 목록
     */
    PageWrapper<ProjectVO> getProjects(int page, int size, String keyword);

    /**
     * 프로젝트 생성
     *
     * @param projectName    프로젝트명
     * @param projectDesc    프로젝트 설명
     * @param multipartFiles 임베딩 문서 목록
     */
    void createProject(String projectName, String projectDesc, MultipartFile[] multipartFiles);

    /**
     * 프로젝트 삭제
     *
     * @param projectId 프로젝트 ID
     */
    void deleteProject(long projectId);

    /**
     * 프로젝트 임베딩 문서 목록 조회
     *
     * @param projectId 프로젝트 ID
     * @return 임베딩 문서 목록
     */
    List<FileDetailVO> getProjectSources(long projectId);

    /**
     * 프로젝트 임베딩 문서 업데이트
     *
     * @param projectId           프로젝트 ID
     * @param multipartFiles      임베딩 문서 목록
     * @param deleteFileDetailIds 삭제 파일 상세 ID 목록
     */
    void updateProjectSources(long projectId, MultipartFile[] multipartFiles, List<Long> deleteFileDetailIds);
}
