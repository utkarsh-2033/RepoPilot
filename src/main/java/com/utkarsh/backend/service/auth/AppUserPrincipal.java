package com.utkarsh.backend.service.auth;

import com.utkarsh.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AppUserPrincipal implements OAuth2User {

    private final User user;

    private final Map<String, Object> attributes;

    public AppUserPrincipal(User user , Map<String, Object> attributes) {
        this.user = user;
        this.attributes=attributes;

    }
    public User getUser() {
        return user;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return user.getName();
    }
}
