package com.genai.global.auth.service.impl;

import com.genai.global.auth.controller.dto.request.RegisterRequestDto;
import com.genai.global.auth.service.AuthService;
import com.genai.global.auth.repository.MemberRepository;
import com.genai.global.auth.repository.entity.MemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void register(RegisterRequestDto registerRequestDto) {

        if (memberRepository.existsByUserId(registerRequestDto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        MemberEntity member = MemberEntity.builder()
                .userId(registerRequestDto.getUserId())
                .password(passwordEncoder.encode(registerRequestDto.getPassword()))
                .name(registerRequestDto.getName())
                .email(registerRequestDto.getEmail())
                .role("ROLE_USER")
                .build();

        memberRepository.save(member);
    }
}
