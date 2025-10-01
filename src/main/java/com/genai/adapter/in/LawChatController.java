package com.genai.adapter.in;

import com.genai.adapter.in.dto.request.LawChatRequestDto;
import com.genai.adapter.in.dto.response.ChatResponseDto;
import com.genai.adapter.in.dto.response.ResponseDto;
import com.genai.application.service.ChatService;
import com.genai.application.vo.QuestionLawVo;
import com.genai.constant.ChatConst;
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
@RequestMapping("/law")
public class LawChatController {

    private static final String SERVICE_NAME = "law";
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

        log.info("법령 사용자 스트림 요청({})", sessionId);

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
            log.info("법령 사용자 스트림 종료({})", sessionId);
            emitters.remove(emitterKey);
        });
        emitter.onTimeout(() -> {
            log.info("법령 사용자 스트림 타임 아웃({})", sessionId);
            emitters.remove(emitterKey);
        });

        return emitter;
    }

    /**
     * 법령 질의 요청
     *
     * @param lawChatRequestDto 질의 정보
     */
    @PostMapping("/chat")
    public ResponseEntity<ResponseDto<ChatResponseDto>> lawChat(@RequestBody LawChatRequestDto lawChatRequestDto) {

        String sessionId = lawChatRequestDto.getSessionId();
        String emitterKey = String.format("/%s/%s", SERVICE_NAME, sessionId);
        SseEmitter emitter = emitters.get(emitterKey);

        String queryEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.QUERY_EVENT_NAME, sessionId);
        String answerEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.ANSWER_EVENT_NAME, sessionId);

        String query = lawChatRequestDto.getQuery();

        log.info("법령 사용자 질문 요청({}) | {}", sessionId, query);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(queryEventName).data(query));
                emitter.send(SseEmitter.event().name(answerEventName).data(ChatConst.ANSWER_START_PREFIX));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }

            QuestionLawVo questionLawVo = chatService.questionLawUseCase(query, sessionId);

            StringBuilder answerBuilder = new StringBuilder();
            questionLawVo.getAnswerStream()
                    .subscribe(message -> {
                                try {
                                    if (ChatConst.STREAM_OVER_PREFIX.equals(message)) {
                                        emitter.send(SseEmitter.event().name(answerEventName).data(ChatConst.ANSWER_END_PREFIX));
                                        emitter.complete();
                                    } else {
                                        answerBuilder.append(message);
                                        emitter.send(SseEmitter.event().name(answerEventName).data(message));
                                    }
                                } catch (IOException e) {
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            () -> {
                                log.info("법령 사용자 답변 완료({}) | {} | {}", sessionId, query, answerBuilder);
                                emitter.complete();
                            }
                    );

            return ResponseEntity.ok()
                    .body(ResponseDto.<ChatResponseDto>builder()
                            .status("SUCCESS")
                            .message("법령 질의 요청 성공")
                            .data(ChatResponseDto.builder()
                                    .query(query)
                                    .sessionId(sessionId)
                                    .documents(questionLawVo.getDocuments())
                                    .build())
                            .build());
        } else {
            log.error("법령 사용자 스트림 없음({})", sessionId);

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