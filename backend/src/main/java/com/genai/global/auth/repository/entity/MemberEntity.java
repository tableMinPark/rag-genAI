package com.genai.global.auth.repository.entity;

import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "GEN_MEMBER")
@EntityListeners(AuditingEntityListener.class)
public class MemberEntity {

    @Id
    @Column(name = "user_id", length = 50)
    @Comment("사용자 ID")
    private String userId;

    @Column(name = "password", length = 200, nullable = false)
    @Comment("비밀번호 (BCrypt)")
    private String password;

    @Column(name = "name", length = 100, nullable = false)
    @Comment("이름")
    private String name;

    @Column(name = "email", length = 200)
    @Comment("이메일")
    private String email;

    @Column(name = "role", length = 20, nullable = false)
    @Comment("권한")
    private String role;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    @Comment("생성 일자")
    private LocalDateTime createdAt;
}
