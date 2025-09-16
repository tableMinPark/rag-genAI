package com.genai.controller;

import com.genai.constant.ChatConst;
import com.genai.controller.dto.request.ChatRequestDto;
import com.genai.service.ChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ChatController {

    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    private final ChatService chatService;

    /**
     * Emitter 조회
     *
     * @param session 세션
     * @return SSE Emitter
     */
    @GetMapping("/stream")
    public SseEmitter stream(HttpSession session) {
        String sessionId = session.getId();

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(sessionId, emitter);

        // 세션 만료 또는 연결 끊김 처리
        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> emitters.remove(sessionId));

        return emitter;
    }

    /**
     * 질의
     *
     * @param chatRequestDto 질의 정보
     * @param session        세션
     */
    @PostMapping("/chat")
    public void chat(@RequestBody ChatRequestDto chatRequestDto, HttpSession session) {
        String sessionId = session.getId();
        SseEmitter emitter = emitters.get(sessionId);

        log.info("사용자 질의({}) | {}", sessionId, chatRequestDto.getQuery());

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(ChatConst.QUERY_EVENT_NAME).data(chatRequestDto.getQuery()));
                emitter.send(SseEmitter.event().name(ChatConst.ANSWER_EVENT_NAME).data(ChatConst.ANSWER_START_PREFIX));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }

            chatService.questionStream(chatRequestDto.getQuery(), sessionId, answer -> {
                try {
                    if (ChatConst.STREAM_OVER_PREFIX.equals(answer)) {
                        emitter.send(SseEmitter.event().name(ChatConst.ANSWER_EVENT_NAME).data(ChatConst.ANSWER_END_PREFIX));
                        emitter.complete();
                    } else {
                        emitter.send(SseEmitter.event().name(ChatConst.ANSWER_EVENT_NAME).data(answer));
                    }

                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            });
        }
    }
}