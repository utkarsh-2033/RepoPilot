package com.utkarsh.backend.controller;

import com.utkarsh.backend.dto.UserResponse;
import com.utkarsh.backend.entity.User;
import com.utkarsh.backend.service.auth.AppUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class UserController {

    @GetMapping("api/user/me")
    public ResponseEntity<UserResponse> getUser(@AuthenticationPrincipal AppUserPrincipal user) {
        User u = user.getUser();
        UserResponse userResponse =
                new UserResponse(u.getId(),
                        u.getUsername(),
                        u.getEmail(),
                        u.getName(),
                        u.getAvatarUrl());
        return ResponseEntity.ok(userResponse);

    }

    @GetMapping("/login_url")
    public ResponseEntity<String> getLoginUrl() {
        String loginUrl = "oauth2/authorization/github";
        return ResponseEntity.ok(loginUrl);
    }
}
