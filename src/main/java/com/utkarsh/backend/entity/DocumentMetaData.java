package com.utkarsh.backend.entity;

import lombok.Builder;

@Builder
public record DocumentMetaData(
        Long repoId,
        String filePath,
        String language,
        Integer chunkIndex
) {
}
