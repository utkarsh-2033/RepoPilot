package com.utkarsh.backend.dto;

import java.time.Instant;

public record GithubRepositoryResponse(
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
        Instant updatedAt
) {
}
