package com.genai.core.repository;

import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.type.LlmType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ModelRepository {

    String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity);

    List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity);

    Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity);

    Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity);

    String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity, LlmType llmType);

    List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType);

    Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType);

    Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType);
}