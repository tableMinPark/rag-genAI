package com.genai.core.service.module.impl;

import com.genai.core.common.enums.CoreLogMessage;
import com.genai.global.common.utils.AhoCorasick;
import com.genai.global.common.vo.IndexedContentVO;
import com.genai.core.common.constant.PromptConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.module.TranslateModuleService;
import com.genai.core.service.module.constant.TranslateModuleConst;
import com.genai.core.service.module.vo.PartTranslateContextVO;
import com.genai.core.service.module.vo.PartTranslateState;
import com.genai.global.common.utils.ReactiveLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateModuleServiceImpl implements TranslateModuleService {

    private final ModelRepository modelRepository;
    private final PromptRepository promptRepository;

    /**
     * 부분 번역
     *
     * @param afterLangName 번역 언어
     * @param content       본문 분리 부분 문자열
     * @param dictionary    사전 문자열
     * @return 부분 번역 Mono
     */
    @Override
    public Mono<String> partTranslate(String afterLangName, String content, String dictionary) {

        String query = "**Context**를 **" + afterLangName + "** 로 번역하라.";

        Mono<PromptEntity> promptEntityMono = Mono.fromCallable(() -> promptRepository.findById(PromptConst.TRANSLATE_PROMPT_ID)
                        .orElseThrow(() -> new NotFoundException("프롬프트"))
                        .copyAndConcatPromptContent(
                                TranslateModuleConst.TARGET_LANGUAGE_PROMPT(afterLangName),
                                dictionary
                        )
                )
                .subscribeOn(Schedulers.boundedElastic());

        return promptEntityMono
                .flatMap(promptEntity -> modelRepository.generateAnswerAsync(query, content, null, null, promptEntity)
                        .map(answerEntities -> {

                            StringBuilder answerBuilder = new StringBuilder();

                            answerEntities.forEach(answerEntity -> {
                                if (!answerEntity.getIsInference()) {
                                    answerBuilder.append(answerEntity.getContent());
                                }
                            });

                            return answerBuilder.toString().trim();

                        })
                        .doOnEach(ReactiveLogUtil.debug(CoreLogMessage.PART_TRANSLATE_MESSAGE, v -> new Object[]{
                                content, v
                        }))
                ).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * 부분 번역 재귀
     *
     * @param state       부분 번역 상태
     * @param ahoCorasick 사전 색인
     * @return 부분 번역 Flux
     */
    @Override
    public Flux<PartTranslateContextVO> partTranslate(PartTranslateState state, AhoCorasick ahoCorasick) {

        Mono<List<IndexedContentVO>> contentsMono = Mono.just(state.getIndexedContents())
                .map(indexedContents -> indexedContents.stream()
                        .filter(indexedContent -> indexedContent != null && indexedContent.getContent() != null && !indexedContent.getContent().trim().isBlank()).toList())
                .doOnEach(ReactiveLogUtil.info(CoreLogMessage.PART_TRANSLATE_RECURSIVE_MESSAGE, v -> new Object[]{
                        v.size()
                }));

        return contentsMono
                .flatMapMany(indexedContents -> Flux.fromIterable(indexedContents)
                        .flatMap(indexedContent -> {
                            int index = indexedContent.getIndex();
                            String content = indexedContent.getContent();
                            String dictionaryContent = "";

                            // 사전 목록 조회
                            if (state.isContainDic()) {
                                StringBuilder dictionaryContentBuilder = new StringBuilder();
                                Set<String> matchedWords = ahoCorasick.search(content.toLowerCase());
                                state.getDictionaries().stream()
                                        .filter(dictionaryEntity -> matchedWords.contains(dictionaryEntity.getDictionary().toLowerCase()))
                                        .forEach(dictionaryEntity -> {
                                            dictionaryContentBuilder
                                                    .append("|")
                                                    .append(dictionaryEntity.getDictionary())
                                                    .append("|")
                                                    .append(dictionaryEntity.getDictionaryDesc())
                                                    .append("|\n");
                                        });

                                if (!dictionaryContentBuilder.isEmpty()) {
                                    dictionaryContent = """
                                            ## Reference Dictionary
                                            |word|replace_word|
                                            |---|---|
                                            """ + dictionaryContentBuilder.toString().trim();
                                }
                            }

                            return partTranslate(state.getAfterLangName(), content, dictionaryContent)
                                    .map(partTranslate -> PartTranslateContextVO.of(
                                            index,
                                            partTranslate,
                                            state.getStateId(),
                                            state.increaseProgress()
                                    ));

                        }, TranslateModuleConst.BATCH_SIZE))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
