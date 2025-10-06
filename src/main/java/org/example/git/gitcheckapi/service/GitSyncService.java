package org.example.git.gitcheckapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.example.git.gitcheckapi.model.GitInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
            // --- Lấy commit local
            FileRepositoryBuilder builder = new FileRepositoryBuilder();
            Repository repository = builder.setGitDir(new File(".git"))
                    .readEnvironment()
                    .findGitDir()
                    .build();

            String localCommitId = null;
            String author = null;
            String date = null;
            String message = null;

            try (Git git = new Git(repository)) {
                Iterable<RevCommit> commits = git.log().setMaxCount(1).call();
                for (RevCommit commit : commits) {
                    localCommitId = commit.getId().getName();
                    author = commit.getAuthorIdent().getName();
                    Instant commitDate = Instant.ofEpochSecond(commit.getCommitTime());
                    date = DateTimeFormatter.ISO_INSTANT.format(commitDate);
                    message = commit.getFullMessage();
                }
            }

            // --- Lấy commit remote (GitHub)
            String githubApiUrl = String.format(
                    "https://api.github.com/repos/%s/%s/commits/%s",
                    repoOwner, repoName, branch
            );

            URL url = new URL(githubApiUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/vnd.github.v3+json");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = mapper.readValue(content.toString(), new TypeReference<>() {});
            String remoteCommitId = response != null ? (String) response.get("sha") : null;

            boolean synced = localCommitId != null && localCommitId.equals(remoteCommitId);

            return new GitInfo(localCommitId, remoteCommitId, synced, author, date, message);

        } catch (Exception e) {
            e.printStackTrace();
            return new GitInfo("N/A", "N/A", false, "Error", "N/A",
                    "❌ Lỗi khi kiểm tra sync: " + e.getMessage());
        }
    }
}
