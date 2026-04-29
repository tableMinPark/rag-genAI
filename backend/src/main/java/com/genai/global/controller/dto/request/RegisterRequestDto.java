package com.genai.global.controller.dto.request;

import lombok.Getter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
public class RegisterRequestDto {

    @NotBlank
    @Size(min = 3, max = 50)
    private String userId;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Email
    private String email;
}
