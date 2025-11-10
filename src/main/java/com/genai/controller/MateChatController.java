package com.genai.controller;

import com.genai.controller.dto.request.ChatRequestDto;
import com.genai.controller.dto.response.ChatResponseDto;
import com.genai.controller.dto.response.ResponseDto;
import com.genai.controller.vo.ReferenceDocumentVo;
import com.genai.global.constant.ChatConst;
import com.genai.global.enums.MenuType;
import com.genai.service.ChatService;
import com.genai.service.domain.Answer;
import com.genai.service.vo.QuestionVo;
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
@RequestMapping("/mate")
public class MateChatController {

    private static final String SERVICE_NAME = "mate";
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

        log.info("{} 사용자 스트림 요청({})", SERVICE_NAME, sessionId);

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
            log.info("{} 사용자 스트림 종료({})", SERVICE_NAME, sessionId);
            emitters.remove(emitterKey);
        });
        emitter.onTimeout(() -> {
            log.info("{} 사용자 스트림 타임 아웃({})", SERVICE_NAME, sessionId);
            emitters.remove(emitterKey);
        });

        return emitter;
    }

    /**
     * RAG 질의 요청
     *
     * @param chatRequestDto 질의 정보
     */
    @PostMapping("/chat")
    public ResponseEntity<ResponseDto<ChatResponseDto>> lawChat(@RequestBody ChatRequestDto chatRequestDto) {

        String sessionId = chatRequestDto.getSessionId();
        String emitterKey = String.format("/%s/%s", SERVICE_NAME, sessionId);
        SseEmitter emitter = emitters.get(emitterKey);

        String queryEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.QUERY_EVENT_NAME, sessionId);
        String answerEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.ANSWER_EVENT_NAME, sessionId);
        String inferenceEventName = String.format("/%s/%s/%s", SERVICE_NAME, ChatConst.INFERENCE_EVENT_NAME, sessionId);

        String query = chatRequestDto.getQuery();

        log.info("{} 사용자 질문 요청({}) | {}", SERVICE_NAME, sessionId, query);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(queryEventName).data(query));
                emitter.send(SseEmitter.event().name(inferenceEventName).data(ChatConst.STREAM_START_PREFIX));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }

            QuestionVo questionVo = chatService.questionRagUseCase(query, sessionId, MenuType.MATE);
            questionVo.answerStream()
                    .subscribe(answers -> {
                                for (Answer answer : answers) {
                                    try {
                                        if (answer.isInference()) {
                                            emitter.send(SseEmitter.event().name(inferenceEventName).data(answer.getConvertContent()));
                                        } else {
                                            emitter.send(SseEmitter.event().name(answerEventName).data(answer.getConvertContent()));
                                        }
                                    } catch (IOException e) {
                                        emitter.completeWithError(e);
                                    }
                                }
                            },
                            emitter::completeWithError,
                            emitter::complete
                    );

            return ResponseEntity.ok()
                    .body(ResponseDto.<ChatResponseDto>builder()
                            .status("SUCCESS")
                            .message(SERVICE_NAME + " 질의 요청 성공")
                            .data(ChatResponseDto.builder()
                                    .query(query)
                                    .sessionId(sessionId)
                                    .documents(questionVo.documents().stream()
                                            .map(document -> ReferenceDocumentVo.builder()
                                                    .title(document.getTitle())
                                                    .subTitle(document.getSubTitle())
                                                    .thirdTitle(document.getThirdTitle())
                                                    .content(document.getContent())
                                                    .subContent(document.getSubContent())
                                                    .filePath(document.getFilePath())
                                                    .url(document.getUrl())
                                                    .docType(document.getDocType())
                                                    .categoryCode(document.getCategoryCode())
                                                    .build())
                                            .toList())
                                    .build())
                            .build());
        } else {
            log.error("{} 사용자 스트림 없음({})", SERVICE_NAME, sessionId);

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