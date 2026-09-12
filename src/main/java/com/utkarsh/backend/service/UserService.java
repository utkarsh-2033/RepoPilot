package com.utkarsh.backend.service;

import com.utkarsh.backend.entity.User;
import com.utkarsh.backend.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepo userRepo;

    public UserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public void registerOrUpdateUser(String provider ,
                                     Integer providerId,
                                     String username,
                                     String email,
                                     String accessToken ,
                                     String avatarUrl ,
                                     String name
                                     ) {

        Optional<User> existingUser = userRepo.findByProviderAndProviderId(provider, providerId);

        if(existingUser.isPresent()) {
            User user = existingUser.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setAccessToken(accessToken);
            user.setAvatarUrl(avatarUrl);
            user.setName(name);
            userRepo.save(user);
        } else {
            User newUser = new User();
            newUser.setProvider(provider);
            newUser.setProviderId(providerId);
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setAccessToken(accessToken);
            newUser.setAvatarUrl(avatarUrl);
            newUser.setName(name);
            userRepo.save(newUser);
        }
    }


    public User getUserByProviderAndProviderId(String provider, Integer id) {
        return userRepo.findByProviderAndProviderId(provider, id).orElse(null);
    }
}
