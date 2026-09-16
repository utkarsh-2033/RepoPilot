package com.utkarsh.backend.entity;

import com.utkarsh.backend.dto.CitationDto;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String message;
    @Column(name="session_id", nullable = false)
    private UUID sessionId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<CitationDto> citations;

    @Column(name="created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ChatMessage() {

    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null)
            this.createdAt = Instant.now();
    }

    public enum Role {
        USER,
        ASSISTANT
    }

}
