package com.genai.core.common.enums;

import com.genai.global.common.utils.ReactiveLogMessage;
import lombok.Getter;

import java.util.List;

public enum CoreLogMessage implements ReactiveLogMessage {

    PREVIOUS_CONVERSATIONS_MESSAGE("대화 이력 목록 조회", "Previous conversations get", List.of("Conversations")),
    REWRITE_QUERY_MESSAGE("재정의 질의 생성", "Rewrite query generate", List.of("Rewrite Query")),
    MULTITURN_CONVERSATIONS_MESSAGE("멀티턴을 위한 대화 이력 선별", "Multi-turn conversations pick / Topic change check", List.of("Is Change Topic", "Multi-turn Conversations")),
    RERANK_MESSAGE("검색 결과 리랭킹", "Search result reranking", List.of("Rerank Documents")),
    QUESTION_CONTEXT_MESSAGE("질의 컨텍스트 생성", "Question context generate", List.of("Is Change Topic", "Conversations", "Multi-turn Conversations", "User Query", "Rewrite Query", "Rerank Documents")),
    REFERENCE_MESSAGE("참고 문서", "Reference documents generate", List.of("Reference Documents")),
    CHAT_HISTORY_SAVE_MESSAGE("대화 이력 저장", "Chat history save", List.of("Chat ID ", "Msg ID", "User Query", "Rewrite Query", "Answer")),
    CHAT_STATE_MESSAGE("대화 상태 생성", "Chat state generate", List.of("Chat ID ", "Chat State")),
    LLM_INSTANCE_TRY_ACQUIRE_MESSAGE("LLM Instance 할당", "LLM Instance try acquire", List.of("LLM Instance ID", "Remaining Capacity")),
    LLM_INSTANCE_RELEASE_MESSAGE("LLM Instance 반납", "LLM Instance release", List.of("LLM Instance ID", "Remaining Capacity")),
    LLM_REQUEST_ERROR_MESSAGE("LLM 요청 실패", "LLM request error", List.of("LLM Instance ID", "Request ID", "URL", "Body")),
    LLM_RESPONSE_BLOCKING_MESSAGE("LLM Blocking 요청 성공", "LLM blocking API request & response", List.of("LLM Instance ID", "Request ID", "URL", "Request Body", "Response Body")),
    LLM_RESPONSE_STREAM_MESSAGE("LLM Stream 요청 성공", "LLM stream API request & response", List.of("LLM Instance ID", "Request ID", "URL", "Request Body", "Response Body")),
    PART_EXPORT_MESSAGE("핵심 부분 추출", "Part content export", List.of("Contents", "Part Export")),
    PART_EXPORT_RECURSIVE_MESSAGE("핵심 부분 추출 재귀 배치", "Part content export recursive", List.of("Round", "Content Length", "Content Size")),
    WHOLE_PART_EXPORT_MESSAGE("핵심 부분 추출 문자열 병합", "Whole part contents export", List.of("Contents", "Whole Part Export")),
    PART_EXPORTS_SUMMARY_MESSAGE("핵심 부분 추출 병합 문자열 요약", "Part content export", List.of("Contents", "Summary")),
    PART_TRANSLATE_MESSAGE("부분 번역", "Part content translate", List.of("Contents", "Translate")),
    PART_TRANSLATE_RECURSIVE_MESSAGE("부분 번역 재귀 배치", "Part content translate", List.of("Content Size")),
    ;

    @Getter
    private final String message;

    CoreLogMessage(String description, String title, List<String> contentNames) {
        this.message = generateMessage(title, contentNames);
    }

    private static final int TITLE_MAX_LENGTH = 80;
    private static final int CONTENT_NAME_MAX_LENGTH = 25;

    private static String generateMessage(String title, List<String> contentNames) {

        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("# ").append(String.format("%-" + TITLE_MAX_LENGTH + "s", title)).append("\n");

        for (String contentName : contentNames) {
            ;
            messageBuilder
                    .append("\t- ")
                    .append(String.format("%-" + CONTENT_NAME_MAX_LENGTH + "s", contentName))
                    .append(": {}")
                    .append("\n");
        }

        return "\n" + messageBuilder.toString().trim();
    }
}