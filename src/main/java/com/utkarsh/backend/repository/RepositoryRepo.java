package com.utkarsh.backend.repository;

import com.utkarsh.backend.entity.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepo extends JpaRepository<Repository, UUID> {

    List<Repository> findByUserId(UUID userId);

    Optional<Repository> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
}
