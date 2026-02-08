package com.genai.core.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_COMMON_CODE")
public class CommonCodeEntity {

    @Id
    @Column(name = "code", nullable = false, unique = true)
    @Comment("공통 코드")
    private String code;

    @Column(name = "code_name")
    @Comment("공통 코드명")
    private String codeName;

    @Column(name = "code_group")
    @Comment("그룹 코드")
    private String codeGroup;

    @Column(name = "sort_order")
    @Comment("정렬 필드")
    private Integer sortOrder;

}
