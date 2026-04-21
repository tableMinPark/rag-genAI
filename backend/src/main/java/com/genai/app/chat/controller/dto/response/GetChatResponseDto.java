package com.genai.app.chat.controller.dto.response;

import com.genai.app.chat.service.vo.ChatVO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetChatResponseDto {

    private Long chatId;

    private String title;

    private String menuCode;

    private LocalDateTime sysCreateDt;

    private LocalDateTime sysModifyDt;

    public static GetChatResponseDto of(ChatVO chatVO) {
        return GetChatResponseDto.builder()
                .chatId(chatVO.getChatId())
                .title(chatVO.getTitle())
                .menuCode(chatVO.getMenuCode())
                .sysCreateDt(chatVO.getSysCreateDt())
                .sysModifyDt(chatVO.getSysModifyDt())
                .build();
    }

    public static List<GetChatResponseDto> toList(List<ChatVO> chatVOs) {
        return chatVOs.stream().map(GetChatResponseDto::of).toList();
    }
}
