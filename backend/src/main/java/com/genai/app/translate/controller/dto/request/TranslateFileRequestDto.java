package com.genai.app.translate.controller.dto.request;

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
public class TranslateFileRequestDto {

    @NotBlank
    private String sessionId;

    @NotBlank
    private String beforeLang;

    @NotBlank
    private String afterLang;

    @NotNull
    private boolean containDic;
}
