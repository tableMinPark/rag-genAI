package com.genai.application.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.genai.constant.SearchConst;
import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @JsonAlias("DOCID")
    private String docId;

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

    @JsonAlias("file_path")
    private String filePath;

    @JsonAlias("url")
    private String url;

    @JsonAlias("doc_type")
    private String docType;

    @JsonAlias("category_code")
    private String categoryCode;

    @JsonAlias(SearchConst.VECTOR_FIELD)
    private String context;

    @JsonAlias("alias")
    private String alias;
}
