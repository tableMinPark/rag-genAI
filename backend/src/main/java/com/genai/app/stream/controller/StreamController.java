package com.genai.app.stream.controller;

import com.genai.app.stream.controller.dto.response.StreamCancelResponseDto;
import com.genai.global.stream.service.StreamCoreService;
import com.genai.global.common.dto.ResponseDto;
import com.genai.app.common.enums.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/stream")
public class StreamController {

    private final StreamCoreService streamCoreService;

    /**
     * 스트림 중지 요청
     *
     * @param sessionId 세션 ID
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ResponseDto<StreamCancelResponseDto>> cancelStream(@NotBlank @PathVariable("sessionId") String sessionId) {

        log.info("[{}] " + String.format("%-20s", "Stream cancel from user" + " |"), sessionId);

        // 답변 스트림 삭제
        streamCoreService.deleteStream(sessionId);

        return ResponseEntity.ok().body(Response.CANCEL_STREAM_SUCCESS.toResponseDto(StreamCancelResponseDto.builder()
                .sessionId(sessionId)
                .build()));
    }
}
