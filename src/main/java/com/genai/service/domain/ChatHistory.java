package com.genai.service.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistory {

    private List<Context> contexts;

    private String promptCode;

    private String query;

    private String inference;

    private String answer;

    private String sessionId;
}
