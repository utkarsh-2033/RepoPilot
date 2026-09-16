package com.utkarsh.backend.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessageResponseDto(
        UUID id,
        String message,
        String role,
        Instant createdAt,
        List<CitationDto> citations
) {
}
