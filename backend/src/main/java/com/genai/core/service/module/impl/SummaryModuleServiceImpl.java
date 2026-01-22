package com.genai.core.service.module.impl;

import com.genai.core.service.module.SummaryModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryModuleServiceImpl implements SummaryModuleService {

    /**
     * 부분 요약
     *
     * @param contents 본문 분리 부분 문자열 목록
     * @return 부분 요약 Flux
     */
    @Override
    public Flux<String> partSummaryAsync(List<String> contents) {
        return null;
    }

    /**
     * 부분 요약문 전체 요약
     *
     * @param contents 부분 요약 문자열 목록
     * @return 전체 요약 Mono
     */
    @Override
    public Mono<String> summaryAsync(List<String> contents) {
        return null;
    }
}
