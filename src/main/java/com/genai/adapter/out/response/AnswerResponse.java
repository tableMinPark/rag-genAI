package com.genai.adapter.out.response;

import com.genai.application.domain.Answer;
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

    private List<Answer> data = Collections.emptyList();
}
