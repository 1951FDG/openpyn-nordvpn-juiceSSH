package com.getsixtyfour.openvpnmgmt.android;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.getsixtyfour.android.GitHubCrashIssue;
import com.getsixtyfour.android.GitHubCrashIssueHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NonNls;

@SuppressWarnings("UtilityClass")
public final class IntentUtils {

    private IntentUtils() {
    }

    @Nullable
    static Intent getGitHubIntent(@NonNull Context context, @NonNull Throwable e) {
        if (e instanceof IOException) {
            @NonNls String message = Utils.getTopLevelCauseMessage(e);

            // ConnectException
            if ("connect failed: ENETUNREACH (Network is unreachable)".equals(message)) {
                return null;
            }

            // ConnectException
            if ("isConnected failed: ECONNREFUSED (Connection refused)".equals(message)) {
                return null;
            }

            // SocketTimeoutException
            if ("Read timed out".equals(message)) {
                return null;
            }

            // SocketException
            if ("Software caused connection abort".equals(message)) {
                return null;
            }
        }

        @NonNls List<String> packagesToLink = new ArrayList<>();
        packagesToLink.add("com.getsixtyfour.openvpnmgmt");

        @NonNls List<String> packagesToStack = new ArrayList<>();
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.android");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.api");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.cli");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.core");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.exceptions");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.implementation");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.listeners");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.model");
        packagesToStack.add("com.getsixtyfour.openvpnmgmt.utils");

        @NonNls String repositoryRoot = "libraries/openvpn-management/src/main/java";

        @NonNls Map<String, String> repositoryRoots = new HashMap<>();
        repositoryRoots.put("com.getsixtyfour.openvpnmgmt.android.OpenVpnService", "libraries/openvpn-service/src/main/java");

        GitHubCrashIssue issue = GitHubCrashIssueHelper.getGitHubCrashIssue(context);
        issue.setPackagesToLink(packagesToLink);
        issue.setPackagesToStack(packagesToStack);
        issue.setRepositoryRoot(repositoryRoot);
        issue.setRepositoryRootsByName(repositoryRoots);

        return issue.createIntent(e);
    }

    @Nullable
    static Intent getStopSelfIntent(@NonNull Context context, @NonNull Throwable e) {
        if (e instanceof ThreadDeath) {
            return null;
        }

        Intent intent = new Intent(context, OpenVpnService.class);
        intent.setAction(Constants.ACTION_EXIT);
        intent.putExtra(Constants.EXTRA_ACTION, Constants.TYPE_FINISH);

        return intent;
    }
}
