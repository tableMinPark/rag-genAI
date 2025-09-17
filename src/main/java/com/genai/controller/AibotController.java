package com.genai.controller;

import com.genai.constant.ChatConst;
import com.genai.controller.dto.request.ChatAibotRequestDto;
import com.genai.service.ChatService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/aibot")
public class AibotController {

    private static final String SERVICE_NAME = "aibot";
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

        log.info("사용자 세션 요청({}/{})", sessionId, tabId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(emitterKey, emitter);

        // 세션 만료 또는 연결 끊김 처리
        emitter.onCompletion(() -> emitters.remove(emitterKey));
        emitter.onTimeout(() -> emitters.remove(emitterKey));

        return emitter;
    }

    /**
     * 질의
     *
     * @param chatAibotRequestDto 질의 정보
     * @param session             세션
     */
    @PostMapping("/chat")
    public void chatToAibot(@RequestBody ChatAibotRequestDto chatAibotRequestDto, HttpSession session) {

        String sessionId = session.getId();
        String emitterKey = String.format("/%s/%s/%s", SERVICE_NAME, sessionId, chatAibotRequestDto.getTabId());

        log.info("사용자 질문 요청({}/{}) | {}", sessionId, chatAibotRequestDto.getTabId(), chatAibotRequestDto.getQuery());

        SseEmitter emitter = emitters.get(emitterKey);

        String queryEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.QUERY_EVENT_NAME, chatAibotRequestDto.getTabId());
        String answerEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.ANSWER_EVENT_NAME, chatAibotRequestDto.getTabId());

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(queryEventName).data(chatAibotRequestDto.getQuery()));
                emitter.send(SseEmitter.event().name(answerEventName).data(ChatConst.ANSWER_START_PREFIX));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }

            chatService.questionStream(chatAibotRequestDto.getQuery(), sessionId, answer -> {
                try {
                    if (ChatConst.STREAM_OVER_PREFIX.equals(answer)) {
                        emitter.send(SseEmitter.event().name(answerEventName).data(ChatConst.ANSWER_END_PREFIX));
                        emitter.complete();
                    } else {
                        emitter.send(SseEmitter.event().name(answerEventName).data(answer));
                    }

                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            });
        }
    }
}