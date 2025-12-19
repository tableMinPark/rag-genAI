package com.genai.chat.controller.dto.response;

import com.genai.core.service.vo.ComnCodeVO;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetCategoriesResponseDto {

    private String code;

    private String name;

    public static GetCategoriesResponseDto of(ComnCodeVO comnCode) {
        return GetCategoriesResponseDto.builder()
                .code(comnCode.getCode())
                .name(comnCode.getCodeName())
                .build();
    }

    public static List<GetCategoriesResponseDto> toList(List<ComnCodeVO> comnCodes) {
        return comnCodes.stream()
                .map(GetCategoriesResponseDto::of)
                .toList();
    }
}
