package com.oath2.oath20.service;

import com.oath2.oath20.config.jwtConfig.JwtTokenGenerator;
import com.oath2.oath20.dto.AuthResponseDto;
import com.oath2.oath20.dto.TokenType;
import com.oath2.oath20.dto.UserRegistrationDto;
import com.oath2.oath20.entity.RefreshTokenEntity;
import com.oath2.oath20.entity.UserInfoEntity;
import com.oath2.oath20.mapper.UserInfoMapper;
import com.oath2.oath20.repository.RefreshTokenRepository;
import com.oath2.oath20.repository.UserInfoRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserInfoRepository userInfoRepository;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserInfoMapper userInfoMapper;

    public AuthResponseDto getJwtTokensAfterAuthentication(Authentication auhthentication, HttpServletResponse response){
        try {
            var userInfoEntity = userInfoRepository.findByEmailId(auhthentication.getName()).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER NOT FOUND"));

            String accessToken = jwtTokenGenerator.generateAccessToken(auhthentication);
            String refreshToken = jwtTokenGenerator.generateRefreshToken(auhthentication);

            saveUserRefreshToken(userInfoEntity,refreshToken);
            createRefreshTokenCookie(response, refreshToken);

            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .accessTokenExpiry(15*60)
                    .username(userInfoEntity.getUsername())
                    .tokenType(TokenType.Bearer)
                    .build();

        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please try again!! the error might be: " + e.getMessage());
        }
    }

    private void createRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);

        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setMaxAge(15*24*60*60);

        response.addCookie(refreshTokenCookie);

    }               

    private void saveUserRefreshToken(UserInfoEntity userInfoEntity, String refreshToken) {
        var refreshTokenEntity = RefreshTokenEntity.builder()
                .user(userInfoEntity)
                .refreshToken(refreshToken)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshTokenEntity);
    }

    public Object getAccessTokenUsingRefreshToken(String authorizationHeader) {

        if(!authorizationHeader.startsWith(TokenType.Bearer.name())){

            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Please verify token type");
        }

        final String refreshToken = authorizationHeader.substring(7);

        var refreshTokenEntity = refreshTokenRepository.findByRefreshToken(refreshToken)
                .filter(tokens-> !tokens.isRevoked())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Refresh token revoked"));

        UserInfoEntity userInfoEntity = refreshTokenEntity.getUser();

        Authentication authentication =  createAuthenticationObject(userInfoEntity);

        String accessToken = jwtTokenGenerator.generateAccessToken(authentication);

        return  AuthResponseDto.builder()
                .accessToken(accessToken)
                .accessTokenExpiry(5 * 60)
                .username(userInfoEntity.getUsername())
                .tokenType(TokenType.Bearer)
                .build();
    }

    private static Authentication createAuthenticationObject(UserInfoEntity userInfoEntity) {
        String username = userInfoEntity.getEmailId();
        String password = userInfoEntity.getPassword();
        String roles = userInfoEntity.getRoles();

        String[] roleArray = roles.split(",");
        GrantedAuthority[] authorities = Arrays.stream(roleArray)
                .map(role -> (GrantedAuthority) role::trim)
                .toArray(GrantedAuthority[]::new);

        return new UsernamePasswordAuthenticationToken(username, password, Arrays.asList(authorities));
    }

    public void enableUser(String email) {
        UserInfoEntity user = userInfoRepository.findByEmailId(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(true);
        userInfoRepository.save(user);
    }

    public AuthResponseDto registerUser(UserRegistrationDto userRegistrationDto,HttpServletResponse httpServletResponse) {

        try {

            Optional<UserInfoEntity> user = userInfoRepository.findByEmailId(userRegistrationDto.userEmail());
            if (user.isPresent()) {
                throw new Exception("User Already Exist");
            }

            UserInfoEntity userDetailsEntity = userInfoMapper.convertToEntity(userRegistrationDto);
            Authentication authentication = createAuthenticationObject(userDetailsEntity);


            // Generate a JWT token
            String accessToken = jwtTokenGenerator.generateAccessToken(authentication);
            String refreshToken = jwtTokenGenerator.generateRefreshToken(authentication);

            UserInfoEntity savedUserDetails = userInfoRepository.save(userDetailsEntity);
            saveUserRefreshToken(userDetailsEntity, refreshToken);

            createRefreshTokenCookie(httpServletResponse, refreshToken);

            return AuthResponseDto.builder()
                    .accessToken(accessToken)
                    .accessTokenExpiry(5 * 60)
                    .username(savedUserDetails.getUsername())
                    .tokenType(TokenType.Bearer)
                    .build();


        } catch (Exception e) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
