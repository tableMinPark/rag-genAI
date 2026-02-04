package com.genai.core.service.module.impl;

import com.genai.core.constant.PromptConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.service.module.TranslateModuleService;
import com.genai.global.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateModuleServiceImpl implements TranslateModuleService {

    private final ModelRepository modelRepository;
    private final PromptRepository promptRepository;

    /**
     * 부분 번역
     *
     * @param beforeLang 원문 언어
     * @param afterLang  번역 언어
     * @param content    본문 분리 부분 문자열
     * @return 부분 번역 Mono
     */
    @Override
    public Mono<String> partTranslate(String beforeLang, String afterLang, String content) {
        return this.partTranslate(beforeLang, afterLang, content, "");
    }

    /**
     * 부분 번역
     *
     * @param beforeLang 원문 언어
     * @param afterLang  번역 언어
     * @param content    본문 분리 부분 문자열
     * @param dictionary 사전 문자열
     * @return 부분 번역 Mono
     */
    @Override
    public Mono<String> partTranslate(String beforeLang, String afterLang, String content, String dictionary) {
        return Mono.fromCallable(() -> promptRepository.findById(PromptConst.TRANSLATE_PROMPT_ID)
                        .orElseThrow(() -> new NotFoundException("프롬프트")))
                .map(promptEntity -> {
                    String query = String.format("Translate %s to %s\n\n%s", beforeLang, afterLang, dictionary).trim();
                    log.info("부분 번역 | {}", content.replace("\n", "\\n"));
                    String partTranslate = modelRepository.generateAnswerSyncStr(query, content, CommonUtil.generateRandomId(), promptEntity);
                    return partTranslate + "\n";
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
