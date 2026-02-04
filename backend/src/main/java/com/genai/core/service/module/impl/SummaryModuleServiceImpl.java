package com.genai.core.service.module.impl;

import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.module.SummaryModuleService;
import com.genai.core.service.module.constant.SummaryModuleConst;
import com.genai.global.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryModuleServiceImpl implements SummaryModuleService {

    private final ModelRepository modelRepository;

    /**
     * 부분 요약
     *
     * @param content 본문 분리 부분 문자열
     * @return 부분 요약 Flux
     */
    @Override
    public Mono<String> partSummary(String content) {

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(SummaryModuleConst.PART_SUMMARY_PROMPT)
                .temperature(SummaryModuleConst.PART_SUMMARY_TEMPERATURE)
                .topP(SummaryModuleConst.PART_SUMMARY_TOP_P)
                .build();

        return Mono.fromCallable(() -> {
            log.info("부분 요약 | {}", content.replace("\n", "\\n"));
            return modelRepository.generateAnswerSyncStr("", content, CommonUtil.generateRandomId(), promptEntity);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 부분 요약
     *
     * @param contents 본문 분리 부분 문자열 목록
     * @return 부분 요약 Flux
     */
    @Override
    public Flux<String> partSummaries(List<String> contents, int batchSize) {

        return Flux.fromIterable(contents)
                .buffer(batchSize)
                .concatMap(batch -> Flux.fromIterable(batch)
                        .flatMapSequential(this::partSummary, batchSize));
    }

    /**
     * 부분 요약문 전체 요약
     *
     * @param contents 부분 요약 문자열 목록
     * @return 전체 요약 Mono
     */
    @Override
    public Mono<String> wholeSummaries(List<String> contents) {

        // 전체 요약
        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(SummaryModuleConst.WHOLE_SUMMARY_PROMPT)
                .temperature(SummaryModuleConst.WHOLE_SUMMARY_TEMPERATURE)
                .topP(SummaryModuleConst.WHOLE_SUMMARY_TOP_P)
                .build();

        return Flux.fromIterable(contents)
                .collectList()
                .flatMap(targetContents -> Mono.fromCallable(() -> {
                    String context = String.join("\n\n---\n\n", targetContents);
                    log.info("전체 요약 | {}", context.replace("\n", "\\n"));
                    return modelRepository.generateAnswerSyncStr("", context, CommonUtil.generateRandomId(), promptEntity);
                })).subscribeOn(Schedulers.boundedElastic());
    }
}
