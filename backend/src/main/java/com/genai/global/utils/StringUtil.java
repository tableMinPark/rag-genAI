package com.genai.global.utils;

import java.util.*;

public class StringUtil {

    // 원형 숫자 기호 (0 ~ 49)
    private static final char[] CIRCLE_NUMBERS = {
            0x24EA, 0x2460, 0x2461, 0x2462, 0x2463, 0x2464, 0x2465, 0x2466, 0x2467, 0x2468,
            0x2469, 0x246A, 0x246B, 0x246C, 0x246D, 0x246E, 0x246F, 0x2470, 0x2471, 0x2472,
            0x2473, 0x3251, 0x3252, 0x3253, 0x3254, 0x3255, 0x3256, 0x3257, 0x3258, 0x3259,
            0x325A, 0x325B, 0x325C, 0x325D, 0x325E, 0x325F, 0x32B1, 0x32B2, 0x32B3, 0x32B4,
            0x32B5, 0x32B6, 0x32B7, 0x32B8, 0x32B9, 0x32BA, 0x32BB, 0x32BC, 0x32BD, 0x32BE
    };

    /**
     * 파일 확장자 제거
     *
     * @param originFilename 파일명
     * @return 확장자 제거 이름
     */
    public static String removeExtension(String originFilename) {
        if (originFilename == null || originFilename.isBlank()) {
            return originFilename;
        }

        int lastDotIndex = originFilename.lastIndexOf('.');

        // 점(.)이 없거나 맨 앞에만 있는 경우 (예: ".gitignore")
        if (lastDotIndex <= 0) {
            return originFilename;
        }

        return originFilename.substring(0, lastDotIndex);
    }

    /**
     * 문자열 숫자 여부 확인
     *
     * @param str 문자열
     * @return 숫자 여부
     */
    public static boolean isNumber(String str) {
        if (str != null && !str.isBlank()) {
            return !str.chars().allMatch(Character::isDigit);
        }
        return true;
    }

    /**
     * 공백/개행 정리
     *
     * @param str 원본 문자열
     * @return 공백 정리 문자열
     */
    public static String normalize(String str) {
        str = str.replaceAll("[ \\t\\f\\r]+", " ");   // 연속 공백 → 하나
        str = str.replaceAll(" *\\n+ *", "\n");       // 개행 여러 개 → 하나
        return str.trim();
    }

    /**
     * 원형 숫자 기호 대치
     *
     * @param c 문자
     * @return 대치 문자열
     */
    public static String getCircleNumber(char c) {
        for (int num = 0; num < CIRCLE_NUMBERS.length; num++) {
            if (c == CIRCLE_NUMBERS[num]) {
                return String.valueOf(num);
            }
        }

        return String.valueOf(c);
    }

    /**
     * 두 문장의 코사인 유사도 반환 (Bag-of-Words, TF 기반)
     */
    public static double cosineSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null || s1.isEmpty() || s2.isEmpty()) return 0.0;

        // 단어 단위 분리 (원하면 tokenizer 교체 가능)
        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");

        // 단어 빈도 저장
        Map<String, Integer> freq1 = new HashMap<>();
        Map<String, Integer> freq2 = new HashMap<>();

        for (String w : words1) {
            freq1.put(w, freq1.getOrDefault(w, 0) + 1);
        }
        for (String w : words2) {
            freq2.put(w, freq2.getOrDefault(w, 0) + 1);
        }

        // 전체 단어 집합 생성
        Set<String> allWords = new HashSet<>();
        allWords.addAll(freq1.keySet());
        allWords.addAll(freq2.keySet());

        // 벡터 생성
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String word : allWords) {
            int a = freq1.getOrDefault(word, 0);
            int b = freq2.getOrDefault(word, 0);

            dot += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0 || normB == 0) return 0.0;

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}