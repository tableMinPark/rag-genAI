package com.genai.chat.controller;

import com.genai.chat.controller.dto.request.ChatRequestDto;
import com.genai.chat.controller.dto.response.ChatResponseDto;
import com.genai.chat.controller.dto.response.ResponseDto;
import com.genai.core.service.QuestionCoreService;
import com.genai.core.service.StreamCoreService;
import com.genai.core.service.subscriber.StreamSubscriber;
import com.genai.core.service.vo.QuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final QuestionCoreService questionCoreService;
    private final StreamCoreService streamCoreService;

    /**
     * AI 질의 답변 요청
     *
     * @param chatRequestDto 질의 정보
     */
    @PostMapping("/ai")
    public ResponseEntity<ResponseDto<ChatResponseDto>> chatAi(@RequestBody ChatRequestDto chatRequestDto) {

        String sessionId = chatRequestDto.getSessionId();
        String query = chatRequestDto.getQuery();

        log.info("사용자 질문 요청 : {} | {}", sessionId, query);

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        QuestionVO questionVO = questionCoreService.questionAi(
                query, sessionId, chatRequestDto.getChatId(), Collections.emptyList());

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok()
                .body(ResponseDto.<ChatResponseDto>builder()
                        .status("SUCCESS")
                        .message("AI 질의 요청 성공")
                        .data(ChatResponseDto.builder()
                                .query(query)
                                .sessionId(sessionId)
                                .documents(questionVO.documents())
                                .build())
                        .build());
    }

    /**
     * LLM 질의 답변 요청
     *
     * @param chatRequestDto 질의 정보
     */
    @PostMapping("/llm")
    public ResponseEntity<ResponseDto<ChatResponseDto>> chatLlm(@RequestBody ChatRequestDto chatRequestDto) {

        String sessionId = chatRequestDto.getSessionId();
        String query = chatRequestDto.getQuery();

        log.info("사용자 질문 요청 : {} | {}", sessionId, query);

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        QuestionVO questionVO = questionCoreService.questionLlm(query, sessionId, chatRequestDto.getChatId());

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok()
                .body(ResponseDto.<ChatResponseDto>builder()
                        .status("SUCCESS")
                        .message("Llm 질의 요청 성공")
                        .data(ChatResponseDto.builder()
                                .query(query)
                                .sessionId(sessionId)
                                .documents(Collections.emptyList())
                                .build())
                        .build());
    }

    /**
     * LLM Simulation 질의 답변 요청
     *
     * @param chatRequestDto 질의 정보
     */
    @PostMapping("/simulation")
    public ResponseEntity<ResponseDto<ChatResponseDto>> chatSimulation(@RequestBody ChatRequestDto chatRequestDto) {

        String sessionId = chatRequestDto.getSessionId();
        String query = chatRequestDto.getQuery();
        String context = chatRequestDto.getContext();
        String promptContext = chatRequestDto.getPrompt();
        int maxTokens = chatRequestDto.getMaxTokens();
        double temperature = chatRequestDto.getTemperature();
        double topP = chatRequestDto.getTopP();

        log.info("사용자 질문 요청 : {} | {}", sessionId, query);

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = chatRequestDto.getChatId();
        QuestionVO questionVO = questionCoreService.questionSimulation(
                query, sessionId, chatId, context, promptContext, temperature, topP, maxTokens);

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok()
                .body(ResponseDto.<ChatResponseDto>builder()
                        .status("SUCCESS")
                        .message("Simulation 질의 요청 성공")
                        .data(ChatResponseDto.builder()
                                .query(query)
                                .sessionId(sessionId)
                                .documents(Collections.emptyList())
                                .build())
                        .build());
    }
}