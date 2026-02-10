package com.genai.core.utils;

import java.util.List;

public class DecisionDetectUtil {

    private static final List<String> DECISION_KEYWORDS = List.of(
            // 확정/결론
            "결정", "확정", "결론", "정리하면", "요약하면",

            // 선택
            "이걸로", "이 방식으로", "이 구조로",
            "채택", "선택",

            // 실행/적용
            "사용한다", "적용한다", "유지한다", "변경한다",
            "분리한다", "통합한다",

            // 정책/규칙
            "앞으로", "이후에는", "항상", "기본적으로",
            "~하기로 한다"
    );

    private static final List<String> NEGATIVE_KEYWORDS = List.of(
            // 미확정 표현 (결정 아님)
            "고민", "검토", "가능", "아직", "생각중",
            "어떨까", "일단", "임시", "추후"
    );

    public static boolean detect(String query, String answer) {
        return detectFromText(query) || detectFromText(answer);
    }

    private static boolean detectFromText(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        // 미확정 키워드가 강하게 포함되면 decision 아님
        boolean hasNegative = NEGATIVE_KEYWORDS.stream()
                .anyMatch(text::contains);

        if (hasNegative) {
            return false;
        }

        return DECISION_KEYWORDS.stream().anyMatch(text::contains);
    }
}
