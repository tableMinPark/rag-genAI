package com.genai.summary;

import com.genai.core.controller.dto.response.ResponseDto;
import com.genai.core.service.SummaryCoreService;
import com.genai.core.service.vo.SummaryVO;
import com.genai.summary.dto.request.SummaryFileRequestDto;
import com.genai.summary.dto.request.SummaryTextRequestDto;
import com.genai.summary.dto.response.SummaryResponseDto;
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
@RequestMapping("/api/summary")
public class SummaryController {

    private final SummaryCoreService summaryCoreService;

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

        log.info("텍스트 요약 요청 : {} | {} : {}", sessionId, lengthRatio, context);

        long chatId = 5L;
        SummaryVO summaryVO = summaryCoreService.summary(lengthRatio, context, sessionId, chatId);

        return ResponseEntity.ok()
                .body(ResponseDto.<SummaryResponseDto>builder()
                        .status("SUCCESS")
                        .message("요약 요청 성공")
                        .data(SummaryResponseDto.builder()
                                .sessionId(sessionId)
                                .msgId(summaryVO.getMsgId())
                                .content(summaryVO.getContent())
                                .build())
                        .build());
    }

    /**
     * 번역 요청
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

        log.info("파일 번역 요청 : {} | {} : {}", sessionId, lengthRatio, multipartFile.getOriginalFilename());

        long chatId = 5L;
        SummaryVO summaryVO = summaryCoreService.summary(lengthRatio, multipartFile, sessionId, chatId);

        return ResponseEntity.ok()
                .body(ResponseDto.<SummaryResponseDto>builder()
                        .status("SUCCESS")
                        .message("요약 요청 성공")
                        .data(SummaryResponseDto.builder()
                                .sessionId(sessionId)
                                .msgId(summaryVO.getMsgId())
                                .content(summaryVO.getContent())
                                .build())
                        .build());
    }
}
