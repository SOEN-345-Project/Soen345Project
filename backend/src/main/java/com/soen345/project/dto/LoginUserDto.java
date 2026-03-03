package com.soen345.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginUserDto {
    private String email;
    private String phoneNumber;
    @NotNull(message = "Password is required")
    private String password;
}
