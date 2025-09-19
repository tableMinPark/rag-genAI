package com.genai.adapter.in;

import com.genai.adapter.in.dto.request.LlmChatRequestDto;
import com.genai.adapter.in.dto.response.ChatResponseDto;
import com.genai.adapter.in.dto.response.ResponseDto;
import com.genai.application.service.ChatService;
import com.genai.constant.ChatConst;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/llm")
public class LlmController {

    private static final String SERVICE_NAME = "llm";
    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ChatService chatService;

    /**
     * Emitter 조회
     *
     * @param sessionId 세션 ID
     * @return SSE Emitter
     */
    @GetMapping("/stream/{sessionId}")
    public SseEmitter stream(@PathVariable String sessionId) {

        String emitterKey = String.format("/%s/%s", SERVICE_NAME, sessionId);

        log.info("LLM 테스트 스트림 요청({})", sessionId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(emitterKey, emitter);
        try {
            // 연결 확인 코멘트 전송
            emitter.send(SseEmitter.event().comment(sessionId));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        // 세션 만료 또는 연결 끊김 처리
        emitter.onCompletion(() -> emitters.remove(emitterKey));
        emitter.onTimeout(() -> emitters.remove(emitterKey));

        return emitter;
    }

    /**
     * LLM 테스트 질의 요청
     *
     * @param llmChatRequestDto 질의 정보
     */
    @PostMapping("/chat")
    public ResponseEntity<ResponseDto<ChatResponseDto>> lawChat(@RequestBody LlmChatRequestDto llmChatRequestDto) {

        String sessionId = llmChatRequestDto.getSessionId();
        String emitterKey = String.format("/%s/%s", SERVICE_NAME, sessionId);
        SseEmitter emitter = emitters.get(emitterKey);

        String queryEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.QUERY_EVENT_NAME, sessionId);
        String answerEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.ANSWER_EVENT_NAME, sessionId);

        String query = llmChatRequestDto.getQuery();
        String context = llmChatRequestDto.getContext();
        String promptContext = llmChatRequestDto.getPrompt();

        if (emitter != null) {
            log.info("LLM 테스트 질문 요청({}) | {}\n{}\n{}", sessionId, query, context, promptContext);

            try {
                emitter.send(SseEmitter.event().name(queryEventName).data(query));
                emitter.send(SseEmitter.event().name(answerEventName).data(ChatConst.ANSWER_START_PREFIX));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }

            chatService.questionUseCase(query, context, promptContext, sessionId)
                    .subscribe(message -> {
                                try {
                                    if (ChatConst.STREAM_OVER_PREFIX.equals(message)) {
                                        emitter.send(SseEmitter.event().name(answerEventName).data(ChatConst.ANSWER_END_PREFIX));
                                        emitter.complete();
                                    } else {
                                        emitter.send(SseEmitter.event().name(answerEventName).data(message));
                                    }
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            emitter::complete
                    );

            return ResponseEntity.ok()
                    .body(ResponseDto.<ChatResponseDto>builder()
                            .status("SUCCESS")
                            .message("LLM 테스트 질의 요청 성공")
                            .data(ChatResponseDto.builder()
                                    .query(query)
                                    .sessionId(sessionId)
                                    .build())
                            .build());
        } else {
            log.error("LLM 테스트 스트림 없음({})", sessionId);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ResponseDto.<ChatResponseDto>builder()
                            .status("ERROR")
                            .message("답변 스트림이 열리지 않음")
                            .data(ChatResponseDto.builder()
                                    .query(query)
                                    .sessionId(sessionId)
                                    .build())
                            .build());
        }
    }
}