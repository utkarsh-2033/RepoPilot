package com.utkarsh.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record GithubRepositoryResponse(
        UUID id,
        Long githubRepoId,
        String name,
        String fullName,
        String description,
        String htmlUrl,
        String defaultBranch,
        String language,
        String owner,
        String ownerHtmlUrl,
        Boolean isPrivate,
        Instant createdAt,
        Instant updatedAt,
        String indexStatus,
        String indexError,
        int filesProcessed,
        int filesTotal,
        int chunkCount
) {
}
