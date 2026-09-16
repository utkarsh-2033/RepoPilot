package com.utkarsh.backend.service.indexing;

import com.utkarsh.backend.dto.RepositoryIndexStatusResponse;
import com.utkarsh.backend.entity.IndexStatus;
import com.utkarsh.backend.entity.RepoFile;
import com.utkarsh.backend.entity.Repository;
import com.utkarsh.backend.exception.BadRequestException;
import com.utkarsh.backend.exception.RepositoryNotFoundException;
import com.utkarsh.backend.repository.RepositoryRepo;
import com.utkarsh.backend.service.github.GithubApiClient;
import com.utkarsh.backend.service.github.GithubApiRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class IndexingService {

    private static final int VECTOR_BATCH_SIZE = 32;
    private static final int PROGRESS_EVERY_N_FILES = 4;
    private static final String METADATA_REPO_ID = "repoId";

    private final  GithubApiClient githubApiClient;
    private final RepositoryRepo repositoryRepo;
    private final FileFilter fileFilter;
    private final FileChunking fileChunking;
    private final VectorStore vectorStore;;
    private final GithubApiRateLimiter rateLimiter;

    IndexingService(GithubApiClient githubApiClient,
                    RepositoryRepo repositoryRepo,
                    FileFilter fileFilter,
                    FileChunking fileChunking,
                    GithubApiRateLimiter rateLimiter,
                    VectorStore vectorStore) {
        this.githubApiClient = githubApiClient;
        this.repositoryRepo = repositoryRepo;
        this.fileFilter = fileFilter;
        this.fileChunking = fileChunking;
        this.vectorStore = vectorStore;
        this.rateLimiter = rateLimiter;
    }

    @Value("${indexing.max-file-bytes:102400}")
    private long maxFileBytes;

    public RepositoryIndexStatusResponse startIndexing(UUID userId, Long repoId) {

        Repository repo =
                repositoryRepo.findByUserIdAndGithubRepoId(userId, repoId)
                        .orElseThrow(() -> new RepositoryNotFoundException("Repository not found for user: " + userId + " and repoId: " + repoId));
        if (repo.getIndexStatus() == (IndexStatus.INDEXING)) {
            throw new BadRequestException("Indexing already in progress");
        }
        updateIndexStatus(userId, repoId, 0, 0, 0, IndexStatus.INDEXING, null);
        return new RepositoryIndexStatusResponse(
                repo.getIndexStatus().toString(),
                "Indexing started",
                0,
                0,
                0
        );
    }

    @Async("indexingExecutor")
    public void asyncIndexing(UUID userId, Long repoId) {
        try {
            indexing(userId, repoId);
        } catch (Exception e) {
            log.error("Error occurred while indexing repository: {}", repoId, e);
            markFailedStataus(userId, repoId, e.getMessage());
        }
    }

    public void indexing(UUID userId, Long repoId) {
        Optional<Repository> repository =
                repositoryRepo.findByUserIdAndGithubRepoId(userId, repoId);
        if (repository.isPresent()) {
            Repository repo = repository.get();
            int filesProcessed = 0;
            int chunkCount = 0;
            int filesTotal = 0;
            deleteExistingVectors(repoId.toString());


            Map<String, Object> repoTree = githubApiClient.getRepoTree(repo.getOwner(), repo.getName(), repo.getDefaultBranch());
            if (repoTree == null || !repoTree.containsKey("tree")) {
                log.error("Error occurred while fetching repo tree for repository: {}", repoId);
                markFailedStataus(userId, repoId, "Unable to fetch repository tree");
                return;
            }

            log.atInfo().log("Processing repository tree for repository: {}", repoId);

            List<String> filePaths = lisValidFiles((List<Map<String, Object>>) repoTree.get("tree"));
            filesTotal = filePaths.size();
            log.atInfo().log("Found {} valid files in repository: {}", filesTotal, repoId);
            log.info(
                    "Starting indexing: repoId={}, githubRepo={}/{}, branch={}, eligibleFiles={}",
                    repoId,
                    repo.getOwner(),
                    repo.getName(),
                    repo.getDefaultBranch(),
                    filesTotal
            );

            List<Document> batch = new ArrayList<>();

            for (String filePath : filePaths) {

                try {

                    RepoFile file = githubApiClient.getFileContent(repo.getOwner(), repo.getName(), filePath);
                    if (file == null) {
                        log.error("Error occurred while fetching file content for file: {} in repository: {}", filePath, repoId);
                        continue;
                    }
                    log.atInfo().log("Processing file: {} in repository: {}", filePath, repoId);
                    String fileConentent = file.content();
                    if ("base64".equals(file.encoding())) {
                        String normalizedBase64 = file.content().replaceAll("\\s", "");
                        byte[] decodedBytes = Base64.getDecoder().decode(normalizedBase64);
                        fileConentent = new String(decodedBytes);
                    }
                    List<Document> chunks = fileChunking.chunkFile(filePath, fileConentent, repo.getId());

                    chunkCount += chunks.size();
                    batch.addAll(chunks);
                    if (batch.size() >= VECTOR_BATCH_SIZE) {
                        vectorStore.add(batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    log.error("Error occurred while processing file: {} in repository: {}", filePath, repoId, e);
                }
                filesProcessed++;
                if (filesProcessed % PROGRESS_EVERY_N_FILES == 0 || filesProcessed == filePaths.size()) {
                    updateIndexStatus(userId ,repoId, filesProcessed, filesTotal, chunkCount, IndexStatus.INDEXING, null);
                }
                rateLimiter.pause();
            }
            if (!batch.isEmpty()) {
                vectorStore.add(batch);
            }
            if(chunkCount==0){
                markFailedStataus(userId, repoId, "No valid files found for indexing");
                return;
            }
            
            markReadyStatus(userId, repoId);
        }
    }

    private void deleteExistingVectors(String id) {
        try {
            var filter = new FilterExpressionBuilder().eq(METADATA_REPO_ID, id).build();
            vectorStore.delete(filter);
        } catch (Exception ex) {
            log.warn("Could not delete existing vectors for repo {}: {}", id, ex.getMessage());
        }
    };

    public List<String> lisValidFiles(List<Map<String, Object>> trees) {
        return trees.stream()
                .filter(tree -> "blob".equals(tree.get("type")))
                .filter(tree -> {
                    String path = String.valueOf(tree.get("path"));
                    long size = tree.get("size") instanceof Number n ? n.longValue() : 0L;
                    return fileFilter.isEligible(path, size, maxFileBytes);
                })
                .map(tree -> String.valueOf(tree.get("path")))
                .toList();

    }

    public void markFailedStataus(UUID userId, Long repoId, String errorMessage) {
        Optional<Repository> repository =
                repositoryRepo.findByUserIdAndGithubRepoId(userId, repoId);
        if (repository.isPresent()) {
            Repository repo = repository.get();
            repo.setIndexStatus(IndexStatus.FAILED);
            repo.setErrorMessage(errorMessage);
            repositoryRepo.save(repo);
        }
    }

    public void markReadyStatus(UUID userId, Long repoId) {
        Optional<Repository> repository =
                repositoryRepo.findByUserIdAndGithubRepoId(userId, repoId);
        if (repository.isPresent()) {
            Repository repo = repository.get();
            repo.setIndexStatus(IndexStatus.READY);
            repo.setErrorMessage(null);
            repositoryRepo.save(repo);
        }
    }

    public void updateIndexStatus(UUID userId, Long repoId,
                                  int filesprocessed,
                                  int filestotal,
                                  int chunkcount,
                                  IndexStatus indexStatus,
                                  String errorMessage) {
        Optional<Repository> repository =
                repositoryRepo.findByUserIdAndGithubRepoId(userId, repoId);
        if (repository.isPresent()) {
            Repository repo = repository.get();
            repo.setIndexStatus(indexStatus);
            repo.setFilesProcessed(filesprocessed);
            repo.setFilesTotal(filestotal);
            repo.setChunkCount(chunkcount);
            repo.setErrorMessage(errorMessage);
            repositoryRepo.save(repo);
        }
    }
}
