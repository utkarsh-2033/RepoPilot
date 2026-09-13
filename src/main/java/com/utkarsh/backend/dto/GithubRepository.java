package com.utkarsh.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GithubRepository(
        @JsonProperty("id") Long githubRepoId,
        String name,
        @JsonProperty("full_name") String fullName,
        String description,
        @JsonProperty("html_url") String htmlUrl,
        @JsonProperty("default_branch") String defaultBranch,
        String language,
        GithubRepoOwner owner,
        @JsonProperty("private") Boolean isPrivate,
        @JsonProperty("updated_at") String updatedAtGithub
) {
}

