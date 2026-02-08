package com.genai.core.service.module;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SummaryModuleService {

    /**
     * 부분 요약
     *
     * @param content 본문 분리 부분 문자열
     * @return 부분 요약 Mono
     */
    Mono<String> partSummary(String content);

    /**
     * 부분 요약
     *
     * @param contents 본문 분리 부분 문자열 목록
     * @return 부분 요약 Flux
     */
    Flux<String> partSummaries(List<String> contents, int batchSize);

    /**
     * 부분 요약문 전체 요약
     *
     * @param contents 부분 요약 문자열 목록
     * @return 전체 요약 Mono
     */
    Mono<String> wholeSummaries(List<String> contents);
}
