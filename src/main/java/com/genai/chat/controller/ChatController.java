package com.genai.chat.controller;

import com.genai.chat.controller.dto.request.ChatAiRequestDto;
import com.genai.chat.controller.dto.request.ChatLlmRequestDto;
import com.genai.chat.controller.dto.request.ChatSimulationRequestDto;
import com.genai.chat.controller.dto.response.ChatAiResponseDto;
import com.genai.core.controller.dto.response.ResponseDto;
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
     * @param chatAiRequestDto 질의 정보
     */
    @PostMapping("/ai")
    public ResponseEntity<ResponseDto<ChatAiResponseDto>> chatAi(@RequestBody ChatAiRequestDto chatAiRequestDto) {

        String sessionId = chatAiRequestDto.getSessionId();
        String query = chatAiRequestDto.getQuery();

        log.info("사용자 질문 요청 : {} | {}", sessionId, query);

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        QuestionVO questionVO = questionCoreService.questionAi(
                query, sessionId, 2L, chatAiRequestDto.getCategoryCodes());

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok()
                .body(ResponseDto.<ChatAiResponseDto>builder()
                        .status("SUCCESS")
                        .message("AI 질의 요청 성공")
                        .data(ChatAiResponseDto.builder()
                                .query(query)
                                .sessionId(sessionId)
                                .documents(questionVO.documents())
                                .build())
                        .build());
    }

    /**
     * LLM 질의 답변 요청
     *
     * @param chatLlmRequestDto 질의 정보
     */
    @PostMapping("/llm")
    public ResponseEntity<ResponseDto<ChatAiResponseDto>> chatLlm(@RequestBody ChatLlmRequestDto chatLlmRequestDto) {

        String sessionId = chatLlmRequestDto.getSessionId();
        String query = chatLlmRequestDto.getQuery();

        log.info("사용자 질문 요청 : {} | {}", sessionId, query);

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        QuestionVO questionVO = questionCoreService.questionLlm(query, sessionId, 1L);

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok()
                .body(ResponseDto.<ChatAiResponseDto>builder()
                        .status("SUCCESS")
                        .message("Llm 질의 요청 성공")
                        .data(ChatAiResponseDto.builder()
                                .query(query)
                                .sessionId(sessionId)
                                .documents(Collections.emptyList())
                                .build())
                        .build());
    }

    /**
     * LLM Simulation 질의 답변 요청
     *
     * @param chatSimulationRequestDto 질의 정보
     */
    @PostMapping("/simulation")
    public ResponseEntity<ResponseDto<ChatAiResponseDto>> chatSimulation(@RequestBody ChatSimulationRequestDto chatSimulationRequestDto) {

        String sessionId = chatSimulationRequestDto.getSessionId();
        String query = chatSimulationRequestDto.getQuery();
        String context = chatSimulationRequestDto.getContext();
        String promptContext = chatSimulationRequestDto.getPrompt();
        int maxTokens = chatSimulationRequestDto.getMaxTokens();
        double temperature = chatSimulationRequestDto.getTemperature();
        double topP = chatSimulationRequestDto.getTopP();

        log.info("사용자 질문 요청 : {} | {}", sessionId, query);

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 0L;
        QuestionVO questionVO = questionCoreService.questionSimulation(
                query, sessionId, chatId, context, promptContext, temperature, topP, maxTokens);

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok()
                .body(ResponseDto.<ChatAiResponseDto>builder()
                        .status("SUCCESS")
                        .message("Simulation 질의 요청 성공")
                        .data(ChatAiResponseDto.builder()
                                .query(query)
                                .sessionId(sessionId)
                                .documents(Collections.emptyList())
                                .build())
                        .build());
    }
}