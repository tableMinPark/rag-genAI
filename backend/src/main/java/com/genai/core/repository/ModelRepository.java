package com.genai.core.repository;

import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.module.vo.ConversationVO;
import com.genai.core.type.LlmType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface ModelRepository {

    /**
     * 답변 생성 요청
     *
     * @param query        질의문
     * @param context      검색 결과 데이터
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 응답 문자열
     */
    String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity);

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 목록
     */
    List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity);

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 목록 Mono
     */
    Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity);

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @return 답변 엔티티 Flux
     */
    Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity);

    /**
     * 답변 생성 요청
     *
     * @param query        질의문
     * @param context      검색 결과 데이터
     * @param sessionId    세션 식별자
     * @param promptEntity 프롬 프트
     * @return 답변 응답 문자열
     */
    String generateAnswerSyncStr(String query, String context, String sessionId, PromptEntity promptEntity, LlmType llmType);

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 목록
     */
    List<AnswerEntity> generateAnswerSync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType);

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 목록 Mono
     */
    Mono<List<AnswerEntity>> generateAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType);

    /**
     * 답변 생성 요청
     *
     * @param query         질의문
     * @param context       검색 결과 데이터
     * @param chatState     대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 식별자
     * @param promptEntity  프롬 프트
     * @param llmType       LLM 타입
     * @return 답변 엔티티 Flux
     */
    Flux<AnswerEntity> generateStreamAnswerAsync(String query, String context, String chatState, List<ConversationVO> conversations, String sessionId, PromptEntity promptEntity, LlmType llmType);
}