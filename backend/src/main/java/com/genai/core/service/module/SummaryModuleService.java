package com.genai.core.service.module;

import com.genai.core.service.module.vo.PartExportContextVO;
import com.genai.core.service.module.vo.PartExportState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SummaryModuleService {

    /**
     * 핵심 부분 추출
     *
     * @param content 본문 분리 부분 문자열
     * @return 핵심 부분 추출 Mono
     */
    Mono<String> partExport(String content);

    /**
     * 핵심 부분 추출 재귀
     *
     * @param state 핵심 부분 추출 상태
     * @return 핵심 부분 추출 Flux
     */
    Flux<PartExportContextVO> partExport(PartExportState state);

    /**
     * 부분 요약문 전체 요약
     *
     * @param contents 부분 요약 문자열 목록
     * @return 전체 요약 Mono
     */
    Mono<String> partExportSummary(List<String> contents);
}
