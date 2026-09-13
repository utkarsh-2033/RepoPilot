package com.utkarsh.backend.service;

import com.utkarsh.backend.dto.GithubRepository;
import com.utkarsh.backend.dto.GithubRepositoryResponse;
import com.utkarsh.backend.entity.Repository;
import com.utkarsh.backend.repository.RepositoryRepo;
import com.utkarsh.backend.service.github.GithubApiClient;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RepoService {
    private final GithubApiClient githubApiClient;
    private final RepositoryRepo repositoryRepo;
    private final RepositoryPersistenceService repositoryPersistenceService;

    public RepoService(GithubApiClient githubApiClient ,
                       RepositoryRepo repositoryRepo,
                       RepositoryPersistenceService repositoryPersistenceService) {
        this.repositoryRepo = repositoryRepo;
        this.githubApiClient = githubApiClient;
        this.repositoryPersistenceService = repositoryPersistenceService;
    }

    public void syncRepositories(UUID userId) {
        List<GithubRepository> repos= githubApiClient.getUserRepositories();
        repositoryPersistenceService.saveRepositories(repos, userId);
    }

    public List<GithubRepositoryResponse> getUserRepositories(UUID userId) {
        List<Repository> repos=repositoryRepo.findByUserId(userId);
        List<GithubRepositoryResponse> response= new ArrayList<>();
        repos.forEach(repo -> {
            GithubRepositoryResponse r = new GithubRepositoryResponse(
                    repo.getGithubRepoId()
                    , repo.getName()
                    , repo.getFullName()
                    , repo.getDescription()
                    , repo.getHtmlUrl()
                    , repo.getDefaultBranch()
                    , repo.getLanguage()
                    , repo.getOwner()
                    , repo.getOwnerHtmlUrl()
                    , repo.getIsPrivate()
                    , repo.getCreatedAt()
                    , repo.getUpdatedAt()
            );
            response.add(r);
        });
        return response.stream()
                .sorted(Comparator.comparing(GithubRepositoryResponse::createdAt).reversed())
                .toList();
    }

    public GithubRepositoryResponse getRepositoryById(UUID userId, Long githubRepoId) {
        Optional<Repository> repoOpt = repositoryRepo.findByUserIdAndGithubRepoId(userId, githubRepoId);
        if (repoOpt.isPresent()) {
            Repository repo = repoOpt.get();
            return new GithubRepositoryResponse(
                    repo.getGithubRepoId()
                    , repo.getName()
                    , repo.getFullName()
                    , repo.getDescription()
                    , repo.getHtmlUrl()
                    , repo.getDefaultBranch()
                    , repo.getLanguage()
                    , repo.getOwner()
                    , repo.getOwnerHtmlUrl()
                    , repo.getIsPrivate()
                    , repo.getCreatedAt()
                    , repo.getUpdatedAt()
            );
        } else {
            throw new NoSuchElementException("Repository not found for user: " + userId + " and githubRepoId: " + githubRepoId);
        }
    }
}
