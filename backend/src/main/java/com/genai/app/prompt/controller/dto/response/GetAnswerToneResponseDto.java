package com.genai.app.prompt.controller.dto.response;

import com.genai.core.service.module.vo.CommonCodeVO;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetAnswerToneResponseDto {

    private String code;

    private String name;

    public static GetAnswerToneResponseDto of(CommonCodeVO comnCode) {
        return GetAnswerToneResponseDto.builder()
                .code(comnCode.getCode())
                .name(comnCode.getCodeName())
                .build();
    }

    public static List<GetAnswerToneResponseDto> toList(List<CommonCodeVO> comnCodes) {
        return comnCodes.stream()
                .map(GetAnswerToneResponseDto::of)
                .toList();
    }
}
