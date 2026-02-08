package com.genai.app.report.controller;

import com.genai.app.chat.service.ChatService;
import com.genai.app.report.controller.dto.request.ReportFileRequestDto;
import com.genai.app.report.controller.dto.request.ReportTextRequestDto;
import com.genai.app.report.controller.dto.response.ReportResponseDto;
import com.genai.core.service.business.PromptCoreService;
import com.genai.core.service.business.ReportCoreService;
import com.genai.core.service.business.StreamCoreService;
import com.genai.core.service.business.vo.ReportVO;
import com.genai.global.dto.ResponseDto;
import com.genai.global.enums.Menu;
import com.genai.global.enums.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/report")
public class ReportController {

    private final ReportCoreService reportCoreService;
    private final PromptCoreService promptCoreService;
    private final StreamCoreService streamCoreService;
    private final ChatService chatService;

    /**
     * 보고서 생성 요청
     *
     * @param reportTextRequestDto 보고서 생성 정보
     */
    @PostMapping(value = "/text")
    public ResponseEntity<ResponseDto<ReportResponseDto>> generateReportText(@Valid @RequestBody ReportTextRequestDto reportTextRequestDto) {

        String sessionId = reportTextRequestDto.getSessionId();
        String promptContext = promptCoreService.generateReportPrompt(reportTextRequestDto.getRequestContent());
        String title = reportTextRequestDto.getTitle();
        String content = reportTextRequestDto.getContext();

        long chatId = chatService.getChat(sessionId, title, Menu.MENU_REPORT).getChatId();
        ReportVO reportVO = reportCoreService.generateReport(title, promptContext, content, sessionId, chatId);

        reportVO.getAnswerStream().subscribe(streamCoreService.getStream(sessionId));

        return ResponseEntity.ok().body(Response.REPORT_GENERATE_TEXT_SUCCESS.toResponseDto(ReportResponseDto.builder()
                .sessionId(sessionId)
                .msgId(reportVO.getMsgId())
                .build()));
    }

    /**
     * 보고서 생성 요청
     *
     * @param reportFileRequestDto 보고서 생성 정보
     * @param multipartFiles       참조 문서 파일
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ReportResponseDto>> generateReportFile(
            @Valid @RequestPart("requestDto") ReportFileRequestDto reportFileRequestDto,
            @RequestPart("uploadFile") MultipartFile[] multipartFiles
    ) {

        String sessionId = reportFileRequestDto.getSessionId();
        String promptContext = promptCoreService.generateReportPrompt(reportFileRequestDto.getRequestContent());
        String title = reportFileRequestDto.getTitle();

        long chatId = chatService.getChat(sessionId, title, Menu.MENU_REPORT).getChatId();
        ReportVO reportVO = reportCoreService.generateReport(title, promptContext, multipartFiles, sessionId, chatId);

        reportVO.getAnswerStream().subscribe(streamCoreService.getStream(sessionId));

        return ResponseEntity.ok().body(Response.REPORT_GENERATE_FILE_SUCCESS.toResponseDto(ReportResponseDto.builder()
                .sessionId(sessionId)
                .msgId(reportVO.getMsgId())
                .build()));
    }
}
