package com.genai.app.myai.service.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class ProjectVO {

    private final Long projectId;

    private final String projectName;

    private final String projectDesc;

    private final LocalDateTime sysCreateDt;

    private final LocalDateTime sysModifyDt;

    private final Long fileId;

    private final Long promptId;

    private final Integer sourceCount;

    private final Integer chunkCount;
}
