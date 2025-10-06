package org.example.git.gitcheckapi.controller;

import org.example.git.gitcheckapi.model.GitInfo;
import org.example.git.gitcheckapi.service.GitSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GitController {
    private final GitSyncService gitSyncService;

    public GitController(GitSyncService gitSyncService) {
        this.gitSyncService = gitSyncService;
    }

    @GetMapping("/api/git/status")
    public GitInfo getGitStatus() {
        return gitSyncService.checkSyncStatus();
    }
}
