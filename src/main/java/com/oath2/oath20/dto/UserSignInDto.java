package com.oath2.oath20.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSignInDto {

        @NotEmpty(message = "Username must not be empty")
        @Email(message = "Invalid email format")
        String Email;

        @NotEmpty(message = "Password must not be empty")
        String password;

}





