package com.genai.app.prompt.controller.dto.response;

import com.genai.core.service.module.vo.CommonCodeVO;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetAnswerStyleResponseDto {

    private String code;

    private String name;

    public static GetAnswerStyleResponseDto of(CommonCodeVO comnCode) {
        return GetAnswerStyleResponseDto.builder()
                .code(comnCode.getCode())
                .name(comnCode.getCodeName())
                .build();
    }

    public static List<GetAnswerStyleResponseDto> toList(List<CommonCodeVO> comnCodes) {
        return comnCodes.stream()
                .map(GetAnswerStyleResponseDto::of)
                .toList();
    }
}
