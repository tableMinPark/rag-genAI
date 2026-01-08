package com.genai.app.report.controller;

import com.genai.app.report.controller.dto.request.ReportFileRequestDto;
import com.genai.app.report.controller.dto.request.ReportTextRequestDto;
import com.genai.app.report.controller.dto.response.ReportResponseDto;
import com.genai.core.service.business.ReportCoreService;
import com.genai.core.service.business.vo.ReportVO;
import com.genai.global.dto.ResponseDto;
import com.genai.global.enums.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/report")
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
        String title = reportTextRequestDto.getTitle();
        String content = reportTextRequestDto.getContext();

        long chatId = 5L;
        ReportVO reportVO = reportCoreService.generateReport(title, promptContext, List.of(content), sessionId, chatId);

        return ResponseEntity.ok().body(Response.REPORT_GENERATE_TEXT_SUCCESS.toResponseDto(ReportResponseDto.builder()
                .sessionId(sessionId)
                .msgId(reportVO.getMsgId())
                .content(reportVO.getContent())
                .build()));
    }

    /**
     * 보고서 생성 요청
     *
     * @param reportFileRequestDto 보고서 생성 정보
     * @param multipartFiles 참조 문서 파일
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ReportResponseDto>> generateReportFile(
            @Valid
            @RequestPart("requestDto") ReportFileRequestDto reportFileRequestDto,
            @RequestPart("uploadFile") MultipartFile[] multipartFiles
    ) {

        String sessionId = reportFileRequestDto.getSessionId();
        String promptContext = reportFileRequestDto.getPrompt();
        String title = reportFileRequestDto.getTitle();

        long chatId = 5L;
        ReportVO reportVO = reportCoreService.generateReport(title, promptContext, multipartFiles, sessionId, chatId);

        return ResponseEntity.ok().body(Response.REPORT_GENERATE_FILE_SUCCESS.toResponseDto(ReportResponseDto.builder()
                .sessionId(sessionId)
                .msgId(reportVO.getMsgId())
                .content(reportVO.getContent())
                .build()));
    }
}
