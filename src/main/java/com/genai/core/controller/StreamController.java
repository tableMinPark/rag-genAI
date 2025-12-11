package com.genai.core.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/biz/stream")
public class StreamController {

    private static final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Emitter 조회
     *
     * @param sessionId 세션 ID
     * @return SSE Emitter
     */
    @GetMapping("/{serviceName}/{sessionId}")
    public SseEmitter stream(
            @PathVariable("serviceName") String serviceName,
            @PathVariable("sessionId") String sessionId
    ) {

        String emitterKey = String.format("/%s/%s", serviceName, sessionId);

        log.info("{} 스트림 요청({})", serviceName, sessionId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(emitterKey, emitter);
        try {
            // 연결 확인 코멘트 전송
            emitter.send(SseEmitter.event().comment(sessionId));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }

        // 세션 만료 또는 연결 끊김 처리
        emitter.onCompletion(() -> {
            log.info("{} 사용자 스트림 종료({})", serviceName, sessionId);
            emitters.remove(emitterKey);
        });
        emitter.onTimeout(() -> {
            log.info("{} 사용자 스트림 타임 아웃({})", serviceName, sessionId);
            emitters.remove(emitterKey);
        });

        return emitter;
    }
}
