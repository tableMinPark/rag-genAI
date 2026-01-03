package com.genai.app.myai.controller.dto.response;

import com.genai.app.myai.service.vo.ProjectVO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetProjectResponseDto {

    private Long projectId;

    private String projectName;

    private String projectDesc;

    private LocalDateTime sysCreateDt;

    private LocalDateTime sysModifyDt;

    private Integer sourceCount;

    private Integer chunkCount;

    public static GetProjectResponseDto of(ProjectVO projectVo) {
        return GetProjectResponseDto.builder()
                .projectId(projectVo.getProjectId())
                .projectName(projectVo.getProjectName())
                .projectDesc(projectVo.getProjectDesc())
                .sysCreateDt(projectVo.getSysCreateDt())
                .sysModifyDt(projectVo.getSysModifyDt())
                .sourceCount(projectVo.getSourceCount())
                .chunkCount(projectVo.getChunkCount())
                .build();
    }

    public static List<GetProjectResponseDto> toList(List<ProjectVO> projectVos) {
        return projectVos.stream().map(GetProjectResponseDto::of).toList();
    }
}
