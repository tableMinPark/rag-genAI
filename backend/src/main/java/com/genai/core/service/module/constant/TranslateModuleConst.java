package com.genai.core.service.module.constant;

public class TranslateModuleConst {

    public static final int BATCH_SIZE  = 10;

    /*
     * #######################################
     * 부분 번역 프롬프트
     * #######################################
     */
    private static final String TARGET_LANGUAGE_PREFIX = "{{ language }}";
    private static final String TARGET_LANGUAGE_PROMPT = """
    # 번역 대상 언어
    - 사용자가 선택한 언어: {{ language }}
    """;
    public static String TARGET_LANGUAGE_PROMPT(String language) {
        return TARGET_LANGUAGE_PROMPT.replace(TARGET_LANGUAGE_PREFIX, language);
    }
}
