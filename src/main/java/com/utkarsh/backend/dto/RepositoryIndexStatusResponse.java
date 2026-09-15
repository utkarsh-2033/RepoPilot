package com.utkarsh.backend.dto;

public record RepositoryIndexStatusResponse(
        String indexStatus,
        String message,
        int filesProcessed,
        int totalFiles,
        int chunkCount
) {
}
