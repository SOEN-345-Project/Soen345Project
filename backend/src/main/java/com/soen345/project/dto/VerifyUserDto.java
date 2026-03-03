package com.soen345.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyUserDto {
    private String email;
    private String phoneNumber;
    @NotNull(message = "Verification code is required")
    private String verificationCode;
}
