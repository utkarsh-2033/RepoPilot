package com.utkarsh.backend.service;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserService userService;

    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public @Nullable OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        String provider = userRequest.getClientRegistration().getRegistrationId();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        userService.registerOrUpdateUser(
                provider,
                oAuth2User.getAttribute("id"),
                oAuth2User.getAttribute("login"),
                oAuth2User.getAttribute("email"),
                userRequest.getAccessToken().getTokenValue()
        );
        return oAuth2User;
    }
}
