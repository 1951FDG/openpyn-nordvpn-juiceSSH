package com.getsixtyfour.android;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Collections;

@SuppressWarnings("UtilityClass")
public final class GitHubCrashIssueHelper {

    private GitHubCrashIssueHelper() {
    }

    @NonNull
    public static GitHubCrashIssue getGitHubCrashIssue(@NonNull Context context) {
        GitHubCrashIssue.Builder builder = new GitHubCrashIssue.Builder();
        builder.setAssignees(Collections.singletonList(context.getString(R.string.github_issue_assignee)));
        builder.setDirty(Boolean.parseBoolean(context.getString(R.string.git_dirty)));
        builder.setDisabled(BuildConfig.DEBUG);
        builder.setId(context.getString(R.string.git_commit_id));
        builder.setLabels(Collections.singletonList(context.getString(R.string.github_issue_label)));
        builder.setUrl(context.getString(R.string.github_repo_url));
        builder.setVersion(context.getString(R.string.app_version));
        return builder.build();
    }
}
