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
     * @param tabId   탭 식별자
     * @param session 세션
     * @return SSE Emitter
     */
    @GetMapping("/stream/{tabId}")
    public SseEmitter stream(@PathVariable String tabId, HttpSession session) {

        String sessionId = session.getId();
        String emitterKey = String.format("/%s/%s/%s", SERVICE_NAME, sessionId, tabId);

        log.info("LLM 테스트 스트림 요청({} | {})", sessionId, tabId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(emitterKey, emitter);

        // 세션 만료 또는 연결 끊김 처리
        emitter.onCompletion(() -> emitters.remove(emitterKey));
        emitter.onTimeout(() -> emitters.remove(emitterKey));

        return emitter;
    }

    /**
     * LLM 테스트 질의 요청
     *
     * @param llmChatRequestDto 질의 정보
     * @param session           세션
     */
    @PostMapping("/chat")
    public ResponseEntity<ResponseDto<ChatResponseDto>> lawChat(@RequestBody LlmChatRequestDto llmChatRequestDto, HttpSession session) {

        String sessionId = session.getId();
        String tabId = llmChatRequestDto.getTabId();
        String emitterKey = String.format("/%s/%s/%s", SERVICE_NAME, sessionId, tabId);
        SseEmitter emitter = emitters.get(emitterKey);

        String queryEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.QUERY_EVENT_NAME, llmChatRequestDto.getTabId());
        String answerEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.ANSWER_EVENT_NAME, llmChatRequestDto.getTabId());

        String query = llmChatRequestDto.getQuery();
        String context = llmChatRequestDto.getContext();
        String promptContext = llmChatRequestDto.getPrompt();

        if (emitter != null) {
            log.info("LLM 테스트 질문 요청({} | {}) | {}\n{}\n{}", sessionId, tabId, query, context, promptContext);

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
                                    .tabId(tabId)
                                    .build())
                            .build());
        } else {
            log.error("LLM 테스트 스트림 없음({} | {})", sessionId, tabId);

            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(ResponseDto.<ChatResponseDto>builder()
                            .status("ERROR")
                            .message("답변 스트림이 열리지 않음")
                            .data(ChatResponseDto.builder()
                                    .query(query)
                                    .tabId(tabId)
                                    .build())
                            .build());
        }
    }
}