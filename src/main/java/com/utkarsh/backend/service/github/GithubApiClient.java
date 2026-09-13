package com.utkarsh.backend.service.github;

import com.utkarsh.backend.dto.GithubRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
public class GithubApiClient {

    private final RestClient restClient;

    public GithubApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<GithubRepository> getUserRepositories() {
        List<GithubRepository> repos=new ArrayList<>();
        int page=1;
        while (true){
            List<GithubRepository> pageRepos = restClient.get()
                    .uri("https://api.github.com/user/repos?per_page=30&page=" + page)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<GithubRepository>>(){});

            if(pageRepos==null || pageRepos.isEmpty()){
                break;
            }
            repos.addAll(pageRepos);
            page++;
            if(pageRepos.size() < 30) break;
        }
        return repos;
    }
}
