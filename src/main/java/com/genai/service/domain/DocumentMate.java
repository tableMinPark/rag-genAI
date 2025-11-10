package com.genai.service.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@ToString(callSuper = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMate extends Document {

    @JsonAlias("training_id")
    private String trainingId;

    @JsonAlias("original_id")
    private String originalId;

    @JsonAlias("name")
    private String name;

    @JsonAlias("version")
    private String version;

    @JsonAlias("token_size")
    private Integer tokenSize;

    @JsonAlias("sys_create_dt")
    private String sysCreateDt;

    @JsonAlias("sys_modify_dt")
    private String sysModifyDt;
}
