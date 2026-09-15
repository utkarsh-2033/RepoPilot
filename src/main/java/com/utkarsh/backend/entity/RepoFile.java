package com.utkarsh.backend.entity;

public record RepoFile(
                String encoding,
                int size,
                String name,
                String path,
                String content
) {
}
