package com.genai.core.repository.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ToString
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentEntity {

    @JsonAlias("chunk_id")
    @JsonProperty("chunk_id")
    private Long chunkId;

    @JsonAlias("passage_id")
    @JsonProperty("passage_id")
    private Long passageId;

    @JsonAlias("source_id")
    @JsonProperty("source_id")
    private Long sourceId;

    @JsonAlias("file_detail_id")
    @JsonProperty("file_detail_id")
    private Long fileDetailId;

    @JsonAlias("origin_file_name")
    @JsonProperty("origin_file_name")
    private String originFileName;

    @JsonAlias("name")
    @JsonProperty("name")
    private String name;

    @JsonAlias("title")
    @JsonProperty("title")
    private String title;

    @JsonAlias("sub_title")
    @JsonProperty("sub_title")
    private String subTitle;

    @JsonAlias("third_title")
    @JsonProperty("third_title")
    private String thirdTitle;

    @JsonAlias("compact_content")
    @JsonProperty("compact_content")
    private String compactContent;

    @JsonAlias("content")
    @JsonProperty("content")
    private String content;

    @JsonAlias("sub_content")
    @JsonProperty("sub_content")
    private String subContent;

    @JsonAlias("context")
    @JsonProperty("context")
    private String context;

    // 벡터 변환 필드
    @Builder.Default
    @JsonAlias("vector-context")
    @JsonProperty("vector-context")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<Float> contextVector = new ArrayList<>();

    @JsonAlias("url")
    @JsonProperty("url")
    private String url;

    @JsonAlias("category_code")
    @JsonProperty("category_code")
    private String categoryCode;

    @JsonAlias("source_type")
    @JsonProperty("source_type")
    private String sourceType;

    @JsonAlias("ext")
    @JsonProperty("ext")
    private String ext;

    @JsonAlias("sys_create_dt")
    @JsonProperty("sys_create_dt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sysCreateDt;

    @JsonAlias("sys_modify_dt")
    @JsonProperty("sys_modify_dt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sysModifyDt;

    @JsonAlias("alias")
    @JsonProperty("alias")
    private String alias;
}
