package com.utkarsh.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record SessionsResponseDto(
        UUID id,
        String title,
        Instant updatedAt,
        UUID repositoryId,
        String repositoryName
) {
}
