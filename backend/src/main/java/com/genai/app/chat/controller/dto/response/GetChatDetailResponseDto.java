package com.genai.app.chat.controller.dto.response;

import com.genai.app.chat.service.vo.ChatDetailVO;
import com.genai.app.chat.service.vo.ChatPassageVO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetChatDetailResponseDto {
    
    private Long msgId;

    private Long chatId;

    private String query;

    private String answer;

    private LocalDateTime sysCreateDt;

    private LocalDateTime sysModifyDt;

    private List<ChatPassageVO> passages;

    public static GetChatDetailResponseDto of(ChatDetailVO chatDetailVo) {
        return GetChatDetailResponseDto.builder()
                .msgId(chatDetailVo.getMsgId())
                .chatId(chatDetailVo.getChatId())
                .query(chatDetailVo.getQuery())
                .answer(chatDetailVo.getAnswer())
                .sysCreateDt(chatDetailVo.getSysCreateDt())
                .sysModifyDt(chatDetailVo.getSysModifyDt())
                .passages(chatDetailVo.getPassages())
                .build();
    }

    public static List<GetChatDetailResponseDto> toList(List<ChatDetailVO> chatDetailVos) {
        return chatDetailVos.stream().map(GetChatDetailResponseDto::of).toList();
    }
}
