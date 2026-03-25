package com.genai.core.service.module.impl;

import com.genai.core.constant.PromptConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.module.TranslateModuleService;
import com.genai.core.utils.ReactiveLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateModuleServiceImpl implements TranslateModuleService {

    private final ModelRepository modelRepository;

    /**
     * 부분 번역
     *
     * @param translateInfo  번역 정보
     * @param content    본문 분리 부분 문자열
     * @param dictionary 사전 문자열
     * @param promptEntity 번역 프롬프트
     * @return 부분 번역 Mono
     */
    @Override
    public Mono<String> partTranslate(String translateInfo, String content, String dictionary, PromptEntity promptEntity) {

        PromptEntity translatePromptEntity = promptEntity.copy()
                .concatPromptContent(translateInfo)
                .concatPromptContent(dictionary);

        return modelRepository.generateAnswerAsync(null, content, null, null, translatePromptEntity)
                .map(answerEntities -> {
                    StringBuilder answerBuilder = new StringBuilder();
                    answerEntities.forEach(answerEntity -> {
                        if (!answerEntity.getIsInference()) {
                            answerBuilder.append(answerEntity.getContent());
                        }
                    });
                    return answerBuilder.toString().trim();
                })
                .doOnEach(ReactiveLogUtil.debug(ReactiveLogUtil.PART_TRANSLATE_MESSAGE, v -> new Object[]{
                        content.replace("\n", "\\n"), v.replace("\n", "\\n")
                }));
    }
}
