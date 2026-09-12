package com.utkarsh.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private Integer providerId;
    @Column(name="avatar_url")
    private String avatarUrl;
    @Column(name="display_name")
    private String name;
    @Column(name="github_username")
    private String username;
    private String email;
    @Column(name = "access_token",nullable = false )
    private String accessToken;
    @Column(name = "token_scopes", length = 500)
    private String tokenScopes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
