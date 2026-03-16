package com.genai.core.service.business.vo;

import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.service.module.vo.ConversationVO;
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

    private boolean isChangeTopic;

    private List<ConversationVO> conversations;

    private List<ConversationVO> multiturnConversations;

    private String query;

    private String rewriteQuery;

    private List<Rerank> reranks;
}
