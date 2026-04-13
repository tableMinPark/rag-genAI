package com.genai.app.myai.service.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class DeleteProjectVO {

    private final ProjectVO project;

    private final Long fileId;
}
