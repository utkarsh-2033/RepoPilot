package com.utkarsh.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name="user_id", nullable = false)
    private UUID userid;
    @Column(name="repository_id", nullable = false)
    private UUID repositoryId;
    @Column(name="title", nullable = false)
    private String title;
    @Column(name="updated_at", nullable = false)
    private Instant updatedAt;


    @PrePersist
    protected void onCreate() {
        if(this.updatedAt == null)
            this.updatedAt = Instant.now();

        if (title == null || title.isBlank())
            title = "New chat";
    }
}
