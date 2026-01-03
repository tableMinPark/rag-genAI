package com.genai.app.myai.controller.dto.response;

import com.genai.core.service.business.vo.FileDetailVO;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetProjectSourceResponseDto {

    private Long fileDetailId;

    private String fileOriginName;

    private String ext;

    private Integer fileSize;

    public static GetProjectSourceResponseDto of(FileDetailVO fileDetailVo) {
        return GetProjectSourceResponseDto.builder()
                .fileDetailId(fileDetailVo.getFileDetailId())
                .fileOriginName(fileDetailVo.getFileOriginName())
                .ext(fileDetailVo.getExt())
                .fileSize(fileDetailVo.getFileSize())
                .build();
    }

    public static List<GetProjectSourceResponseDto> toList(List<FileDetailVO> fileDetailVos) {
        return fileDetailVos.stream().map(GetProjectSourceResponseDto::of).toList();
    }
}
