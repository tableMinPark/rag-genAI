package com.genai.adapter.out;

import com.genai.application.domain.Prompt;
import com.genai.application.port.PromptPort;
import com.genai.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PromptPortAdapter implements PromptPort {

    /**
     * 프롬 프트 코드 기준 프롬 프트 조회
     *
     * @param promptCode 프롬 프트 코드
     * @return 프롬 프트
     */
    @Override
    public Prompt getPrompt(String promptCode) {
        return switch (promptCode) {
            case "PROM-001" -> Prompt.builder()
                    .promptCode("PROM-001")
                    .promptName("법령")
                    .temperature(0.01)
                    .topP(0.9)
                    .maxTokens(2048)
                    .context("""
                            {
                              "system_prompt": {
                                "role": "system",
                                "content": [
                                  {
                                    "purpose": "이 시스템은 법령 관련 질의에 대해 RAG 기반으로 검색된 법령 원문을 참고하여 질의에 대한 답변을 제공하는 역할을 합니다."
                                  },
                                  {
                                    "rules": [
                                      "항상 500자 이내로 답변을 합니다.",
                                      "항상 검색된 법령 조항 또는 문서를 근거로 답변을 작성해야 합니다.",
                                      "검색 결과가 불충분하거나 관련 법령이 없을 경우, '관련 법령을 찾을 수 없음'을 명시합니다.",
                                      "개인적인 의견, 해석, 법률 자문은 하지 않습니다.",
                                      "간결하고 명확한 문장을 사용해야 합니다.",
                                      "전문 용어는 간단히 풀어서 설명합니다.",
                                      "항상 중립적이고 객관적인 톤을 유지합니다."
                                    ]
                                  }
                                ]
                              }
                            }
                            """)
                    .build();
            default -> throw new NotFoundException(promptCode + "을(를) 가진 프롬 프트를 찾을 수 없습니다.");
        };
    }
}