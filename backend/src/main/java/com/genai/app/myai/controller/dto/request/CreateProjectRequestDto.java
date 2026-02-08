package com.genai.app.myai.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequestDto {

    @NotBlank
    private String projectName;

    @NotBlank
    private String projectDesc;

    @NotBlank
    private String roleCode;

    @NotBlank
    private String toneCode;

    @NotBlank
    private String styleCode;
}
