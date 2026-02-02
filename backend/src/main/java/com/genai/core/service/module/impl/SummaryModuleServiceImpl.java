package com.genai.core.service.module.impl;

import com.genai.core.constant.ReportConst;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.module.SummaryModuleService;
import com.genai.global.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryModuleServiceImpl implements SummaryModuleService {

    private final ModelRepository modelRepository;

    /**
     * 부분 요약
     *
     * @param contents 본문 분리 부분 문자열 목록
     * @return 부분 요약 Flux
     */
    @Override
    public Flux<String> partSummary(List<String> contents) {

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(ReportConst.REPORT_PART_SUMMARIES_PROMPT)
                .temperature(ReportConst.REPORT_PART_SUMMARIES_TEMPERATURE)
                .topP(ReportConst.REPORT_PART_SUMMARIES_TOP_P)
                .build();

        return Flux.fromIterable(contents)
                .buffer(3)
                .concatMap(targetContents -> Flux.fromIterable(targetContents)
                        .flatMapSequential(targetContent -> {

                            log.info("부분 요약 | {}", targetContent);

                            return Flux.just(modelRepository
                                    .generateAnswerSyncStr("", targetContent, CommonUtil.generateRandomId(), promptEntity));
                        }, 3));
    }

    /**
     * 부분 요약문 전체 요약
     *
     * @param contents 부분 요약 문자열 목록
     * @return 전체 요약 Mono
     */
    @Override
    public Mono<String> wholeSummary(List<String> contents) {

        // 전체 요약
        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(ReportConst.REPORT_SUMMARIES_PROMPT)
                .temperature(ReportConst.REPORT_SUMMARIES_TEMPERATURE)
                .topP(ReportConst.REPORT_SUMMARIES_TOP_P)
                .build();

        return Flux.fromIterable(contents)
                .collectList()
                .flatMap(targetContents -> Mono.fromCallable(() -> {
                    String context = String.join("\n\n---\n\n", targetContents);
                    return modelRepository.generateAnswerSyncStr("", context, CommonUtil.generateRandomId(), promptEntity);
                }));
    }
}
