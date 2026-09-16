package com.utkarsh.backend.service.github;
import lombok.extern.slf4j.Slf4j;
import com.utkarsh.backend.dto.GithubRepository;
import com.utkarsh.backend.entity.RepoFile;
import com.utkarsh.backend.exception.GithubApiClientException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GithubApiClient {

    private final RestClient restClient;
    private final GithubApiRateLimiter rateLimiter;

    public GithubApiClient(RestClient restClient, GithubApiRateLimiter rateLimiter) {
        this.restClient = restClient;
        this.rateLimiter = rateLimiter;
    }

    public List<GithubRepository> getUserRepositories() {
        final int perPage = 30;
        List<GithubRepository> repos = new ArrayList<>();
        int page = 1;
        while (true) {
            int currentPage = page;
            try {
                rateLimiter.pause();
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
        rateLimiter.pause();
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

//    public RepoFile getFileContent(String owner , String repoName , String filePath){
//        rateLimiter.pause();
//        return restClient.get()
//                .uri(uri->uri
//                        .path("/repos/{owner}/{repo}/contents/{filePath}")
//                        .build(owner, repoName, filePath)
//                )
//                .retrieve()
//                .body(new ParameterizedTypeReference<RepoFile>() {
//                });
//    }
public RepoFile getFileContent(String owner, String repoName, String filePath) {

    long start = System.currentTimeMillis();

    try {
        ResponseEntity<RepoFile> response = restClient.get()
                .uri(uri -> uri
                        .path("/repos/{owner}/{repo}/contents/{filePath}")
                        .build(owner, repoName, filePath)
                )
                .retrieve()
                .toEntity(RepoFile.class);

//        logGithubResponse(
//                "GET_FILE_CONTENT",
//                owner,
//                repoName,
//                filePath,
//                response.getStatusCode().value(),
//                response.getHeaders(),
//                System.currentTimeMillis() - start
//        );

        return response.getBody();

    } catch (HttpClientErrorException e) {

//        logGithubError(
//                "GET_FILE_CONTENT",
//                owner,
//                repoName,
//                filePath,
//                e
//        );

        throw e;
    }
}

    private void logGithubResponse(
            String operation,
            String owner,
            String repo,
            String path,
            int status,
            HttpHeaders headers,
            long durationMs
    ) {
        log.info("""
            ===== GitHub API =====
            operation      : {}
            repository     : {}/{}
            path           : {}
            status         : {}
            duration       : {} ms
            rateLimit      : {}
            rateRemaining  : {}
            rateUsed       : {}
            rateReset      : {}
            rateResource   : {}
            retryAfter     : {}
            ======================
            """,
                operation,
                owner,
                repo,
                path,
                status,
                durationMs,
                headers.getFirst("X-RateLimit-Limit"),
                headers.getFirst("X-RateLimit-Remaining"),
                headers.getFirst("X-RateLimit-Used"),
                headers.getFirst("X-RateLimit-Reset"),
                headers.getFirst("X-RateLimit-Resource"),
                headers.getFirst("Retry-After")
        );
    }

    private void logGithubError(
            String operation,
            String owner,
            String repo,
            String path,
            HttpClientErrorException e
    ) {
        HttpHeaders headers = e.getResponseHeaders();

        log.error("""
            ===== GitHub API ERROR =====
            operation      : {}
            repository     : {}/{}
            path           : {}
            status         : {}
            response       : {}
            rateLimit      : {}
            rateRemaining  : {}
            rateUsed       : {}
            rateReset      : {}
            rateResource   : {}
            retryAfter     : {}
            ============================
            """,
                operation,
                owner,
                repo,
                path,
                e.getStatusCode().value(),
                e.getResponseBodyAsString(),
                headers != null ? headers.getFirst("X-RateLimit-Limit") : null,
                headers != null ? headers.getFirst("X-RateLimit-Remaining") : null,
                headers != null ? headers.getFirst("X-RateLimit-Used") : null,
                headers != null ? headers.getFirst("X-RateLimit-Reset") : null,
                headers != null ? headers.getFirst("X-RateLimit-Resource") : null,
                headers != null ? headers.getFirst("Retry-After") : null
        );
    }


}
