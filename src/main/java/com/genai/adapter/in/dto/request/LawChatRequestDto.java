package com.genai.adapter.in.dto.request;

import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LawChatRequestDto {

    private String tabId;

    private String query;
}
