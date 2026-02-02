package com.genai.core.service.business.vo;

import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.repository.wrapper.Rerank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Builder
@Getter
@AllArgsConstructor
public class QuestionContextVO {

    private List<ConversationVO> conversations;

    private String query;

    private String rewriteQuery;

    private List<Rerank> reranks;
}
