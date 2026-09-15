package com.utkarsh.backend.controller;

import com.utkarsh.backend.dto.GithubRepositoryResponse;
import com.utkarsh.backend.dto.RepositoryIndexStatusResponse;
import com.utkarsh.backend.service.RepoService;
import com.utkarsh.backend.service.auth.AppUserPrincipal;
import com.utkarsh.backend.service.indexing.IndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class RepositoryController {
    private final RepoService repoService;
    private final IndexingService indexingService;

    public RepositoryController(RepoService repoService, IndexingService indexingService) {
        this.repoService = repoService;
        this.indexingService = indexingService;
    }

    @GetMapping("/repos")
    public ResponseEntity<List<GithubRepositoryResponse>> getUserRepositories( @AuthenticationPrincipal AppUserPrincipal user) {
        List<GithubRepositoryResponse> repos = repoService.getUserRepositories(user.getUser().getId());
        return ResponseEntity.ok(repos);
    }
    @GetMapping("/repo/{githubRepoId}")
    public ResponseEntity<GithubRepositoryResponse> getRepositoryById(@PathVariable Long githubRepoId, @AuthenticationPrincipal AppUserPrincipal user) {
        GithubRepositoryResponse repo = repoService.getRepositoryById(user.getUser().getId(), githubRepoId);
        if (repo != null) {
            return ResponseEntity.ok(repo);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @PostMapping("repos/sync")
    public ResponseEntity<Void> syncUserRepositories(@AuthenticationPrincipal AppUserPrincipal user) {
        repoService.syncRepositories(user.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/repo/{id}/index")
    public ResponseEntity<Void> indexRepository(@PathVariable Long id, @AuthenticationPrincipal AppUserPrincipal user) {
        try {
            indexingService.startIndexing(user.getUser().getId(), id);
            indexingService.asyncIndexing(user.getUser().getId(), id);
        } catch (Exception e) {
            indexingService.markFailedStataus(user.getUser().getId(), id, e.getMessage());
            return ResponseEntity.status(500).build();
        }
        return ResponseEntity.accepted().build();
    }
    @GetMapping("repo/{id}/status")
    public RepositoryIndexStatusResponse status(@PathVariable Long  id , @AuthenticationPrincipal AppUserPrincipal user) {
        return repoService.getRepoIndexingStatus(id, user.getUser().getId());
    }

}
