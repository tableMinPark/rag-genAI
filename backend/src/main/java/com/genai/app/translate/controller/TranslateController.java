package com.genai.app.translate.controller;

import com.genai.app.translate.controller.dto.request.TranslateFileRequestDto;
import com.genai.app.translate.controller.dto.request.TranslateTextRequestDto;
import com.genai.app.translate.controller.dto.response.GetTranslateLanguageResponseDto;
import com.genai.app.translate.controller.dto.response.TranslateResponseDto;
import com.genai.core.constant.CommonConst;
import com.genai.core.service.business.StreamCoreService;
import com.genai.core.service.business.TranslateCoreService;
import com.genai.core.service.business.subscriber.StreamSubscriber;
import com.genai.core.service.business.vo.TranslateVO;
import com.genai.core.service.module.CommonCodeModuleService;
import com.genai.core.service.module.vo.CommonCodeVO;
import com.genai.global.dto.ResponseDto;
import com.genai.global.enums.Response;
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
    private final CommonCodeModuleService commonCodeModuleService;
    private final StreamCoreService streamCoreService;

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

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 4L;
        TranslateVO translateVO = translateCoreService.translate(beforeLang, afterLang, context, sessionId, chatId, containDic);

        translateVO.getAnswerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.TRANSLATE_GENERATE_TEXT_SUCCESS.toResponseDto(TranslateResponseDto.builder()
                .sessionId(sessionId)
                .msgId(translateVO.getMsgId())
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

        StreamSubscriber streamSubscriber = streamCoreService.getStream(sessionId);

        long chatId = 4L;
        TranslateVO translateVO = translateCoreService.translate(beforeLang, afterLang, multipartFile, sessionId, chatId, containDic);

        translateVO.getAnswerStream().subscribe(streamSubscriber);

        return ResponseEntity.ok().body(Response.TRANSLATE_GENERATE_FILE_SUCCESS.toResponseDto(TranslateResponseDto.builder()
                .sessionId(sessionId)
                .msgId(translateVO.getMsgId())
                .build()));
    }

    /**
     * 번역 언어 목록 조회
     */
    @GetMapping("/language")
    public ResponseEntity<ResponseDto<List<GetTranslateLanguageResponseDto>>> getTranslateLanguages() {

        List<CommonCodeVO> translateLanguageComnCodes = commonCodeModuleService.getCommonCodes(CommonConst.TRANSLATE_LANGUAGE_CODE_GROUP);

        return ResponseEntity.ok().body(Response.TRANSLATE_TRANSLATE_LANGUAGES.toResponseDto(GetTranslateLanguageResponseDto
                .toList(translateLanguageComnCodes)));
    }
}
