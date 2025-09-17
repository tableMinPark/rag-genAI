package com.genai.repository;

import com.genai.entity.PromptEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PromptRepositoryImpl implements PromptRepository {

    /**
     * 프롬 프트 코드 기준 프롬 프트 엔티티 조회
     *
     * @param promptCode 프롬 프트 코드
     * @return 프롬 프트 엔티티
     */
    @Override
    public Optional<PromptEntity> findByPromptCode(String promptCode) {
        return switch (promptCode) {
            case "PROM-001" -> Optional.ofNullable(PromptEntity.builder()
                    .promptCode("PROM-001")
                    .promptName("법령")
                    .context("""
                    당신은 법률 상담 직원이에요.
                    제공된 context의 내용을 이해한 후, '답변 예시' 에 맞게 답변을 생성해주세요.
                    질문에 대한 답변은 무조건 제공된 context에서 찾아서 답변해주세요.
                    
                    ## 답변예시 ##
                    * 아래 유형 중 하나만 사용하여 답변하세요. 해당 내용이 답변내용에 포함되지 않도록 하세요.
                    * (법령명)은 context의 '#법령명:'에서 꼭 찾아주세요.  법령명에 조항(제1조, 제2조 등)을 반드시 포함하여 출력해 주세요.
                    
                    # 유형1 : 질문에 대한 답변이 context에 없는 경우
                    질문을 명확하게 작성해주세요.
                    
                    # 유형2 : 질문이 단일 단어인 경우
                    질문을 명확하게 작성해주세요.
                    
                    # 유형3 : 질문에 대한 답변이 context에 있는 경우
                    [참고 법령 : (법령명)]
                    
                    - (내용을 입력해주세요.)
                    - (내용을 입력해주세요.)
                    - (내용을 입력해주세요.)
                    상세한 내용은 아래 관련 출처를 확인해주세요.
                    
                    
                    ## 유의사항 ##
                    # 질문이 context에 없는 질문이면 절대 답변하지 말고 <답변예시> 의 정해진 답변을 사용하세요.\s
                    # 답변의 길이는 최대 1000자를 넘지 않게 작성해주세요.
                    # 마지막에 "상세한 내용은 아래 관련 출처를 확인해주세요." 멘트는 꼭 넣어주세요.
                    # 다시 한번 말하는데, context에 있는 법령명만 포함하여 답변을 생성해주세요.\s
                    # Context에 없는 법령명은 절대 노출하지 마세요.
                    """)
                    .build());
            default -> Optional.empty();
        };
    }
}
