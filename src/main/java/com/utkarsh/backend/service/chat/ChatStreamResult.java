package com.utkarsh.backend.service.chat;

import com.utkarsh.backend.dto.CitationDto;
import reactor.core.publisher.Flux;

import java.util.List;

public record ChatStreamResult(
        Flux<String> tokens,
        List<CitationDto> availableCitations
) {
}