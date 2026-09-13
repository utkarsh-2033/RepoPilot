package com.utkarsh.backend.controller.github;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/github")
public class GithubController {

    private final RestClient restClient;

    public GithubController(RestClient restClient ) {
        this.restClient = restClient;
    }

    @GetMapping("/user")
    public ResponseEntity<String> getUser() {
         return restClient.get()
                 .uri("https://api.github.com/user/repos")
                 .retrieve()
                 .toEntity(String.class);
    }
}
