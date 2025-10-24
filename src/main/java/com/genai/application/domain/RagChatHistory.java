package com.genai.application.domain;

import com.genai.application.enums.CollectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RagChatHistory extends ChatHistory {

    private CollectionType collectionType;

    private List<Search<Document>> keywordSearchDocuments;

    private List<Search<Document>> vectorSearchDocuments;

    private List<Rerank> reranks;

    private List<Rerank> topReranks;
}
