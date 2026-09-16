package com.utkarsh.backend.dto;

public record CitationDto(
        String id,
        String filePath,
        Integer startLine,
        Integer endLine,
        String language
) {
}
