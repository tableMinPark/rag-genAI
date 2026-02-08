package com.genai.app.summary.controller.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryFileRequestDto {

    @NotBlank
    private String sessionId;

    @NotNull
    private float lengthRatio;
}
