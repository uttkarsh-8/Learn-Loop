package com.oath2.oath20.controller;

import com.oath2.oath20.config.userConfig.UserInfoManagerConfig;
import com.oath2.oath20.dto.AuthResponseDto;
import com.oath2.oath20.dto.UserRegistrationDto;
import com.oath2.oath20.dto.UserSignInDto;
import com.oath2.oath20.entity.UserInfoEntity;
import com.oath2.oath20.repository.UserInfoRepository;
import com.oath2.oath20.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final UserInfoManagerConfig userInfoManagerConfig;

//    @PostMapping("sign-in")
//    public ResponseEntity<?> authenticateUser(Authentication authentication, HttpServletResponse response){
//
//        return ResponseEntity.ok(authService.getJwtTokensAfterAuthentication(authentication, response));
//    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> authenticateUser(@RequestBody UserSignInDto signInRequest, HttpServletResponse response) {
        // Fetch the user details using UserDetailsService
        UserDetails userDetails = userInfoManagerConfig.loadUserByUsername(signInRequest.getEmail());

        // Check if the password matches
        if (!passwordEncoder.matches(signInRequest.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
        }

        // Create authentication token
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails.getUsername(), null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate and return JWT tokens
        return ResponseEntity.ok(authService.getJwtTokensAfterAuthentication(authentication, response));
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationDto userRegistrationDto, BindingResult bindingResult, HttpServletResponse httpServletResponse){

        if (bindingResult.hasErrors()) {

            List<String> errorMessage = bindingResult.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .toList();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
        }

        return ResponseEntity.ok(authService.registerUser(userRegistrationDto,httpServletResponse));
    }

    @PreAuthorize("hasAuthority('SCOPE_REFRESH_TOKEN')")
    @PostMapping("/refresh-token")
    public ResponseEntity<?> getAccessToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader){

        return ResponseEntity.ok(authService.getAccessTokenUsingRefreshToken(authorizationHeader));
    }

}
