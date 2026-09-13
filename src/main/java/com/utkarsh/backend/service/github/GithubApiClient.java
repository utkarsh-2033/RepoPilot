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
        final int  perPage=30;
        List<GithubRepository> repos=new ArrayList<>();
        int page=1;
        while (true){
            int currentPage=page;
            List<GithubRepository> pageRepos =
                    restClient.get()
                            .uri(uri -> uri
                                    .path("/user/repos")
                                    .queryParam("per_page", perPage)
                                    .queryParam("page", currentPage)
                                    .build()
                            )
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
