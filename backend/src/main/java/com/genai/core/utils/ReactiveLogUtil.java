package com.genai.core.utils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.ContextView;

import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ReactiveLogUtil {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String DEFAULT_TRACE_ID = "NO_TRACE_ID";

    /* =========================
     * 공통 유틸
     * ========================= */
    private static String traceId(ContextView ctx) {
        return ctx.getOrDefault(TRACE_ID_KEY, DEFAULT_TRACE_ID);
    }

    private static Object[] withTraceId(String traceId, Object[] args) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = traceId;
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

    /* =========================
     * 진행중 (onNext)
     * ========================= */
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

    /* =========================
     * 에러 로그
     * ========================= */
    public static <T> Consumer<Signal<T>> error(String message) {
        return signal -> {
            if (!signal.isOnError()) return;

            ContextView ctx = signal.getContextView();
            String traceId = traceId(ctx);

            log.error("[{}] " + message, traceId, signal.getThrowable());
        };
    }

    /* =========================
     * 에러 + fallback
     * ========================= */
    public static <T> Function<Throwable, Mono<T>> errorResume(String message, Object[] args, T fallback) {
        return e ->
                Mono.deferContextual(ctx -> {
                    String traceId = traceId(ctx);

                    log.error("[{}] " + message,
                            withTraceId(traceId, args),
                            e
                    );

                    return fallback != null
                            ? Mono.just(fallback)
                            : Mono.empty();
                });
    }

    public static <T> Function<Throwable, Flux<T>> fluxErrorResume(String message, Object[] args, T fallback) {
        return e ->
                Flux.deferContextual(ctx -> {
                    String traceId = ctx.getOrDefault(TRACE_ID_KEY, DEFAULT_TRACE_ID);

                    log.error("[{}] " + message,
                            withTraceId(traceId, args),
                            e
                    );

                    if (fallback != null) {
                        return Flux.just(fallback);
                    }
                    return Flux.empty();
                });
    }

    public static final String PREVIOUS_CONVERSATIONS_MESSAGE = generateMessage(
            "Previous conversations get",
            "Conversations"
    );

    public static final String REWRITE_QUERY_MESSAGE = generateMessage(
            "Rewrite query generate",
            "Rewrite Query"
    );

    public static final String MULTITURN_CONVERSATIONS_MESSAGE = generateMessage(
            "Multi-turn conversations pick / Topic change check",

            "Is Change Topic",
            "Multi-turn Conversations"
    );

    public static final String RERANK_MESSAGE = generateMessage(
            "Search result reranking",
            "Rerank Documents"
    );

    public static final String QUESTION_CONTEXT_MESSAGE = generateMessage(
            "Question context generate",
            "Is Change Topic",
            "Conversations",
            "Multi-turn Conversations",
            "User Query",
            "Rewrite Query",
            "Rerank Documents"
    );

    public static final String REFERENCE_MESSAGE = generateMessage(
            "Reference documents generate",
            "Reference Documents"
    );

    public static final String CHAT_HISTORY_SAVE_MESSAGE = generateMessage(
            "Chat history save",
            "Chat ID ",
            "Msg ID",
            "User Query",
            "Rewrite Query",
            "Answer"
    );

    public static final String CHAT_STATE_MESSAGE = generateMessage(
            "Chat state generate",
            "Chat ID ",
            "Chat State"
    );

    public static final String LLM_INSTANCE_TRY_ACQUIRE_MESSAGE = generateMessage(
            "LLM Instance try acquire",
            "LLM Instance ID",
            "Remaining Capacity"
    );

    public static final String LLM_INSTANCE_RELEASE_MESSAGE = generateMessage(
            "LLM Instance release",
            "LLM Instance ID",
            "Remaining Capacity"
    );

    public static final String LLM_REQUEST_ERROR_MESSAGE = generateMessage(
            "LLM request error",
            "LLM Instance ID",
            "Request ID",
            "URL",
            "Body"
    );

    public static final String LLM_RESPONSE_BLOCKING_MESSAGE = generateMessage(
            "LLM blocking API request & response",
            "LLM Instance ID",
            "Request ID",
            "URL",
            "Request Body",
            "Response Body"
    );

    public static final String LLM_RESPONSE_STREAM_MESSAGE = generateMessage(
            "LLM stream API request & response",
            "LLM Instance ID",
            "Request ID",
            "URL",
            "Request Body",
            "Response Body"
    );

    public static final String PART_EXPORT_MESSAGE = generateMessage(
            "Part content export",
            "Content",
            "Part Export"
    );

    public static final String PART_EXPORTS_SUMMARY_MESSAGE = generateMessage(
            "Part content export",
            "Contents",
            "Summary"
    );

    public static final String PART_TRANSLATE_MESSAGE = generateMessage(
            "Part content translate",
            "Contents",
            "Translate"
    );

    private static final int TITLE_MAX_LENGTH        = 80;
    private static final int CONTENT_NAME_MAX_LENGTH = 25;

    private static String generateMessage(String title, String... contentNames) {

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