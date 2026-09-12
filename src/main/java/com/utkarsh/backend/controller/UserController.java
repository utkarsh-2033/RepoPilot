package com.utkarsh.backend.controller;

import com.utkarsh.backend.entity.User;
import com.utkarsh.backend.service.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class UserController {

    public User getUser(@AuthenticationPrincipal AppUserPrincipal user) {
        return user.getUser();
    }
}
