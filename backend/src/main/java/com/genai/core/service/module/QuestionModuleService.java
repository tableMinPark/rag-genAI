package com.genai.core.service.module;

import com.genai.core.repository.vo.ConversationVO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface QuestionModuleService {

    Flux<ConversationVO> getConversations(long chatId);

    Mono<String> rewriteQuery(String query, List<ConversationVO> conversations, String sessionId);

    Mono<String> summaryState(String chatState, List<ConversationVO> conversations, String sessionId);
}
