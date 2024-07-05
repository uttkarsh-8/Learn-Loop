package com.oath2.oath20.config.userConfig;

import com.oath2.oath20.entity.UserInfoEntity;
import com.oath2.oath20.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

//@RequiredArgsConstructor
//@Component
//public class InitialUserInfo implements CommandLineRunner {
//
//    private final UserInfoRepository userInfoRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    public void run(String... args) throws Exception {
//        UserInfoEntity manager  = new UserInfoEntity();
//        manager.setUsername("ManagerAAA");
//        manager.setPassword(passwordEncoder.encode("password"));
//        manager.setRoles("ROLE_MANAGER");
//        manager.setEmailId("manager@manager.com");
//
//        UserInfoEntity user = new UserInfoEntity();
//        user.setUsername("UserAAA");
//        user.setPassword(passwordEncoder.encode("password"));
//        user.setRoles("ROLE_USER");
//        user.setEmailId("user@user.com");
//
//        UserInfoEntity admin = new UserInfoEntity();
//        admin.setUsername("AdminAAA");
//        admin.setPassword(passwordEncoder.encode("password"));
//        admin.setRoles("ROLE_ADMIN");
//        admin.setEmailId("admin@admin.com");
//
//        userInfoRepository.saveAll(List.of(admin, manager, user));
//    }
//}
