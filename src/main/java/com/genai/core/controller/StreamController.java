package com.genai.core.controller;

import com.genai.core.controller.dto.response.ResponseDto;
import com.genai.core.controller.dto.response.StreamCancelResponseDto;
import com.genai.core.service.StreamCoreService;
import com.genai.core.service.subscriber.StreamSubscriber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamCoreService streamCoreService;

    /**
     * 스트림 발행
     *
     * @param sessionId 세션 ID
     * @return SSE Emitter
     */
    @GetMapping("/{sessionId}")
    public SseEmitter stream(@PathVariable("sessionId") String sessionId) throws InterruptedException {

        log.info("사용자 스트림 요청 : {}", sessionId);

        // 스트림 등록
        StreamSubscriber streamSubscriber = streamCoreService.createStream(sessionId);

        // 연결 끊김 처리
        streamSubscriber.getEmitter().onCompletion(() -> {
            streamCoreService.deleteStream(sessionId);
            log.info("사용자 SSE 종료 : {}", sessionId);
        });

        // 세션 만료 처리
        streamSubscriber.getEmitter().onTimeout(() -> {
            streamCoreService.deleteStream(sessionId);
            log.info("사용자 SSE 타임 아웃 : {}", sessionId);
        });

        // 에러 처리
        streamSubscriber.getEmitter().onError(throwable -> {
            log.error("사용자 SSE 에러 : {} | {}", sessionId, throwable.getMessage());
        });

        return streamSubscriber.getEmitter();
    }

    /**
     * 스트림 중지 요청
     *
     * @param sessionId 세션 ID
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<ResponseDto<StreamCancelResponseDto>> cancelStream(@PathVariable("sessionId") String sessionId) {

        // 답변 스트림 삭제
        streamCoreService.deleteStream(sessionId);

        return ResponseEntity.ok()
                .body(ResponseDto.<StreamCancelResponseDto>builder()
                        .status("SUCCESS")
                        .message("스트림 중지 요청 성공")
                        .data(StreamCancelResponseDto.builder()
                                .sessionId(sessionId)
                                .build())
                        .build());
    }
}
