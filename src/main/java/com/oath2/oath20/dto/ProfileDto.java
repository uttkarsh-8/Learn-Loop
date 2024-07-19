package com.oath2.oath20.dto;

public record ProfileDto(
        String username,
        String fullName,
        String profilePictureUrl,
        String bio,
        String userRole,
        String company,
        Integer yearsOfExperience
) {
}
