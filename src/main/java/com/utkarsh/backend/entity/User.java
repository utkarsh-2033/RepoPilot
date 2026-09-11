package com.utkarsh.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String provider;
    private Integer providerId;
    private String avatarUrl;
    private String username;
    private String email;
    private String accessToken;
}
