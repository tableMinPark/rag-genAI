package com.genai.report.controller;

import com.genai.core.controller.dto.response.ResponseDto;
import com.genai.core.service.ReportCoreService;
import com.genai.core.service.vo.ReportVO;
import com.genai.report.controller.dto.request.ReportFileRequestDto;
import com.genai.report.controller.dto.request.ReportTextRequestDto;
import com.genai.report.controller.dto.response.ReportResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {

    private final ReportCoreService reportCoreService;

    /**
     * 보고서 생성 요청
     *
     * @param reportTextRequestDto 보고서 생성 정보
     */
    @PostMapping(value = "/text")
    public ResponseEntity<ResponseDto<ReportResponseDto>> generateReportText(@Valid @RequestBody ReportTextRequestDto reportTextRequestDto) {

        String sessionId = reportTextRequestDto.getSessionId();
        String promptContext = reportTextRequestDto.getPrompt();
        String content = reportTextRequestDto.getContext();

        log.info("텍스트 참조 보고서 생성 요청 : {} | {} : {}", sessionId, promptContext, content);

        long chatId = 3L;
        ReportVO reportVO = reportCoreService.generateReport("", promptContext, content, sessionId, chatId);

        return ResponseEntity.ok()
                .body(ResponseDto.<ReportResponseDto>builder()
                        .status("SUCCESS")
                        .message("보고서 생성 요청 성공")
                        .data(ReportResponseDto.builder()
                                .sessionId(sessionId)
                                .msgId(reportVO.getMsgId())
                                .content(reportVO.getContent())
                                .build())
                        .build());
    }

    /**
     * 보고서 생성 요청
     *
     * @param reportFileRequestDto 보고서 생성 정보
     * @param multipartFile 참조 문서 파일
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ReportResponseDto>> generateReportFile(
            @Valid
            @RequestPart("requestDto") ReportFileRequestDto reportFileRequestDto,
            @RequestPart("uploadFile") MultipartFile multipartFile
    ) {

        String sessionId = reportFileRequestDto.getSessionId();
        String promptContext = reportFileRequestDto.getPrompt();

        log.info("파일 참조 보고서 생성 요청 : {} | {} : {}", sessionId, promptContext, multipartFile.getOriginalFilename());

        long chatId = 3L;
        ReportVO reportVO = reportCoreService.generateReport("", promptContext, multipartFile, sessionId, chatId);

        return ResponseEntity.ok()
                .body(ResponseDto.<ReportResponseDto>builder()
                        .status("SUCCESS")
                        .message("보고서 생성 요청 성공")
                        .data(ReportResponseDto.builder()
                                .sessionId(sessionId)
                                .msgId(reportVO.getMsgId())
                                .content(reportVO.getContent())
                                .build())
                        .build());
    }
}
