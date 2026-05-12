package com.genai.global.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum Menu {

    MENU_AI("ai", "RAG Chat", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_LLM("llm", "LLM Chat", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_SIMULATION("simulation", "시뮬레이션", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_MYAI("myai", "나만의 AI", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_REPORT("report", "보고서", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_SUMMARY("summary", "요약", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_TRANSLATE("translate", "번역", List.of("ROLE_USER", "ROLE_ADMIN")),

    MENU_CHAT_HISTORY("chatHistory", "대화이력", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_DOCUMENT("document", "문서관리", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_PROJECT("project", "나만의 AI 프로젝트", List.of("ROLE_USER", "ROLE_ADMIN")),
    MENU_PROMPT("prompt", "프롬프트", List.of("ROLE_USER", "ROLE_ADMIN")),
    ;

    public static List<String> getMenuIds(String role) {
        return Arrays.stream(values())
                .filter(menu -> menu.roles.contains(role))
                .map(Menu::getId)
                .toList();
    }

    private final String id;

    private final String name;

    private final List<String> roles;
}
