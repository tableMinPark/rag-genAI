package com.genai.repository.impl;

import com.genai.repository.PromptRepository;
import com.genai.service.domain.Prompt;
import com.genai.global.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PromptRepositoryImpl implements PromptRepository {

    /**
     * TODO: 프롬 프트 코드 기준 프롬 프트 조회
     * 관리 도구 적용 이후, 관리 도구 테이블 조회 로직 작성 필요
     *
     * @param promptCode 프롬 프트 코드
     * @return 프롬 프트
     */
    @Override
    public Prompt getPrompt(String promptCode) {
        return switch (promptCode) {
            case "PROM-000" -> Prompt.builder()
                    .promptCode("PROM-000")
                    .promptName("DEFAULT LLM")
                    .maxTokens(4096)
                    .temperature(0.7)
                    .topP(0.8)
                    .minP(0)
                    .topK(20)
                    .context("""
                            {
                              "system_prompt": {
                                "role": "system",
                                "content": [
                                  {
                                    "purpose": "이 시스템은 사용자 질의에 대해 자유롭게 답변을 제공하는 역할을 합니다."
                                  },
                                  {
                                    "rules": [
                                      "항상 마크다운 형식으로 답변을 합니다.",
                                      "간결하고 명확한 문장을 사용해야 합니다.",
                                      "전문 용어는 간단히 풀어서 설명합니다.",
                                      "항상 중립적이고 객관적인 톤을 유지합니다.",
                                      "각 문장을 개행으로 구분하여 답변을 작성해야 합니다.",
                                      "비교가 필요한 경우, 표를 통해 비교 분석하여 답변합니다.",
                                      "특정 프로세스 절차가 3단계를 초과하는 경우, ***Mermaid*** 문법으로 절차에 대한 순서도를 포함하여 답변을 해야 합니다."
                                    ]
                                  }
                                ]
                              }
                            }
                            """.replace("\n", " ").replaceAll("( {2,})+", " "))
                    .build();
            case "PROM-001" -> Prompt.builder()
                    .promptCode("PROM-001")
                    .promptName("사고_법령")
                    .maxTokens(4096)
                    .temperature(0.6)
                    .topP(0.95)
                    .minP(0)
                    .topK(20)
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
                                      "항상 마크다운 형식으로 답변을 합니다.",
                                      "항상 2000자 이내로 답변을 합니다.",
                                      "검색된 법령 조항 또는 문서를 참고합니다.",
                                      "검색 결과가 불충분하거나 관련 법령이 없을 경우, '관련 법령을 찾을 수 없습니다.'라고 답변합니다.",
                                      "간결하고 명확한 문장을 사용해야 합니다.",
                                      "전문 용어는 간단히 풀어서 설명합니다.",
                                      "항상 중립적이고 객관적인 톤을 유지합니다.",
                                      "각 문장을 개행으로 구분하여 답변을 작성해야 합니다.",
                                      "비교가 필요한 경우, 표를 통해 비교 분석하여 답변합니다.",
                                      "특정 프로세스 절차가 3단계를 초과하는 경우, ***Mermaid*** 문법으로 절차에 대한 순서도를 포함하여 답변을 해야 합니다."
                                    ]
                                  }
                                ]
                              }
                            }
                            """.replace("\n", " ").replaceAll("( {2,})+", " "))
                    .build();
            case "PROM-002" -> Prompt.builder()
                    .promptCode("PROM-002")
                    .promptName("비사고_법령")
                    .maxTokens(4096)
                    .temperature(0.7)
                    .topP(0.8)
                    .minP(0)
                    .topK(20)
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
                                      "항상 마크다운 형식으로 답변을 합니다.",
                                      "항상 2000자 이내로 답변을 합니다.",
                                      "항상 검색된 법령 조항 또는 문서를 근거로 답변을 작성해야 합니다.",
                                      "검색 결과가 불충분하거나 관련 법령이 없을 경우, '관련 법령을 찾을 수 없습니다.'라고 답변합니다.",
                                      "개인적인 의견, 해석, 법률 자문은 하지 않습니다.",
                                      "간결하고 명확한 문장을 사용해야 합니다.",
                                      "전문 용어는 간단히 풀어서 설명합니다.",
                                      "항상 중립적이고 객관적인 톤을 유지합니다.",
                                      "각 문장을 개행으로 구분하여 답변을 작성해야 합니다.",
                                      "비교가 필요한 경우, 표를 통해 비교 분석하여 답변합니다.",
                                      "특정 프로세스 절차가 3단계를 초과하는 경우, ***Mermaid*** 문법으로 절차에 대한 순서도를 포함하여 답변을 해야 합니다."
                                    ]
                                  }
                                ]
                              }
                            }
                            """.replace("\n", " ").replaceAll("( {2,})+", " "))
                    .build();
            default -> throw new NotFoundException(promptCode + "을(를) 가진 프롬 프트를 찾을 수 없습니다.");
        };
    }
}