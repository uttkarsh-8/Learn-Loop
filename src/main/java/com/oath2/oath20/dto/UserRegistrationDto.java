package com.oath2.oath20.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;

public record UserRegistrationDto (

        @NotEmpty(message = "User Name must not be empty")
        String userName,

        @NotEmpty(message = "Mobile number must not be empty")
        String userMobileNo,

        @NotEmpty(message = "User email must not be empty")
        @Email(message = "Invalid email format")
        String userEmail,

        @NotEmpty(message = "User password must not be empty")
        String userPassword,

        @NotEmpty(message = "User role must not be empty")
        String userRole
){ }
