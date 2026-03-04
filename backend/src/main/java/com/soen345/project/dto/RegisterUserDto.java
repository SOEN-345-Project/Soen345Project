package com.soen345.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterUserDto {
    @NotNull(message = "First name is required")
    private String firstName;
    @NotNull(message = "Last name is required")
    private String lastName;
    private String email;
    @NotNull(message = "Password is required")
    private String password;
    private String phoneNumber;
    private String verificationMethod = "EMAIL";
}
