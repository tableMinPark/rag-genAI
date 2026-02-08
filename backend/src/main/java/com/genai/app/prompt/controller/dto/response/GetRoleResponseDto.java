package com.genai.app.prompt.controller.dto.response;

import com.genai.app.chat.controller.dto.response.GetCategoriesResponseDto;
import com.genai.core.service.module.vo.CommonCodeVO;
import lombok.*;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetRoleResponseDto {

    private String code;

    private String name;

    public static GetRoleResponseDto of(CommonCodeVO comnCode) {
        return GetRoleResponseDto.builder()
                .code(comnCode.getCode())
                .name(comnCode.getCodeName())
                .build();
    }

    public static List<GetRoleResponseDto> toList(List<CommonCodeVO> comnCodes) {
        return comnCodes.stream()
                .map(GetRoleResponseDto::of)
                .toList();
    }
}
