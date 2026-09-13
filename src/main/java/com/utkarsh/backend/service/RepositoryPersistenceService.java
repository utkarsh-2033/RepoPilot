package com.utkarsh.backend.service;

import com.utkarsh.backend.dto.GithubRepository;
import com.utkarsh.backend.entity.Repository;
import com.utkarsh.backend.repository.RepositoryRepo;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RepositoryPersistenceService {
    private final RepositoryRepo repositoryRepo;

    public RepositoryPersistenceService(RepositoryRepo repositoryRepo) {
        this.repositoryRepo = repositoryRepo;
    }

    @Transactional
    public void saveRepositories(List<GithubRepository> repos, UUID userId) {
        repos.forEach(repo ->{
            Optional<Repository> existingRepo = repositoryRepo.findByUserIdAndGithubRepoId(userId, repo.githubRepoId());
            if(existingRepo.isPresent()){
                Repository repository = existingRepo.get();
                repository.setName(repo.name());
                repository.setFullName(repo.fullName());
                repository.setDescription(repo.description());
                repository.setHtmlUrl(repo.htmlUrl());
                repository.setDefaultBranch(repo.defaultBranch());
                repository.setLanguage(repo.language());
                repository.setOwner(repo.owner().login());
                repository.setOwnerHtmlUrl(repo.owner().html_url());
                repository.setIsPrivate(repo.isPrivate());
                repositoryRepo.save(repository);
                return;
            }
            Repository repository = Repository.builder()
                    .githubRepoId(repo.githubRepoId())
                    .owner(repo.owner().login())
                    .ownerHtmlUrl(repo.owner().html_url())
                    .name(repo.name())
                    .fullName(repo.fullName())
                    .isPrivate(repo.isPrivate())
                    .defaultBranch(repo.defaultBranch())
                    .language(repo.language())
                    .userId(userId)
                    .build();
            repositoryRepo.save(repository);
        });
    }
}
