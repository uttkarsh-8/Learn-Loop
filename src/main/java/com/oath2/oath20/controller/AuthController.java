package com.oath2.oath20.controller;

import com.oath2.oath20.config.userConfig.UserInfoManagerConfig;
import com.oath2.oath20.dto.AuthResponseDto;
import com.oath2.oath20.dto.UserRegistrationDto;
import com.oath2.oath20.dto.UserSignInDto;
import com.oath2.oath20.entity.UserInfoEntity;
import com.oath2.oath20.service.AuthService;
import com.oath2.oath20.service.OtpService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final UserInfoManagerConfig userInfoManagerConfig;
    private final OtpService otpService;

    @PostMapping("/sign-in")
    public ResponseEntity<?> authenticateUser(@RequestBody UserSignInDto signInRequest, HttpServletResponse response) {
        // Fetch the user details using UserDetailsService
        UserDetails userDetails = userInfoManagerConfig.loadUserByUsername(signInRequest.getEmail());

        // Check if the password matches
        if (!passwordEncoder.matches(signInRequest.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        // Check if the user is enabled (has verified their email)
        if (!userDetails.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account not verified. Please verify your email.");
        }

        // Create authentication token
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate and return JWT tokens
        return ResponseEntity.ok(authService.getJwtTokensAfterAuthentication(authentication, response));
    }

    @PostMapping("/sign-up")
    @RateLimiter(name = "default")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto userRegistrationDto, BindingResult bindingResult, HttpServletResponse httpServletResponse) {
        if (bindingResult.hasErrors()) {
            List<String> errorMessage = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        AuthResponseDto response = authService.registerUser(userRegistrationDto, httpServletResponse);
        otpService.generateAndSendOtp(userRegistrationDto.userEmail());
        return ResponseEntity.ok("User registered. Please check your email for OTP to verify your account.");
    }

    @PostMapping("/verify-otp")
    @RateLimiter(name = "default")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        boolean isValid = otpService.verifyOtp(email, otp);
        if (isValid) {
            authService.enableUser(email);
            return ResponseEntity.ok("Email verified successfully. You can now sign in.");
        } else {
            return ResponseEntity.badRequest().body("Invalid OTP.");
        }
    }

    @PostMapping("/resend-otp")
    @RateLimiter(name = "default")
    public ResponseEntity<?> resendOtp(@RequestParam String email) {
        otpService.generateAndSendOtp(email);
        return ResponseEntity.ok("New OTP sent to your email.");
    }

    @PreAuthorize("hasAuthority('SCOPE_REFRESH_TOKEN')")
    @PostMapping("/refresh-token")
    public ResponseEntity<?> getAccessToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResponseEntity.ok(authService.getAccessTokenUsingRefreshToken(authorizationHeader));
    }
}