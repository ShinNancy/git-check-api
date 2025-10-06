package org.example.git.gitcheckapi.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GitInfo {
    private String localCommitId;
    private String remoteCommitId;
    private boolean isSynced;
    private String author;
    private String date;
    private String commitMessage;
}
