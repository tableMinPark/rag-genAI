package com.genai.app.report.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReportTextRequestDto {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String prompt;

    @NotBlank
    private String title;

    private String context;
}
