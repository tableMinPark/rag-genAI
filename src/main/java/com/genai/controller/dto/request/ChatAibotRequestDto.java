package com.genai.controller.dto.request;

import lombok.*;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatAibotRequestDto {

    private String tabId;

    private String query;
}
