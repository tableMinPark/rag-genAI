package com.genai.global.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    // 정규식 패턴 정의
    private static final Pattern LINK_PATTERN  = Pattern.compile("\\[([^]]+)]\\([^)]*\\)");
    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^]]+)]\\([^)]*\\)");

    public static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 랜덤 ID 값 생성
     *
     * @return 랜덤 ID
     */
    public static String generateRandomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 마크다운 링크 및 이미지 링크 제거 후 텍스트 반환
     *
     * @param input 입력 문자열
     * @return 치환된 텍스트
     */
    public static String cleanMarkdownLinks(String input) {
        if (input == null || input.isEmpty()) return input;

        String result = input;

        Matcher imgMatcher = IMAGE_PATTERN.matcher(result);
        StringBuilder imgBuffer = new StringBuilder();
        while (imgMatcher.find()) {
            imgMatcher.appendReplacement(imgBuffer, imgMatcher.group(1));
        }
        imgMatcher.appendTail(imgBuffer);
        result = imgBuffer.toString();

        Matcher linkMatcher = LINK_PATTERN.matcher(result);
        StringBuilder linkBuffer = new StringBuilder();
        while (linkMatcher.find()) {
            linkMatcher.appendReplacement(linkBuffer, linkMatcher.group(1));
        }
        linkMatcher.appendTail(linkBuffer);

        return linkBuffer.toString();
    }

    public static String writeJson(Object object) {

        String json = "{}";

        try {
            json = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException ignored) {
        }

        return json;
    }
}
