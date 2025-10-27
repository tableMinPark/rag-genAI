package com.genai.service.domain;

import com.genai.global.enums.MenuType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RagChatHistory extends ChatHistory {

    private MenuType menuType;

    private List<Search<Document>> keywordSearchDocuments;

    private List<Search<Document>> vectorSearchDocuments;

    private List<Rerank> reranks;

    private List<Rerank> topReranks;
}
