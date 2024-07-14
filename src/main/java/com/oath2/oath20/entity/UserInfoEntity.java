package com.oath2.oath20.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "USER_INFO")
public class UserInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "USER_NAME")
    private String username;

    @Column(name = "EMAIL_ID", unique = true, nullable = false)
    private String emailId;

    @Column(name = "PASSWORD", nullable = false)
    private String password;

    @Column(name = "MOBILE_NUMBER")
    private String mobileNumber;

    @Column(nullable = false, name = "ROLES")
    private String roles;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = false;

    @Column(name = "FULL_NAME")
    private String fullName;

    @Column(name = "PROFILE_PICTURE_KEY")
    private String profilePictureKey;

    @Column(name = "BIO", length = 500)
    private String bio;

    @Column(name = "USER_ROLE")
    private String userRole;

    @Column(name = "COMPANY")
    private String company;

    @Column(name = "YEARS_OF_EXPERIENCE")
    private Integer yearsOfExperience;
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RefreshTokenEntity> refreshTokens;

}
