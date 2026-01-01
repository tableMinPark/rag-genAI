package com.genai.core.repository.entity;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentEntity {

    @JsonAlias("DOCID")
    @JsonProperty("DOCID")
    private String chunkId;

    @JsonAlias("source_id")
    @JsonProperty("source_id")
    private String sourceId;

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

    @JsonAlias("category_code")
    @JsonProperty("category_code")
    private String categoryCode;

    @JsonAlias("source_type")
    @JsonProperty("source_type")
    private String sourceType;

    @JsonAlias("version")
    @JsonProperty("version")
    private Long version;

    @JsonAlias("token_size")
    @JsonProperty("token_size")
    private Integer tokenSize;

    @JsonAlias("file_detail_id")
    @JsonProperty("file_detail_id")
    private Long fileDetailId;

    @JsonAlias("origin_file_name")
    @JsonProperty("origin_file_name")
    private String originFileName;

    @JsonAlias("url")
    @JsonProperty("url")
    private String url;

    @JsonAlias("ext")
    @JsonProperty("ext")
    private String ext;

    @JsonAlias("sys_create_dt")
    @JsonProperty("sys_create_dt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sysCreateDt;

    @JsonAlias("sys_modify_dt")
    @JsonProperty("sys_modify_dt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sysModifyDt;

    @JsonAlias("alias")
    @JsonProperty("alias")
    private String alias;

    @JsonAlias("context")
    @JsonProperty("context")
    private String context;

    // 벡터 변환 필드
    @JsonAlias("context_VECTOR")
    @JsonProperty("context_VECTOR")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String contextVector;
}
