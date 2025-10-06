package org.example.git.gitcheckapi.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.example.git.gitcheckapi.model.GitInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.util.Map;

@Service
public class GitSyncService {

    @Value("${git.repo.owner}")
    private String repoOwner;

    @Value("${git.repo.name}")
    private String repoName;

    @Value("${git.repo.branch:main}")
    private String branch;

    public GitInfo checkSyncStatus() {
        try {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(new File(".git"))
                    .readEnvironment().findGitDir().build();

            String localCommitId = null;
            String author = null;
            String message = null;
            String date = null;

            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().setMaxCount(1).call();
                for (RevCommit commit : commits) {
                    localCommitId = commit.getId().getName();
                    author = commit.getAuthorIdent().getName();
                    message = commit.getFullMessage();
                    date = commit.getAuthorIdent().getWhen().toString();
                }
            }

            String githubApiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/commits/%s",
                    repoOwner, repoName, branch
            );

            WebClient webClient = WebClient.create();
            Map<String, Object> response = webClient.get()
                    .uri(githubApiUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String remoteCommitId = (String) response.get("sha");
            boolean synced = localCommitId != null && localCommitId.equals(remoteCommitId);

            return new GitInfo(localCommitId, remoteCommitId, synced, author, date, message);

        } catch (Exception e) {
            return new GitInfo("N/A", "N/A", false, "Error", "N/A",
                    "❌ Lỗi khi kiểm tra sync: " + e.getMessage());
        }
    }
}
