package com.genai.core.utils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.ContextView;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ReactiveLogUtil {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String DEFAULT_TRACE_ID = "NO_TRACE_ID";

    public enum Message {
        PREVIOUS_CONVERSATIONS_MESSAGE("Previous conversations get", List.of("Conversations")),
        REWRITE_QUERY_MESSAGE("Rewrite query generate", List.of("Rewrite Query")),
        MULTITURN_CONVERSATIONS_MESSAGE("Multi-turn conversations pick / Topic change check", List.of("Is Change Topic", "Multi-turn Conversations")),
        RERANK_MESSAGE("Search result reranking", List.of("Rerank Documents")),
        QUESTION_CONTEXT_MESSAGE("Question context generate", List.of("Is Change Topic", "Conversations", "Multi-turn Conversations", "User Query", "Rewrite Query", "Rerank Documents")),
        REFERENCE_MESSAGE("Reference documents generate", List.of("Reference Documents")),
        CHAT_HISTORY_SAVE_MESSAGE("Chat history save", List.of("Chat ID ", "Msg ID", "User Query", "Rewrite Query", "Answer")),
        CHAT_STATE_MESSAGE("Chat state generate", List.of("Chat ID ", "Chat State")),
        LLM_INSTANCE_TRY_ACQUIRE_MESSAGE("LLM Instance try acquire", List.of("LLM Instance ID", "Remaining Capacity")),
        LLM_INSTANCE_RELEASE_MESSAGE("LLM Instance release", List.of("LLM Instance ID", "Remaining Capacity")),
        LLM_REQUEST_ERROR_MESSAGE("LLM request error", List.of("LLM Instance ID", "Request ID", "URL", "Body")),
        LLM_RESPONSE_BLOCKING_MESSAGE("LLM blocking API request & response", List.of("LLM Instance ID", "Request ID", "URL", "Request Body", "Response Body")),
        LLM_RESPONSE_STREAM_MESSAGE("LLM stream API request & response", List.of("LLM Instance ID","Request ID", "URL", "Request Body", "Response Body")),
        PART_EXPORT_MESSAGE("Part content export", List.of("Contents", "Part Export")),
        PART_EXPORT_RECURSIVE_MESSAGE("Part content export recursive", List.of("Round","Content Length", "Content Size")),
        WHOLE_PART_EXPORT_MESSAGE("Whole part contents export", List.of("Contents", "Whole Part Export")),
        PART_EXPORTS_SUMMARY_MESSAGE("Part content export", List.of("Contents", "Summary")),
        PART_TRANSLATE_MESSAGE("Part content translate", List.of("Contents", "Translate")),
        WHOLE_TRANSLAETE_MESSAGE("Whole contents translate", List.of("Contents", "Content Size")),
        ;

        private final String message;

        Message(String title, List<String> contentNames) {
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
        return newArgs;
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