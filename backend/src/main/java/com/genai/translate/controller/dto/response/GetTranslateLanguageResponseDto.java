package com.genai.translate.controller.dto.response;

import com.genai.core.service.vo.ComnCodeVO;
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

    public static GetTranslateLanguageResponseDto of(ComnCodeVO comnCode) {
        return GetTranslateLanguageResponseDto.builder()
                .code(comnCode.getCode())
                .name(comnCode.getCodeName())
                .build();
    }

    public static List<GetTranslateLanguageResponseDto> toList(List<ComnCodeVO> comnCodes) {
        return comnCodes.stream()
                .map(GetTranslateLanguageResponseDto::of)
                .toList();
    }
}
