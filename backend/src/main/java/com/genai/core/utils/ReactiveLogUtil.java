package com.genai.core.utils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.ContextView;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ReactiveLogUtil {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String DEFAULT_TRACE_ID = "NO_TRACE_ID";

    public enum Message {
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
        LLM_RESPONSE_STREAM_MESSAGE("LLM Stream 요청 성공", "LLM stream API request & response", List.of("LLM Instance ID","Request ID", "URL", "Request Body", "Response Body")),
        PART_EXPORT_MESSAGE("핵심 부분 추출", "Part content export", List.of("Contents", "Part Export")),
        PART_EXPORT_RECURSIVE_MESSAGE("핵심 부분 추출 재귀 배치", "Part content export recursive", List.of("Round","Content Length", "Content Size")),
        WHOLE_PART_EXPORT_MESSAGE("핵심 부분 추출 문자열 병합", "Whole part contents export", List.of("Contents", "Whole Part Export")),
        PART_EXPORTS_SUMMARY_MESSAGE("핵심 부분 추출 병합 문자열 요약", "Part content export", List.of("Contents", "Summary")),
        PART_TRANSLATE_MESSAGE("부분 번역", "Part content translate", List.of("Contents", "Translate")),
        PART_TRANSLATE_RECURSIVE_MESSAGE("부분 번역 재귀 배치", "Part content translate", List.of("Content Size")),
        ;

        private final String message;

        Message(String description, String title, List<String> contentNames) {
            this.message = generateMessage(title, contentNames);
        }

        private static final int TITLE_MAX_LENGTH        = 80;
        private static final int CONTENT_NAME_MAX_LENGTH = 25;

        private static String generateMessage(String title, List<String> contentNames) {

            StringBuilder messageBuilder = new StringBuilder();

            messageBuilder.append("# ").append(String.format("%-" + TITLE_MAX_LENGTH + "s", title)).append("\n");

            for (String contentName : contentNames) {;
                messageBuilder
                        .append("\t- ")
                        .append(String.format("%-" + CONTENT_NAME_MAX_LENGTH + "s", contentName))
                        .append(": {}")
                        .append("\n");
            }

            return "\n" + messageBuilder.toString().trim();
        }
    }

    private static String traceId(ContextView ctx) {
        return ctx.getOrDefault(TRACE_ID_KEY, DEFAULT_TRACE_ID);
    }

    private static Object[] withTraceId(String traceId, Object[] args) {
        return withTraceId(traceId, args, null);
    }

    private static Object[] withTraceId(String traceId, Object[] args, Throwable throwable) {
        Object[] newArgs = new Object[args.length + 2];
        newArgs[0] = traceId;
        newArgs[newArgs.length - 1] = throwable;
        System.arraycopy(args, 0, newArgs, 1, args.length);

        return Arrays.stream(newArgs)
                .map(o -> {
                    if (o instanceof String str) {
                        return str.replace("\n", "\\n");
                    }
                    return o;
                })
                .toArray();
    }

    private static <T> T safeGet(Signal<T> signal) {
        try {
            return signal.get();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Info 로그
     * @param message 메시지
     * @param mapper 메시지 값
     */
    public static <T> Consumer<Signal<T>> info(Message message, Function<T, Object[]> mapper) {
        return info(message.message, mapper);
    }

    /**
     * Info 로그
     * @param message 메시지
     * @param mapper 메시지 값
     */
    public static <T> Consumer<Signal<T>> info(String message, Function<T, Object[]> mapper) {
        return signal -> {
            if (!signal.isOnNext()) return;

            ContextView ctx = signal.getContextView();
            String traceId = traceId(ctx);

            T value = safeGet(signal);
            Object[] args = mapper.apply(value);

            log.info("[{}] " + message, withTraceId(traceId, args));
        };
    }

    /**
     * Debug 로그
     * @param message 메시지
     * @param mapper 메시지 값
     */
    public static <T> Consumer<Signal<T>> debug(Message message, Function<T, Object[]> mapper) {
        return debug(message.message, mapper);
    }

    /**
     * Debug 로그
     * @param message 메시지
     * @param mapper 메시지 값
     */
    public static <T> Consumer<Signal<T>> debug(String message, Function<T, Object[]> mapper) {
        return signal -> {
            if (!signal.isOnNext()) return;

            ContextView ctx = signal.getContextView();
            String traceId = traceId(ctx);

            T value = safeGet(signal);
            Object[] args = mapper.apply(value);

            log.debug("[{}] " + message, withTraceId(traceId, args));
        };
    }

    /**
     * Error 로그 및 기본 값 반환
     * @param message 메시지
     * @param args 메시지 값
     * @param fallback 기본 값
     */
    public static <T> Function<Throwable, Mono<T>> errorResume(Message message, Object[] args, T fallback) {
        return errorResume(message.message, args, fallback);
    }

    /**
     * Error 로그 및 기본 값 반환
     * @param message 메시지
     * @param args 메시지 값
     * @param fallback 기본 값
     */
    public static <T> Function<Throwable, Mono<T>> errorResume(String message, Object[] args, T fallback) {
        return e ->
                Mono.deferContextual(ctx -> {
                    String traceId = traceId(ctx);

                    log.error("[{}] " + message, withTraceId(traceId, args, e));

                    return fallback != null
                            ? Mono.just(fallback)
                            : Mono.empty();
                });
    }
}