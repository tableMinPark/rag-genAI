package com.genai.app.summary.controller;

import com.genai.app.chat.service.ChatService;
import com.genai.app.summary.controller.dto.request.SummaryFileRequestDto;
import com.genai.app.summary.controller.dto.request.SummaryTextRequestDto;
import com.genai.core.service.business.StreamCoreService;
import com.genai.core.service.business.SummaryCoreService;
import com.genai.core.service.business.vo.SummaryVO;
import com.genai.global.enums.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/summary")
public class SummaryController {

    private final SummaryCoreService summaryCoreService;
    private final StreamCoreService streamCoreService;
    private final ChatService chatService;

    /**
     * 요약 요청
     *
     * @param summaryTextRequestDto 요약 요청 정보
     */
    @PostMapping(value = "/text")
    public SseEmitter summaryText(@Valid @RequestBody SummaryTextRequestDto summaryTextRequestDto) {

        String sessionId = summaryTextRequestDto.getSessionId();
        float lengthRatio = summaryTextRequestDto.getLengthRatio();
        String context = summaryTextRequestDto.getContext();

        long chatId = chatService.getChat(sessionId, "", Menu.MENU_SUMMARY).getChatId();
        SummaryVO summaryVO = summaryCoreService.summary(lengthRatio, context, sessionId, chatId);

        return streamCoreService.createStream(sessionId).subscribeWithTrace(summaryVO.getAnswerStream());
    }

    /**
     * 요약 요청
     *
     * @param summaryFileRequestDto 번역 요청 정보
     * @param multipartFile         번역 문서 파일
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SseEmitter summaryFile(
            @Valid @RequestPart("requestDto") SummaryFileRequestDto summaryFileRequestDto,
            @RequestPart("uploadFile") MultipartFile multipartFile
    ) {

        String sessionId = summaryFileRequestDto.getSessionId();
        float lengthRatio = summaryFileRequestDto.getLengthRatio();

        long chatId = chatService.getChat(sessionId, "", Menu.MENU_SUMMARY).getChatId();
        SummaryVO summaryVO = summaryCoreService.summary(lengthRatio, multipartFile, sessionId, chatId);

        return streamCoreService.createStream(sessionId).subscribeWithTrace(summaryVO.getAnswerStream());
    }
}
