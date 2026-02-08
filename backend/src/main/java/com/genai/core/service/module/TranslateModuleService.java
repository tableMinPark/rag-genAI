package com.genai.core.service.module;

import reactor.core.publisher.Mono;

public interface TranslateModuleService {

    /**
     * 부분 번역
     *
     * @param beforeLang 원문 언어
     * @param afterLang  번역 언어
     * @param content    본문 분리 부분 문자열
     * @return 부분 번역 Mono
     */
    Mono<String> partTranslate(String beforeLang, String afterLang, String content);

    /**
     * 부분 번역
     *
     * @param beforeLang 원문 언어
     * @param afterLang  번역 언어
     * @param content    본문 분리 부분 문자열
     * @param dictionary 사전 문자열
     * @return 부분 번역 Mono
     */
    Mono<String> partTranslate(String beforeLang, String afterLang, String content, String dictionary);
}
