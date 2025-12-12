package com.genai.chat.controller;

import com.genai.chat.controller.dto.request.ChatRequestDto;
import com.genai.chat.controller.dto.response.ChatResponseDto;
import com.genai.chat.controller.dto.response.ResponseDto;
import com.genai.core.service.QuestionCoreService;
import com.genai.core.service.impl.StreamCoreServiceImpl;
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
@RequestMapping("/mate")
public class MateChatController {

    private final QuestionCoreService questionCoreService;
    private final StreamCoreServiceImpl streamCoreServiceImpl;

    /**
     * 질의 답변 요청
     *
     * @param chatRequestDto 질의 정보
     */
    @PostMapping("/chat")
    public ResponseEntity<ResponseDto<ChatResponseDto>> chat(@RequestBody ChatRequestDto chatRequestDto) {

        String sessionId = chatRequestDto.getSessionId();
        String query = chatRequestDto.getQuery();

        log.info("사용자 질문 요청 : {} | {}", sessionId, query);

        StreamSubscriber streamSubscriber = streamCoreServiceImpl.getStream(sessionId);

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
}