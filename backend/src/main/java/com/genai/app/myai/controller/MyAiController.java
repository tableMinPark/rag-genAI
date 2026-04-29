package com.genai.app.myai.controller;

import com.genai.app.myai.constant.MyAiConst;
import com.genai.app.myai.controller.dto.request.CreateProjectRequestDto;
import com.genai.app.myai.controller.dto.request.UpdateProjectSourcesRequestDto;
import com.genai.app.myai.controller.dto.response.GetProjectResponseDto;
import com.genai.app.myai.controller.dto.response.GetProjectSourceResponseDto;
import com.genai.app.myai.service.MyAiService;
import com.genai.app.myai.service.vo.CreateProjectVO;
import com.genai.app.myai.service.vo.DeleteProjectVO;
import com.genai.app.myai.service.vo.ProjectVO;
import com.genai.app.myai.service.vo.UpdateProjectVO;
import com.genai.global.utils.StringUtil;
import com.genai.core.service.business.EmbedCoreService;
import com.genai.core.service.business.StreamCoreService;
import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.EmbedVO;
import com.genai.core.service.business.vo.FileDetailVO;
import com.genai.core.service.business.vo.PrepareVO;
import com.genai.core.type.CollectionType;
import com.genai.global.dto.PageResponseDto;
import com.genai.core.domain.Member;
import com.genai.global.dto.ResponseDto;
import com.genai.global.enums.Response;
import com.genai.global.wrapper.PageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/myai")
public class MyAiController {

    private final MyAiService myAiService;
    private final EmbedCoreService embedCoreService;
    private final StreamCoreService streamCoreService;

    /**
     * 프로젝트 조회
     *
     * @param projectId 프로젝트 ID
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ResponseDto<GetProjectResponseDto>> getProject(@NotNull @PathVariable("projectId") Long projectId,
                                                                         @AuthenticationPrincipal Member member) {

        String userId = member.getUserId();
        ProjectVO projectVo = myAiService.getProject(userId, projectId);

        return ResponseEntity.ok().body(Response.MYAI_GET_PROJECTS_SUCCESS.toResponseDto(GetProjectResponseDto.of(projectVo)));
    }

    /**
     * 프로젝트 목록 조회
     *
     * @param page    페이지
     * @param size    사이즈
     * @param keyword 키워드
     */
    @GetMapping
    public ResponseEntity<ResponseDto<PageResponseDto<GetProjectResponseDto>>> getProjects(
            @NotNull @Min(1) @RequestParam("page") Integer page,
            @NotNull @Min(1) @Max(100) @RequestParam("size") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @AuthenticationPrincipal Member member
    ) {
        String userId = member.getUserId();
        PageWrapper<ProjectVO> projectVosPage = myAiService.getProjects(userId, page, size, keyword);

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
    public SseEmitter createProject(
            @Valid @RequestPart("requestDto") CreateProjectRequestDto createProjectRequestDto,
            @RequestPart("uploadFiles") MultipartFile[] multipartFiles,
            @AuthenticationPrincipal Member member
    ) {
        String userId = member.getUserId();
        String sessionId = StringUtil.generateRandomId();
        String projectName = createProjectRequestDto.getProjectName();
        String projectDesc = createProjectRequestDto.getProjectDesc();
        String roleCode = createProjectRequestDto.getRoleCode();
        String toneCode = createProjectRequestDto.getToneCode();
        String styleCode = createProjectRequestDto.getStyleCode();
        CollectionType collectionType = CollectionType.myai();

        // 프로젝트 등록 Mono
        Mono<Pair<StreamEvent, CreateProjectVO>> createProjectMono = Mono.just(sessionId).flatMap(o -> Mono.fromCallable(() -> {
                    CreateProjectVO createProject = myAiService.createProject(userId, projectName, projectDesc, roleCode, toneCode, styleCode, multipartFiles);

                    return Pair.of(StreamEvent.prepare(sessionId, PrepareVO.builder()
                            .progress(0.3f)
                            .message("문서 힉습중")
                            .build()), createProject);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .cache();

        // 대상 문서 동기화 Flux
        Mono<Pair<StreamEvent, EmbedVO>> syncSourceMono = createProjectMono.flatMap(pair -> Mono.fromCallable(() -> {
                    CreateProjectVO createProject = pair.getSecond();
                    EmbedVO embed = embedCoreService.syncEmbedSources(
                            CollectionType.myai(),
                            createProject.getFileId(),
                            MyAiConst.categoryCode(createProject.getProject().getProjectId()));

                    return Pair.of(StreamEvent.prepare(sessionId, PrepareVO.builder()
                            .progress(0.6f)
                            .message("문서 힉습중")
                            .build()), embed);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .cache();

        // 문서 임베딩 Flux
        Mono<StreamEvent> embedFlux = syncSourceMono.flatMap(pair -> {
                    EmbedVO embed = pair.getSecond();
                    return embedCoreService.syncEmbedSources(collectionType, embed.getDocumentEntities(), embed.getDeleteDocumentIds())
                            .then(Mono.just(
                                    StreamEvent.prepare(
                                            StringUtil.generateRandomId(),
                                            PrepareVO.builder()
                                                    .progress(1)
                                                    .message("문서 학습중")
                                                    .build()
                                    )
                            ));
                })
                .subscribeOn(Schedulers.boundedElastic());

        Flux<StreamEvent> finalFlux = Flux.concat(createProjectMono.map(Pair::getFirst), syncSourceMono.map(Pair::getFirst), embedFlux)
                .onErrorMap(throwable -> new RuntimeException("프로젝트 등록 중 에러가 발생했습니다. 기존 문서를 삭제하고, 문서를 재등록해주세요.", throwable));

        return streamCoreService.createStream(sessionId).subscribeWithTrace(finalFlux);
    }

    /**
     * 프로젝트 삭제
     *
     * @param projectId 프로젝트 ID
     */
    @DeleteMapping("/{projectId}")
    public ResponseEntity<ResponseDto<?>> deleteProject(@NotNull @PathVariable("projectId") Long projectId,
                                                        @AuthenticationPrincipal Member member) {

        String userId = member.getUserId();
        DeleteProjectVO deleteProjectVO = myAiService.deleteProject(userId, projectId);

        // 임베딩 문서 삭제
        embedCoreService.deleteEmbedSources(
                CollectionType.myai(),
                deleteProjectVO.getFileId(),
                MyAiConst.categoryCode(deleteProjectVO.getProject().getProjectId()));

        return ResponseEntity.ok().body(Response.MYAI_DELETE_PROJECT_SUCCESS.toResponseDto());
    }

    /**
     * 프로젝트 임베딩 문서 목록 조회
     *
     * @param projectId 프로젝트 ID
     */
    @GetMapping(value = "/{projectId}/source")
    public ResponseEntity<ResponseDto<List<GetProjectSourceResponseDto>>> getProjectSources(
            @NotNull @PathVariable("projectId") Long projectId,
            @AuthenticationPrincipal Member member
    ) {
        String userId = member.getUserId();
        List<FileDetailVO> fileDetailVos = myAiService.getProjectSources(userId, projectId);

        return ResponseEntity.ok().body(Response.MYAI_GET_PROJECT_SOURCES_SUCCESS.toResponseDto(GetProjectSourceResponseDto.toList(fileDetailVos)));
    }

    /**
     * 프로젝트 임베딩 문서 목록 수정
     *
     * @param projectId      프로젝트 ID
     * @param multipartFiles 임베딩 문서 목록
     */
    @PostMapping(value = "/{projectId}/source", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter updateProjectSources(
            @NotNull @PathVariable("projectId") Long projectId,
            @Valid @RequestPart("requestDto") UpdateProjectSourcesRequestDto updateProjectSourcesRequestDto,
            @RequestPart(value = "uploadFiles", required = false) MultipartFile[] multipartFiles,
            @AuthenticationPrincipal Member member
    ) {
        String userId = member.getUserId();
        String sessionId = StringUtil.generateRandomId();
        List<Long> deleteFileDetailIds = updateProjectSourcesRequestDto.getDeleteFileDetailIds();
        CollectionType collectionType = CollectionType.myai();

        // 프로젝트 업데이트 Mono
        Mono<Pair<StreamEvent, UpdateProjectVO>> updateProjectMono = Mono.just(sessionId).flatMap(o -> Mono.fromCallable(() -> {
                    UpdateProjectVO updateProject = myAiService.updateProjectSources(userId, projectId, multipartFiles, deleteFileDetailIds);

                    return Pair.of(StreamEvent.prepare(sessionId, PrepareVO.builder()
                            .progress(0.3f)
                            .message("문서 힉습중")
                            .build()), updateProject);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .cache();

        // 대상 문서 동기화 Flux
        Mono<Pair<StreamEvent, EmbedVO>> syncSourceMono = updateProjectMono.flatMap(pair -> Mono.fromCallable(() -> {
                    UpdateProjectVO updateProject = pair.getSecond();
                    EmbedVO embed = embedCoreService.syncEmbedSources(
                            collectionType,
                            updateProject.getFileId(),
                            MyAiConst.categoryCode(updateProject.getProject().getProjectId()));

                    return Pair.of(StreamEvent.prepare(sessionId, PrepareVO.builder()
                            .progress(0.6f)
                            .message("문서 힉습중")
                            .build()), embed);
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .cache();

        // 문서 임베딩 Flux
        Mono<StreamEvent> embedFlux = syncSourceMono.flatMap(pair -> {
                    EmbedVO embed = pair.getSecond();
                    return embedCoreService.syncEmbedSources(collectionType, embed.getDocumentEntities(), embed.getDeleteDocumentIds())
                            .then(Mono.just(
                                    StreamEvent.prepare(
                                            StringUtil.generateRandomId(),
                                            PrepareVO.builder()
                                                    .progress(1)
                                                    .message("문서 학습중")
                                                    .build()
                                    )
                            ));
                })
                .subscribeOn(Schedulers.boundedElastic());

        Flux<StreamEvent> finalFlux = Flux.concat(updateProjectMono.map(Pair::getFirst), syncSourceMono.map(Pair::getFirst), embedFlux)
                .onErrorMap(throwable -> new RuntimeException("프로젝트 수정 중 에러가 발생했습니다. 기존 문서를 삭제하고, 문서를 재등록해주세요.", throwable));

        return streamCoreService.createStream(sessionId).subscribeWithTrace(finalFlux);
    }
}
