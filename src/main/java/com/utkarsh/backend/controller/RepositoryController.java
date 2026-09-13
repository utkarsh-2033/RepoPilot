package com.utkarsh.backend.controller;

import com.utkarsh.backend.dto.GithubRepositoryResponse;
import com.utkarsh.backend.service.RepoService;
import com.utkarsh.backend.service.auth.AppUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class RepositoryController {
    private final RepoService repoService;

    public RepositoryController(RepoService repoService) {
        this.repoService = repoService;
    }

    @GetMapping("/repos")
    public ResponseEntity<List<GithubRepositoryResponse>> getUserRepositories( @AuthenticationPrincipal AppUserPrincipal user) {
        List<GithubRepositoryResponse> repos = repoService.getUserRepositories(user.getUser().getId());
        return ResponseEntity.ok(repos);
    }
    @GetMapping("/repos/{githubRepoId}")
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

}
