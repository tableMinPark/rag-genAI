package com.genai.chat.controller;

import com.genai.chat.controller.dto.request.ChatAiRequestDto;
import com.genai.chat.controller.dto.request.ChatLlmRequestDto;
import com.genai.chat.controller.dto.request.ChatMyAiRequestDto;
import com.genai.chat.controller.dto.request.ChatSimulationRequestDto;
import com.genai.chat.controller.dto.response.ChatAiResponseDto;
import com.genai.chat.controller.dto.response.ChatMyAiResponseDto;
import com.genai.chat.controller.dto.response.GetCategoriesResponseDto;
import com.genai.core.config.constant.ComnConst;
import com.genai.core.config.constant.PromptConst;
import com.genai.core.controller.dto.response.ResponseDto;
import com.genai.core.service.ComnCodeCoreService;
import com.genai.core.service.QuestionCoreService;
import com.genai.core.service.StreamCoreService;
import com.genai.core.service.subscriber.StreamSubscriber;
import com.genai.core.service.vo.ComnCodeVO;
import com.genai.core.service.vo.QuestionVO;
import com.genai.global.enums.Response;
import com.genai.myai.constant.MyAiConst;
import com.genai.myai.service.MyAiService;
import com.genai.myai.service.vo.ProjectVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final QuestionCoreService questionCoreService;
    private final StreamCoreService streamCoreService;
    private final ComnCodeCoreService comnCodeCoreService;
    private final MyAiService myAiService;

    /**
     * AI 질의 답변 요청
     *
     * @param chatAiRequestDto 질의 정보
     */
    @PostMapping("/ai")
    public ResponseEntity<ResponseDto<ChatAiResponseDto>> chatAi(@Valid @RequestBody ChatAiRequestDto chatAiRequestDto) {

        String sessionId = chatAiRequestDto.getSessionId();
        String query = chatAiRequestDto.getQuery();

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 1L;
        long promptId = PromptConst.QUESTION_AI_PROMPT_ID;
        QuestionVO questionVO = questionCoreService.questionAi(query, sessionId, chatId, promptId, chatAiRequestDto.getCategoryCodes());

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.CHAT_AI_SUCCESS.toResponseDto(ChatAiResponseDto.builder()
                .query(query)
                .sessionId(sessionId)
                .documents(questionVO.documents())
                .build()));
    }

    /**
     * LLM 질의 답변 요청
     *
     * @param chatLlmRequestDto 질의 정보
     */
    @PostMapping("/llm")
    public ResponseEntity<ResponseDto<ChatAiResponseDto>> chatLlm(@Valid @RequestBody ChatLlmRequestDto chatLlmRequestDto) {

        String sessionId = chatLlmRequestDto.getSessionId();
        String query = chatLlmRequestDto.getQuery();

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 2L;
        long promptId = PromptConst.QUESTION_PROMPT_ID;
        QuestionVO questionVO = questionCoreService.questionLlm(query, sessionId, chatId, promptId);

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.CHAT_LLM_SUCCESS.toResponseDto(ChatAiResponseDto.builder()
                .query(query)
                .sessionId(sessionId)
                .documents(Collections.emptyList())
                .build()));
    }

    /**
     * LLM Simulation 질의 답변 요청
     *
     * @param chatSimulationRequestDto 질의 정보
     */
    @PostMapping("/simulation")
    public ResponseEntity<ResponseDto<ChatAiResponseDto>> chatSimulation(@Valid @RequestBody ChatSimulationRequestDto chatSimulationRequestDto) {

        String sessionId = chatSimulationRequestDto.getSessionId();
        String query = chatSimulationRequestDto.getQuery();
        String context = chatSimulationRequestDto.getContext();
        String promptContext = chatSimulationRequestDto.getPrompt();
        int maxTokens = chatSimulationRequestDto.getMaxTokens();
        double temperature = chatSimulationRequestDto.getTemperature();
        double topP = chatSimulationRequestDto.getTopP();

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 3L;
        QuestionVO questionVO = questionCoreService.questionSimulation(
                query, sessionId, chatId, context, promptContext, temperature, topP, maxTokens);

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.CHAT_SIMULATION_SUCCESS.toResponseDto(ChatAiResponseDto.builder()
                .query(query)
                .sessionId(sessionId)
                .documents(Collections.emptyList())
                .build()));
    }

    /**
     * 나만의 AI 질의 답변 요청
     *
     * @param chatMyAiRequestDto 질의 정보
     */
    @PostMapping("/myai")
    public ResponseEntity<ResponseDto<ChatMyAiResponseDto>> chatMyAi(@Valid @RequestBody ChatMyAiRequestDto chatMyAiRequestDto) {

        String sessionId = chatMyAiRequestDto.getSessionId();
        String query = chatMyAiRequestDto.getQuery();
        Long projectId = chatMyAiRequestDto.getProjectId();

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        ProjectVO projectVO = myAiService.getProject(projectId);

        long chatId = 4L;
        long promptId = projectVO.getPromptId();
        String categoryCode = MyAiConst.MYAI_CATEGORY_CODE(projectId);
        QuestionVO questionVO = questionCoreService.questionMyAi(query, sessionId, chatId, promptId, categoryCode);

        questionVO.answerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.CHAT_MYAI_SUCCESS.toResponseDto(ChatMyAiResponseDto.builder()
                .query(query)
                .sessionId(sessionId)
                .documents(questionVO.documents())
                .build()));
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/category")
    public ResponseEntity<ResponseDto<List<GetCategoriesResponseDto>>> getCategories() {

        List<ComnCodeVO> categoryCodes = comnCodeCoreService.getComnCodes(ComnConst.CHUNK_CODE_GROUP);

        return ResponseEntity.ok().body(Response.CHAT_CATEGORIES_SUCCESS.toResponseDto(GetCategoriesResponseDto.toList(categoryCodes)));
    }
}