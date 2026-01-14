package com.genai.app.translate.controller.dto.response;

import com.genai.core.service.module.vo.CommonCodeVO;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetTranslateLanguageResponseDto {

    private String code;

    private String name;

    public static GetTranslateLanguageResponseDto of(CommonCodeVO comnCode) {
        return GetTranslateLanguageResponseDto.builder()
                .code(comnCode.getCode())
                .name(comnCode.getCodeName())
                .build();
    }

    public static List<GetTranslateLanguageResponseDto> toList(List<CommonCodeVO> comnCodes) {
        return comnCodes.stream()
                .map(GetTranslateLanguageResponseDto::of)
                .toList();
    }
}
