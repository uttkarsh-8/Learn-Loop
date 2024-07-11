package com.oath2.oath20.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OtpService {
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${otp.expiry.seconds}")
    private long otpExpirySeconds;

    public void generateAndSendOtp(String email) {
        String otp = generateNumericOtp();
        String hashedOtp = passwordEncoder.encode(otp);

        String key = "otp:" + email;
        redisTemplate.opsForValue().set(key, hashedOtp, otpExpirySeconds, TimeUnit.SECONDS);

        emailService.sendOtpEmail(email, otp);
    }

    public boolean verifyOtp(String email, String otp) {
        String key = "otp:" + email;
        String hashedOtp = redisTemplate.opsForValue().get(key);

        if (hashedOtp == null) {
            return false;
        }

        if (passwordEncoder.matches(otp, hashedOtp)) {
            redisTemplate.delete(key);
            return true;
        }

        return false;
    }

    private String generateNumericOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.format("%06d", otp);
    }
}