package com.genai.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class LawEntity {

    @JsonAlias("DOCID")
    private String docId;

    @JsonAlias("training_id")
    private String trainingId;

    @JsonAlias("original_id")
    private String originalId;

    @JsonAlias("title")
    private String title;

    @JsonAlias("sub_title")
    private String subTitle;

    @JsonAlias("third_title")
    private String thirdTitle;

    @JsonAlias("content")
    private String content;

    @JsonAlias("sub_content")
    private String subContent;

    @JsonAlias("context")
    private String context;

    @JsonAlias("category_code")
    private String categoryCode;

    @JsonAlias("doc_type")
    private String docType;

    @JsonAlias("version")
    private String version;

    @JsonAlias("token_size")
    private Integer tokenSize;

    @JsonAlias("created_at")
    private String createdAt;

    @JsonAlias("updated_at")
    private String updatedAt;

    @JsonAlias("alias")
    private String alias;
}
