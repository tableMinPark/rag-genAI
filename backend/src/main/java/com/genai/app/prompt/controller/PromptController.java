package com.genai.app.prompt.controller;

import com.genai.app.prompt.controller.dto.response.GetAnswerStyleResponseDto;
import com.genai.app.prompt.controller.dto.response.GetAnswerToneResponseDto;
import com.genai.app.prompt.controller.dto.response.GetRoleResponseDto;
import com.genai.core.constant.CommonConst;
import com.genai.core.service.module.CommonCodeModuleService;
import com.genai.core.service.module.vo.CommonCodeVO;
import com.genai.global.dto.ResponseDto;
import com.genai.global.enums.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/prompt")
public class PromptController {

    private final CommonCodeModuleService commonCodeModuleService;

    /**
     * 역할 목록 조회
     */
    @GetMapping("/role")
    public ResponseEntity<ResponseDto<List<GetRoleResponseDto>>> getRoles() {

        List<CommonCodeVO> categoryCodes = commonCodeModuleService.getCommonCodes(CommonConst.PROMPT_ROLE_CODE_GROUP);

        return ResponseEntity.ok().body(Response.PROMPT_GET_ROLES_SUCCESS.toResponseDto(GetRoleResponseDto.toList(categoryCodes)));
    }

    /**
     * 답변 톤 목록 조회
     */
    @GetMapping("/tone")
    public ResponseEntity<ResponseDto<List<GetAnswerToneResponseDto>>> getAnswerTones() {

        List<CommonCodeVO> categoryCodes = commonCodeModuleService.getCommonCodes(CommonConst.PROMPT_TONE_CODE_GROUP);

        return ResponseEntity.ok().body(Response.PROMPT_GET_TONES_SUCCESS.toResponseDto(GetAnswerToneResponseDto.toList(categoryCodes)));
    }

    /**
     * 답변 스타일 목록 조회
     */
    @GetMapping("/style")
    public ResponseEntity<ResponseDto<List<GetAnswerStyleResponseDto>>> getAnswerStyles() {

        List<CommonCodeVO> categoryCodes = commonCodeModuleService.getCommonCodes(CommonConst.PROMPT_STYLE_CODE_GROUP);

        return ResponseEntity.ok().body(Response.PROMPT_GET_STYLES_SUCCESS.toResponseDto(GetAnswerStyleResponseDto.toList(categoryCodes)));
    }
}
