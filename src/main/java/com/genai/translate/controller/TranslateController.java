package com.genai.translate.controller;

import com.genai.core.config.constant.ComnConst;
import com.genai.core.controller.dto.response.ResponseDto;
import com.genai.core.service.ComnCodeCoreService;
import com.genai.core.service.TranslateCoreService;
import com.genai.core.service.vo.ComnCodeVO;
import com.genai.core.service.vo.TranslateVO;
import com.genai.global.enums.Response;
import com.genai.translate.controller.dto.request.TranslateFileRequestDto;
import com.genai.translate.controller.dto.request.TranslateTextRequestDto;
import com.genai.translate.controller.dto.response.GetTranslateLanguageResponseDto;
import com.genai.translate.controller.dto.response.TranslateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/translate")
public class TranslateController {

    private final TranslateCoreService translateCoreService;
    private final ComnCodeCoreService comnCodeCoreService;

    /**
     * 번역 요청
     *
     * @param translateTextRequestDto 번역 요청 정보
     */
    @PostMapping(value = "/text")
    public ResponseEntity<ResponseDto<TranslateResponseDto>> translateText(@Valid @RequestBody TranslateTextRequestDto translateTextRequestDto) {

        String sessionId = translateTextRequestDto.getSessionId();
        String beforeLang = translateTextRequestDto.getBeforeLang();
        String afterLang = translateTextRequestDto.getAfterLang();
        boolean containDic = translateTextRequestDto.isContainDic();
        String context = translateTextRequestDto.getContext();

        long chatId = 7L;
        TranslateVO translateVO = translateCoreService.translate(beforeLang, afterLang, context, sessionId, chatId, containDic);

        return ResponseEntity.ok().body(Response.TRANSLATE_GENERATE_TEXT_SUCCESS.toResponseDto(TranslateResponseDto.builder()
                .sessionId(sessionId)
                .msgId(translateVO.getMsgId())
                .content(translateVO.getContent())
                .build()));
    }

    /**
     * 번역 요청
     *
     * @param translateFileRequestDto 번역 요청 정보
     * @param multipartFile           번역 문서 파일
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<TranslateResponseDto>> translateFile(
            @Valid
            @RequestPart("requestDto") TranslateFileRequestDto translateFileRequestDto,
            @RequestPart("uploadFile") MultipartFile multipartFile
    ) {

        String sessionId = translateFileRequestDto.getSessionId();
        String beforeLang = translateFileRequestDto.getBeforeLang();
        String afterLang = translateFileRequestDto.getAfterLang();
        boolean containDic = translateFileRequestDto.isContainDic();

        long chatId = 7L;
        TranslateVO translateVO = translateCoreService.translate(beforeLang, afterLang, multipartFile, sessionId, chatId, containDic);

        return ResponseEntity.ok().body(Response.TRANSLATE_GENERATE_FILE_SUCCESS.toResponseDto(TranslateResponseDto.builder()
                .sessionId(sessionId)
                .msgId(translateVO.getMsgId())
                .content(translateVO.getContent())
                .build()));
    }

    /**
     * 번역 언어 목록 조회
     */
    @GetMapping("/language")
    public ResponseEntity<ResponseDto<List<GetTranslateLanguageResponseDto>>> getTranslateLanguages() {

        List<ComnCodeVO> translateLanguageComnCodes = comnCodeCoreService.getComnCodes(ComnConst.TRANSLATE_LANGUAGE_CODE_GROUP);

        return ResponseEntity.ok().body(Response.TRANSLATE_TRANSLATE_LANGUAGES.toResponseDto(GetTranslateLanguageResponseDto
                .toList(translateLanguageComnCodes)));
    }
}
