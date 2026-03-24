package com.genai.core.service.module;

import com.genai.core.repository.entity.PromptEntity;
import reactor.core.publisher.Mono;

public interface TranslateModuleService {

    /**
     * 부분 번역
     *
     * @param translateInfo  번역 정보
     * @param content    본문 분리 부분 문자열
     * @param dictionary 사전 문자열
     * @param promptEntity 번역 프롬프트
     * @return 부분 번역 Mono
     */
    Mono<String> partTranslate(String translateInfo, String content, String dictionary, PromptEntity promptEntity);
}
