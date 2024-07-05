package com.oath2.oath20.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class DashboardController {

//    @PreAuthorize("hasAnyRole('ROLE_MANAGER', 'ROLE_ADMIN', 'ROLE_USER')")
    @PreAuthorize("hasAuthority('SCOPE_READ')")
    @GetMapping("welcome-message")
    public ResponseEntity<String> getFirstWelcomeMessage(Authentication authentication){
        System.out.println(authentication.getAuthorities()+ " "+ authentication.getCredentials() );
        return ResponseEntity.ok("Welcome to JWT oauth2.0:: "+ authentication.getName() + " with scope: " + authentication.getAuthorities());
    }

//    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PreAuthorize("hasAuthority('SCOPE_WRITE')")

    @GetMapping("/admin-message")
    public ResponseEntity<String> getAdminData(@RequestParam("message")String message, Principal principal){
        return ResponseEntity.ok("Admin :: " + principal.getName()+ " has this message: " + message);
    }

//    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PreAuthorize("hasAuthority('SCOPE_READ')")
    @GetMapping("manager")
    public ResponseEntity<String> getManagerData(Principal principal){
        return ResponseEntity.ok("Manager :: " + principal.getName());
    }
}
