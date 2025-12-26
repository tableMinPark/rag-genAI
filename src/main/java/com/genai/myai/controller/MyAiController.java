package com.genai.myai.controller;

import com.genai.core.controller.dto.response.PageResponseDto;
import com.genai.core.controller.dto.response.ResponseDto;
import com.genai.core.service.vo.FileDetailVO;
import com.genai.core.service.wrapper.PageWrapper;
import com.genai.global.enums.Response;
import com.genai.myai.controller.dto.request.CreateProjectRequestDto;
import com.genai.myai.controller.dto.request.UpdateProjectSourcesRequestDto;
import com.genai.myai.controller.dto.response.GetProjectResponseDto;
import com.genai.myai.controller.dto.response.GetProjectSourceResponseDto;
import com.genai.myai.service.MyAiService;
import com.genai.myai.service.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/myai")
public class MyAiController {

    private final MyAiService myAiService;

    /**
     * 프로젝트 목록 조회
     *
     * @param page 페이지
     * @param size 사이즈
     * @param keyword 키워드
     */
    @GetMapping
    public ResponseEntity<ResponseDto<PageResponseDto<GetProjectResponseDto>>> getProjects(
            @Min(1) @RequestParam("page") int page,
            @Min(1) @Max(100) @RequestParam("size") int size,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        PageWrapper<ProjectVO> projectVosPage = myAiService.getProjects(page, size, keyword);

        PageResponseDto<GetProjectResponseDto> pageResponseDto = PageResponseDto.<GetProjectResponseDto>builder()
                .content(projectVosPage.getContent().stream().map(GetProjectResponseDto::of).toList())
                .isLast(projectVosPage.isLast())
                .pageNo(projectVosPage.getPageNo())
                .pageSize(projectVosPage.getPageSize())
                .totalCount(projectVosPage.getTotalCount())
                .totalPages(projectVosPage.getTotalPages())
                .build();

        return ResponseEntity.ok().body(Response.MYAI_GET_PROJECTS_SUCCESS.toResponseDto(pageResponseDto));
    }

    /**
     * 프로젝트 생성
     *
     * @param createProjectRequestDto 프로젝트 생성 정보
     * @param multipartFiles          임베딩 문서 목록
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Map<String, Object>>> createProject(
            @Valid
            @RequestPart("requestDto") CreateProjectRequestDto createProjectRequestDto,
            @RequestPart("uploadFiles") MultipartFile[] multipartFiles
    ) {
        String projectName = createProjectRequestDto.getProjectName();
        String projectDesc = createProjectRequestDto.getProjectDesc();

        myAiService.createProject(projectName, projectDesc, multipartFiles);

        return ResponseEntity.ok().body(Response.MYAI_CREATE_PROJECT_SUCCESS.toResponseDto());
    }

    /**
     * 프로젝트 삭제
     *
     * @param projectId 프로젝트 ID
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ResponseDto<Map<String, Object>>> deleteProject(@PathVariable("projectId") Long projectId) {

        myAiService.deleteProject(projectId);

        return ResponseEntity.ok().body(Response.MYAI_DELETE_PROJECT_SUCCESS.toResponseDto());
    }

    /**
     * 프로젝트 임베딩 문서 목록 조회
     *
     * @param projectId      프로젝트 ID
     */
    @GetMapping(value = "/{projectId}/source")
    public ResponseEntity<ResponseDto<List<GetProjectSourceResponseDto>>> getProjectSources(
            @PathVariable("projectId") Long projectId
    ) {
        List<FileDetailVO> fileDetailVos = myAiService.getProjectSources(projectId);

        return ResponseEntity.ok().body(Response.MYAI_GET_PROJECT_SOURCES_SUCCESS.toResponseDto(GetProjectSourceResponseDto.toList(fileDetailVos)));
    }

    /**
     * 프로젝트 임베딩 문서 목록 수정
     *
     * @param projectId      프로젝트 ID
     * @param multipartFiles 임베딩 문서 목록
     */
    @PostMapping(value = "/{projectId}/source", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Map<String, Object>>> updateProjectSources(
            @PathVariable("projectId") Long projectId,
            @RequestPart("requestDto") UpdateProjectSourcesRequestDto updateProjectSourcesRequestDto,
            @RequestPart("uploadFiles") MultipartFile[] multipartFiles
    ) {
        List<Long> deleteFileDetailIds = updateProjectSourcesRequestDto.getDeleteFileDetailIds();

        myAiService.updateProjectSources(projectId, multipartFiles, deleteFileDetailIds);

        return ResponseEntity.ok().body(Response.MYAI_UPDATE_PROJECT_SOURCES_SUCCESS.toResponseDto());
    }
}
