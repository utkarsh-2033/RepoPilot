package com.utkarsh.backend.service.github;

import com.utkarsh.backend.dto.GithubRepository;
import com.utkarsh.backend.entity.RepoFile;
import com.utkarsh.backend.exception.GithubApiClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GithubApiClient {

    private final RestClient restClient;

    public GithubApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public List<GithubRepository> getUserRepositories() {
        final int perPage = 30;
        List<GithubRepository> repos = new ArrayList<>();
        int page = 1;
        while (true) {
            int currentPage = page;
            try {
                List<GithubRepository> pageRepos =
                        restClient.get()
                                .uri(uri -> uri
                                        .path("/user/repos")
                                        .queryParam("per_page", perPage)
                                        .queryParam("page", currentPage)
                                        .build()
                                )
                                .retrieve()
                                .body(new ParameterizedTypeReference<List<GithubRepository>>() {
                                });


                if (pageRepos == null || pageRepos.isEmpty()) {
                    break;
                }
                repos.addAll(pageRepos);
                page++;
                if (pageRepos.size() < 30) break;

            } catch (HttpClientErrorException.Unauthorized e) {
                throw new GithubApiClientException("Github authorization is no longer valid", HttpStatus.UNAUTHORIZED);
            } catch (HttpClientErrorException.NotFound e) {
                throw new GithubApiClientException("GitHub resource was not found.", HttpStatus.NOT_FOUND);
            } catch (HttpClientErrorException e) {
                throw new GithubApiClientException("GitHub rejected the request.", HttpStatus.BAD_GATEWAY);
            } catch (HttpServerErrorException e) {
                throw new GithubApiClientException("GitHub Server is currently unavailable.", HttpStatus.BAD_GATEWAY);
            } catch (ResourceAccessException e) {
                throw new GithubApiClientException("Unable to communicate with GitHub.", HttpStatus.SERVICE_UNAVAILABLE);
            }
        }
        return repos;
    }

    public Map<String, Object> getRepoTree(String owner , String repoName , String branchName){

        Map<String, Object> tree=restClient.get()
                .uri(uri->uri
                        .path("/repos/{owner}/{repo}/git/trees/{branchName}")
                        .queryParam("recursive", "1")
                        .build(owner, repoName, branchName)
                )
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {
                });

       return tree;
    }

    public RepoFile getFileContent(String owner , String repoName , String filePath){
        return restClient.get()
                .uri(uri->uri
                        .path("/repos/{owner}/{repo}/contents/{filePath}")
                        .build(owner, repoName, filePath)
                )
                .retrieve()
                .body(new ParameterizedTypeReference<RepoFile>() {
                });
    }



}
