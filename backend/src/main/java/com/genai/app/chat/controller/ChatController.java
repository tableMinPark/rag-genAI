package com.genai.app.chat.controller;

import com.genai.app.chat.controller.dto.request.ChatAiRequestDto;
import com.genai.app.chat.controller.dto.request.ChatLlmRequestDto;
import com.genai.app.chat.controller.dto.request.ChatMyAiRequestDto;
import com.genai.app.chat.controller.dto.request.ChatSimulationRequestDto;
import com.genai.app.chat.controller.dto.response.GetCategoriesResponseDto;
import com.genai.app.chat.controller.dto.response.GetChatDetailResponseDto;
import com.genai.app.chat.controller.dto.response.GetChatResponseDto;
import com.genai.app.chat.service.ChatService;
import com.genai.app.chat.service.vo.ChatDetailVO;
import com.genai.app.chat.service.vo.ChatVO;
import com.genai.global.wrapper.PageWrapper;
import com.genai.app.myai.constant.MyAiConst;
import com.genai.app.myai.service.MyAiService;
import com.genai.app.myai.service.vo.ProjectVO;
import com.genai.core.constant.CommonConst;
import com.genai.core.constant.PromptConst;
import com.genai.core.service.business.QuestionCoreService;
import com.genai.core.service.business.StreamCoreService;
import com.genai.core.service.business.vo.QuestionVO;
import com.genai.core.service.module.CommonCodeModuleService;
import com.genai.core.service.module.vo.CommonCodeVO;
import com.genai.global.dto.ResponseDto;
import com.genai.global.enums.Menu;
import com.genai.global.enums.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final QuestionCoreService questionCoreService;
    private final StreamCoreService streamCoreService;
    private final CommonCodeModuleService commonCodeModuleService;
    private final ChatService chatService;
    private final MyAiService myAiService;

    /**
     * AI 질의 답변 요청
     *
     * @param chatAiRequestDto 질의 정보
     */
    @PostMapping("/ai")
    public SseEmitter chatAi(@Valid @RequestBody ChatAiRequestDto chatAiRequestDto) {

        String userId = "USER";
        String sessionId = chatAiRequestDto.getSessionId();
        String query = chatAiRequestDto.getQuery();

        long chatId = chatService.getChat(userId, query, Menu.MENU_AI).getChatId();
        long promptId = PromptConst.QUESTION_AI_PROMPT_ID;
        QuestionVO questionVO = questionCoreService.questionAi(query, sessionId, chatId, promptId, chatAiRequestDto.getCategoryCodes());

        return streamCoreService.createStream(sessionId).subscribeWithTrace(questionVO.streamFlux(), questionVO.streamEndMono());
    }

    /**
     * LLM 질의 답변 요청
     *
     * @param chatLlmRequestDto 질의 정보
     */
    @PostMapping("/llm")
    public SseEmitter chatLlm(@Valid @RequestBody ChatLlmRequestDto chatLlmRequestDto) {

        String userId = "USER";
        String sessionId = chatLlmRequestDto.getSessionId();
        String query = chatLlmRequestDto.getQuery();

        long chatId = chatService.getChat(userId, query, Menu.MENU_LLM).getChatId();
        long promptId = PromptConst.QUESTION_PROMPT_ID;
        QuestionVO questionVO = questionCoreService.questionLlm(query, sessionId, chatId, promptId);

        return streamCoreService.createStream(sessionId).subscribeWithTrace(questionVO.streamFlux(), questionVO.streamEndMono());
    }

    /**
     * 나만의 AI 질의 답변 요청
     *
     * @param chatMyAiRequestDto 질의 정보
     */
    @PostMapping("/myai")
    public SseEmitter chatMyAi(@Valid @RequestBody ChatMyAiRequestDto chatMyAiRequestDto) {

        String userId = "USER";
        String sessionId = chatMyAiRequestDto.getSessionId();
        String query = chatMyAiRequestDto.getQuery();
        Long projectId = chatMyAiRequestDto.getProjectId();

        ProjectVO projectVO = myAiService.getProject(userId, projectId);

        long chatId = chatService.getChat(userId, query, Menu.MENU_MYAI).getChatId();
        long promptId = projectVO.getPromptId();
        String categoryCode = MyAiConst.categoryCode(projectId);
        QuestionVO questionVO = questionCoreService.questionMyAi(query, sessionId, chatId, promptId, categoryCode);

        return streamCoreService.createStream(sessionId).subscribeWithTrace(questionVO.streamFlux(), questionVO.streamEndMono());
    }

    /**
     * LLM Simulation 질의 답변 요청
     *
     * @param chatSimulationRequestDto 질의 정보
     */
    @PostMapping("/simulation")
    public SseEmitter chatSimulation(@Valid @RequestBody ChatSimulationRequestDto chatSimulationRequestDto) {

        String userId = "USER";
        String sessionId = chatSimulationRequestDto.getSessionId();
        String query = chatSimulationRequestDto.getQuery();
        String context = chatSimulationRequestDto.getContext();
        String promptContent = chatSimulationRequestDto.getPromptContent();
        int maxTokens = chatSimulationRequestDto.getMaxTokens();
        double temperature = chatSimulationRequestDto.getTemperature();
        double topP = chatSimulationRequestDto.getTopP();

        long chatId = chatService.getChat(userId, query, Menu.MENU_SIMULATION).getChatId();
        QuestionVO questionVO = questionCoreService.questionSimulation(
                query, sessionId, chatId, context, promptContent, temperature, topP, maxTokens);

        return streamCoreService.createStream(sessionId).subscribeWithTrace(questionVO.streamFlux());
    }

    /**
     * 카테고리 목록 조회
     */
    @GetMapping("/category")
    public ResponseEntity<ResponseDto<List<GetCategoriesResponseDto>>> getCategories() {

        List<CommonCodeVO> categoryCodes = commonCodeModuleService.getCommonCodes(CommonConst.CHUNK_CODE_GROUP);

        return ResponseEntity.ok().body(Response.CHAT_CATEGORIES_SUCCESS.toResponseDto(GetCategoriesResponseDto.toList(categoryCodes)));
    }

    /**
     * 대화 목록 조회
     *
     * @param menuCode 메뉴 코드
     * @param page     페이지
     * @param size     사이즈
     */
    @GetMapping("/chats")
    public ResponseEntity<ResponseDto<PageWrapper<GetChatResponseDto>>> getChats(
            @RequestParam("menuCode") String menuCode,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    ) {
        String userId = "USER";

        PageWrapper<ChatVO> chatPage = chatService.getChats(userId, menuCode, page, size);

        PageWrapper<GetChatResponseDto> result = PageWrapper.<GetChatResponseDto>builder()
                .content(GetChatResponseDto.toList(chatPage.getContent()))
                .isLast(chatPage.isLast())
                .pageNo(chatPage.getPageNo())
                .pageSize(chatPage.getPageSize())
                .totalCount(chatPage.getTotalCount())
                .totalPages(chatPage.getTotalPages())
                .build();

        return ResponseEntity.ok().body(Response.CHAT_LIST_SUCCESS.toResponseDto(result));
    }

    /**
     * 대화 이력 목록 조회
     *
     * @param chatId 대화 ID
     * @param page   페이지
     * @param size   사이즈
     */
    @GetMapping("/history")
    public ResponseEntity<ResponseDto<List<GetChatDetailResponseDto>>> getChatDetails(
            @RequestParam("chatId") long chatId,
            @RequestParam("page") int page,
            @RequestParam("size") int size
    ) {
        String userId = "USER";

        List<ChatDetailVO> chatDetails = chatService.getChatDetails(userId, chatId, page, size);

        return ResponseEntity.ok().body(Response.CHAT_DETAILS_SUCCESS.toResponseDto(GetChatDetailResponseDto.toList(chatDetails)));
    }
}