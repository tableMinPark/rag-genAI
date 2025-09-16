package com.genai.client.response;

import com.genai.client.vo.AnswerVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerResponse {

    private String status;

    private String message;

    private List<AnswerVo> data = Collections.emptyList();
}
