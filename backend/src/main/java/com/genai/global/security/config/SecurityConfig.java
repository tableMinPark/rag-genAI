package com.genai.global.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.global.security.filter.JwtAuthenticationFilter;
import com.genai.global.security.filter.JwtVerificationFilter;
import com.genai.global.security.filter.TokenReissueFilter;
import com.genai.global.security.handler.AuthFailureHandler;
import com.genai.global.security.handler.AuthSuccessHandler;
import com.genai.global.security.service.MemberDetailsService;
import com.genai.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final MemberDetailsService memberDetailsService;
    private final AuthSuccessHandler authSuccessHandler;
    private final AuthFailureHandler authFailureHandler;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${jwt.refresh-token-cookie-name}")
    private String refreshTokenCookieName;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(memberDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationManager authManager = authenticationManager(http);

        JwtAuthenticationFilter jwtAuthFilter = new JwtAuthenticationFilter(authManager);
        jwtAuthFilter.setAuthenticationSuccessHandler(authSuccessHandler);
        jwtAuthFilter.setAuthenticationFailureHandler(authFailureHandler);

        JwtVerificationFilter jwtVerificationFilter =
                new JwtVerificationFilter(jwtUtil, memberDetailsService);

        TokenReissueFilter tokenReissueFilter =
                new TokenReissueFilter(jwtUtil, memberDetailsService, objectMapper, refreshTokenCookieName);

        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                .antMatchers("/api/auth/login", "/api/auth/register", "/api/auth/reissue", "/api/auth/logout").permitAll()
                .anyRequest().authenticated()
            .and()
            .addFilter(jwtAuthFilter)
            .addFilterBefore(jwtVerificationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(tokenReissueFilter, JwtVerificationFilter.class);

        return http.build();
    }
}
