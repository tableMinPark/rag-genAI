package com.genai.app.myai.service.vo;

import com.genai.core.service.business.vo.FileDetailVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
public class CreateProjectVO {

    private final ProjectVO project;

    private final Long fileId;

    private final List<FileDetailVO> addFileDetails;
}
