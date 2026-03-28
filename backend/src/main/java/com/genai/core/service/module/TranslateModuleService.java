package com.genai.core.service.module;

import com.genai.common.utils.AhoCorasick;
import com.genai.core.service.module.vo.PartTranslateContextVO;
import com.genai.core.service.module.vo.PartTranslateState;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TranslateModuleService {

    /**
     * 부분 번역
     *
     * @param afterLangName 번역 언어
     * @param content       본문 분리 부분 문자열
     * @param dictionary    사전 문자열
     * @return 부분 번역 Mono
     */
    Mono<String> partTranslate(String afterLangName, String content, String dictionary);

    /**
     * 부분 번역 재귀
     *
     * @param state       부분 번역 상태
     * @param ahoCorasick 사전 색인
     * @return 부분 번역 Flux
     */
    Flux<PartTranslateContextVO> partTranslate(PartTranslateState state, AhoCorasick ahoCorasick);
}
