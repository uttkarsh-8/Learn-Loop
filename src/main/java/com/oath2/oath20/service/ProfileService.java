package com.oath2.oath20.service;

import com.oath2.oath20.dto.ProfileDto;
import com.oath2.oath20.entity.UserInfoEntity;
import com.oath2.oath20.repository.UserInfoRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final UserInfoRepository userInfoRepository;
    private final S3Service s3Service;
    private final CloudFrontService cloudFrontService;

    public ProfileService(UserInfoRepository userInfoRepository,
                          S3Service s3Service,
                          CloudFrontService cloudFrontService) {
        this.userInfoRepository = userInfoRepository;
        this.s3Service = s3Service;
        this.cloudFrontService = cloudFrontService;
    }

    public ProfileDto getProfile(String username) {
        logger.debug("Fetching profile for user: {}", username);
        return userInfoRepository.findByUsername(username)
                .map(this::convertToDto)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });
    }

    @Transactional
    public ProfileDto updateProfile(String username, ProfileDto profileDto) {
        logger.debug("Updating profile for user: {}", username);
        UserInfoEntity user = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        updateUserFields(user, profileDto);
        UserInfoEntity savedUser = userInfoRepository.save(user);
        logger.info("Profile updated successfully for user: {}", username);
        return convertToDto(savedUser);
    }

    @Transactional
    public ProfileDto updateProfilePicture(String username, MultipartFile file) {
        logger.debug("Updating profile picture for user: {}", username);
        UserInfoEntity user = userInfoRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        String newImageKey = s3Service.uploadFile(file);
        String oldImageKey = user.getProfilePictureKey();

        user.setProfilePictureKey(newImageKey);
        UserInfoEntity savedUser = userInfoRepository.save(user);

        if (oldImageKey != null) {
            s3Service.deleteFile(oldImageKey);
        }

        logger.info("Profile picture updated successfully for user: {}", username);
        return convertToDto(savedUser);
    }

    private ProfileDto convertToDto(UserInfoEntity user) {
        return new ProfileDto(
                user.getUsername(),
                user.getFullName(),
                Optional.ofNullable(user.getProfilePictureKey())
                        .map(cloudFrontService::getSignedUrl)
                        .orElse(null),
                user.getBio(),
                user.getUserRole(),
                user.getCompany(),
                user.getYearsOfExperience()
        );
    }

    private void updateUserFields(UserInfoEntity user, ProfileDto profileDto) {
        user.setFullName(profileDto.fullName());
        user.setBio(profileDto.bio());
        user.setUserRole(profileDto.currentRole());
        user.setCompany(profileDto.company());
        user.setYearsOfExperience(profileDto.yearsOfExperience());
    }
}