package com.genai.global.service.impl;

import com.genai.global.controller.dto.request.RegisterRequestDto;
import com.genai.global.service.AuthService;
import com.genai.global.repository.MemberRepository;
import com.genai.global.repository.entity.MemberEntity;
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
    public void register(RegisterRequestDto requestDto) {
        if (memberRepository.existsByUserId(requestDto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        MemberEntity member = MemberEntity.builder()
                .userId(requestDto.getUserId())
                .password(passwordEncoder.encode(requestDto.getPassword()))
                .name(requestDto.getName())
                .email(requestDto.getEmail())
                .role("ROLE_USER")
                .build();

        memberRepository.save(member);
    }
}
