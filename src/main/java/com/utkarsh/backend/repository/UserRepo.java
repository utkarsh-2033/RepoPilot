package com.utkarsh.backend.repository;

import com.utkarsh.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepo extends JpaRepository<User, UUID> {
    Optional<User> findByProviderAndProviderId(String provider, Integer providerId);
}
