package com.genai.core.service.module.impl;

import com.genai.common.utils.StringUtil;
import com.genai.common.vo.IndexedContentVO;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.module.SummaryModuleService;
import com.genai.core.service.module.constant.SummaryModuleConst;
import com.genai.core.service.module.vo.PartExportContextVO;
import com.genai.core.service.module.vo.PartExportState;
import com.genai.core.utils.ReactiveLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryModuleServiceImpl implements SummaryModuleService {

    private final ModelRepository modelRepository;

    /**
     * 핵심 부분 추출
     *
     * @param content 본문 분리 부분 문자열
     * @return 핵심 부분 추출 Mono
     */
    @Override
    public Mono<String> partExport(String content) {

        String query = "**Context**의 핵심 내용을 추출하라.";

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(SummaryModuleConst.PART_SUMMARY_PROMPT)
                .temperature(SummaryModuleConst.PART_SUMMARY_TEMPERATURE)
                .topP(SummaryModuleConst.PART_SUMMARY_TOP_P)
                .build();

        return modelRepository.generateAnswerAsync(query, content, null, null, promptEntity)
                .map(answerEntities -> {
                    StringBuilder answerBuilder = new StringBuilder();
                    answerEntities.forEach(answerEntity -> {
                        if (!answerEntity.getIsInference()) {
                            answerBuilder.append(answerEntity.getContent());
                        }
                    });
                    return answerBuilder.toString().trim();
                })
                .doOnEach(ReactiveLogUtil.debug(ReactiveLogUtil.Message.PART_EXPORT_MESSAGE, v -> new Object[]{
                        content, v
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 핵심 부분 추출 재귀
     *
     * @param state 핵심 부분 추출 상태
     * @return 핵심 부분 추출 Flux
     */
    @Override
    public Flux<PartExportContextVO> partExport(PartExportState state) {

        Mono<List<String>> contentsMono = Mono.just(state.getContents())
                .map(contents -> contents.stream().filter(content -> content != null && !content.trim().isBlank()).toList())
                .doOnEach(ReactiveLogUtil.info(ReactiveLogUtil.Message.PART_EXPORT_RECURSIVE_MESSAGE, v -> new Object[]{
                        state.getRound(), state.getContentLength(), v.size()
                }));

        if (state.getRound() == 0 && state.getContentLength() <= PartExportState.PART_INIT_TOKEN_SIZE) {
            return contentsMono
                    .flatMapMany(contents -> {
                        AtomicInteger index = new AtomicInteger(0);

                        return Flux.fromIterable(contents)
                                .map(content -> PartExportContextVO.of(
                                        index.getAndIncrement(),
                                        true,
                                        content,
                                        state.getStateId(),
                                        state.finishProgress()
                                ));

                    }).subscribeOn(Schedulers.boundedElastic());
        } else {
            return contentsMono
                    .flatMapMany(contents -> {
                        Flux<IndexedContentVO> processFlux = Flux.fromIterable(StringUtil.indexingContent(contents))
                                .flatMap(indexedContent -> partExport(indexedContent.getContent())
                                        .map(partExport ->
                                                new IndexedContentVO(indexedContent.getIndex(), indexedContent.getContent())), SummaryModuleConst.BATCH_SIZE)
                                .cache();

                        Flux<PartExportContextVO> progressFlux = processFlux
                                .map(indexedContent -> PartExportContextVO.of(
                                        indexedContent.getIndex(),
                                        false,
                                        indexedContent.getContent(),
                                        state.getStateId(),
                                        state.increaseProgress()
                                ));

                        Flux<PartExportContextVO> nextFlux = processFlux
                                .collectList()
                                .flatMapMany(indexedContents -> {
                                    PartExportState nextState = state.nextRound(indexedContents.stream()
                                            .map(IndexedContentVO::getContent)
                                            .toList());

                                    if (state.isFinished(nextState.getContentLength())) {
                                        return Flux.fromIterable(indexedContents)
                                                .map(indexedContent -> PartExportContextVO.of(
                                                        indexedContent.getIndex(),
                                                        true,
                                                        indexedContent.getContent(),
                                                        state.getStateId(),
                                                        state.finishProgress()
                                                ));
                                    }

                                    return partExport(nextState);
                                });

                        return progressFlux.concatWith(nextFlux);

                    }).subscribeOn(Schedulers.boundedElastic());
        }
    }

    /**
     * 핵심 부분 추출 요약
     *
     * @param contents 핵심 추출 문자열 목록
     * @return 요약 문자열 Mono
     */
    @Override
    public Mono<String> partExportSummary(List<String> contents) {

        String query = "**Context**의 내용을 종합적으로 요약하라.";

        // 전체 요약
        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(SummaryModuleConst.WHOLE_SUMMARY_PROMPT)
                .temperature(SummaryModuleConst.WHOLE_SUMMARY_TEMPERATURE)
                .topP(SummaryModuleConst.WHOLE_SUMMARY_TOP_P)
                .build();

        StringBuilder contentMergeBuilder = new StringBuilder();

        for (int index = 0; index < contents.size(); index++) {
            contentMergeBuilder
                    .append("# 핵심 추출 내용(").append(index).append(")\n")
                    .append(contents.get(index))
                    .append("\n\n---\n\n");
        }

        return Mono.just(contentMergeBuilder.toString().trim())
                .flatMap(contentMerge -> modelRepository.generateAnswerAsync(query, contentMerge, null, null, promptEntity))
                .map(answerEntities -> {

                    StringBuilder answerBuilder = new StringBuilder();

                    answerEntities.forEach(answerEntity -> {
                        if (!answerEntity.getIsInference()) {
                            answerBuilder.append(answerEntity.getContent());
                        }
                    });

                    return answerBuilder.toString().trim();

                })
                .doOnEach(ReactiveLogUtil.debug(ReactiveLogUtil.Message.PART_EXPORTS_SUMMARY_MESSAGE, v -> new Object[]{
                        contentMergeBuilder.toString().trim(), v
                }))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
