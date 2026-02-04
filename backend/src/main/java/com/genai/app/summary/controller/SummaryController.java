package com.genai.app.summary.controller;

import com.genai.app.summary.controller.dto.request.SummaryFileRequestDto;
import com.genai.app.summary.controller.dto.request.SummaryTextRequestDto;
import com.genai.app.summary.controller.dto.response.SummaryResponseDto;
import com.genai.core.service.business.StreamCoreService;
import com.genai.core.service.business.SummaryCoreService;
import com.genai.core.service.business.subscriber.StreamSubscriber;
import com.genai.core.service.business.vo.SummaryVO;
import com.genai.global.dto.ResponseDto;
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
@RequestMapping("/summary")
public class SummaryController {

    private final SummaryCoreService summaryCoreService;
    private final StreamCoreService streamCoreService;

    /**
     * 요약 요청
     *
     * @param summaryTextRequestDto 요약 요청 정보
     */
    @PostMapping(value = "/text")
    public ResponseEntity<ResponseDto<SummaryResponseDto>> summaryText(@Valid @RequestBody SummaryTextRequestDto summaryTextRequestDto) {

        String sessionId = summaryTextRequestDto.getSessionId();
        float lengthRatio = summaryTextRequestDto.getLengthRatio();
        String context = summaryTextRequestDto.getContext();

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 3L;
        SummaryVO summaryVO = summaryCoreService.summary(lengthRatio, context, sessionId, chatId);

        summaryVO.getAnswerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.SUMMARY_GENERATE_TEXT_SUCCESS.toResponseDto(SummaryResponseDto.builder()
                .sessionId(sessionId)
                .msgId(summaryVO.getMsgId())
                .build()));
    }

    /**
     * 요약 요청
     *
     * @param summaryFileRequestDto 번역 요청 정보
     * @param multipartFile 번역 문서 파일
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<SummaryResponseDto>> summaryFile(
            @Valid
            @RequestPart("requestDto") SummaryFileRequestDto summaryFileRequestDto,
            @RequestPart("uploadFile") MultipartFile multipartFile
    ) {

        String sessionId = summaryFileRequestDto.getSessionId();
        float lengthRatio = summaryFileRequestDto.getLengthRatio();

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 3L;
        SummaryVO summaryVO = summaryCoreService.summary(lengthRatio, multipartFile, sessionId, chatId);

        summaryVO.getAnswerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.SUMMARY_GENERATE_FILE_SUCCESS.toResponseDto(SummaryResponseDto.builder()
                .sessionId(sessionId)
                .msgId(summaryVO.getMsgId())
                .build()));
    }
}
