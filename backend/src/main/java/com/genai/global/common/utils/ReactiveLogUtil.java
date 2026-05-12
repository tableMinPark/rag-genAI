package com.genai.global.common.utils;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.ContextView;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class ReactiveLogUtil {

    public static final String TRACE_ID_KEY = "traceId";
    private static final String DEFAULT_TRACE_ID = "NO_TRACE_ID";

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
    public static <T> Consumer<Signal<T>> info(ReactiveLogMessage message, Function<T, Object[]> mapper) {
        return info(message.getMessage(), mapper);
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
    public static <T> Consumer<Signal<T>> debug(ReactiveLogMessage message, Function<T, Object[]> mapper) {
        return debug(message.getMessage(), mapper);
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
    public static <T> Function<Throwable, Mono<T>> errorResume(ReactiveLogMessage message, Object[] args, T fallback) {
        return errorResume(message.getMessage(), args, fallback);
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