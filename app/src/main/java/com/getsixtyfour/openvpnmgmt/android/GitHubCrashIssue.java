package com.getsixtyfour.openvpnmgmt.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NonNls;

/**
 * @see <a href="https://docs.github.com/en/github/managing-your-work-on-github/about-automation-for-issues-and-pull-requests-with-query-parameters">Issues - GitHub Docs</a>
 * <br>
 * @see <a href="https://github.com/BugSplat-Git/Test/issues">Issues - BugSplat-Git/Test</a>
 * <br>
 * @see <a href="https://github.com/alexknvl/tracehash">TraceHash</a>
 * <br>
 * @see <a href="https://github.com/lucaswerkmeister/linkStackTrace">linkStackTrace</a>
 * <br>
 * @see <a href="https://github.com/talal830/stackifier">Stackifier</a>
 */

public class GitHubCrashIssue {

    @NonNls
    private static final Collection<String> PACKAGE_LIST = new ArrayList<>();

    private static final Pattern COMPILE = Pattern.compile(StringUtils.DOT, Pattern.LITERAL);

    @NonNls
    private static final String GENERATED_LAMBDA_CLASS_SUFFIX = "$$Lambda$";

    @NonNls
    private static final String LAMBDA_METHOD_PREFIX = "lambda$";

    @SuppressWarnings("WeakerAccess")
    public static final String UTF_8 = "UTF-8";

    /**
     * URL line feed
     */
    private static final String URL_LF = "%0A";

    private static final int INDEX_NOT_FOUND = -1;

    private static final int INITIAL_SIZE = 256;

    private static final int MAX_STACK_TRACE_SIZE = 20;

    static {
        // Java Packages
        PACKAGE_LIST.add("java.beans");
        PACKAGE_LIST.add("java.io");
        PACKAGE_LIST.add("java.lang");
        PACKAGE_LIST.add("java.math");
        PACKAGE_LIST.add("java.net");
        PACKAGE_LIST.add("java.nio");
        PACKAGE_LIST.add("java.security");
        PACKAGE_LIST.add("java.sql");
        PACKAGE_LIST.add("java.text");
        PACKAGE_LIST.add("java.time");
        PACKAGE_LIST.add("java.util");
    }

    @NonNls
    private final String mGithubRepoUrl;

    @NonNls
    private final String mGithubIssuesUrl;

    @NonNls
    private final String mGithubCommitId;

    @NonNls
    private final String mGithubAssignees;

    @NonNls
    private final String mGithubLabels;

    @NonNls
    private final String mGithubVersionName;

    private final boolean mGithubDirty;

    private final boolean mGithubEnabled;

    public GitHubCrashIssue(@NonNull String url, @NonNull String id, @NonNull String assignees, @NonNull String labels, @NonNull String version,
                            boolean dirty, boolean enabled) {
        if (id.isEmpty()) {
            throw new IllegalArgumentException();
        }
        if (url.isEmpty()) {
            throw new IllegalArgumentException();
        }
        mGithubRepoUrl = url;
        mGithubIssuesUrl = url + "/issues/new";
        mGithubCommitId = id;
        mGithubAssignees = assignees;
        mGithubLabels = labels;
        mGithubVersionName = version;
        mGithubDirty = dirty;
        mGithubEnabled = enabled;
    }

    /**
     * The user-visible version string.
     */
    @NonNull
    private static String getFirmwareVersion() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ? Build.VERSION.RELEASE_OR_CODENAME : Build.VERSION.RELEASE;
    }

    /**
     * Gets a user-friendly name for a lambda function.
     */
    @SuppressWarnings({ "ImplicitNumericConversion", "MagicCharacter" })
    @NonNls
    @NonNull
    private static String getLambdaName(@NonNull String fullFunction) {
        // Lambdas contain a name that's based on an index + timestamp at runtime and changes build-to-build.
        // This makes comparing two builds very difficult when a lambda is in the stack
        //
        // Full function has one of the following formats:
        //   package-$$Lambda$class$randomId
        //   package-$$Lambda$randomId
        //
        // We just want just package.class$Lambda (or package$Lambda) respectively
        int endPkgIdx = fullFunction.indexOf("-$$");
        if (endPkgIdx == INDEX_NOT_FOUND) {
            return fullFunction;
        }
        // firstDollarIdx could be either beginning of class or beginning of the random id
        int firstDollarIdx = fullFunction.indexOf('$', endPkgIdx + 3);
        if (firstDollarIdx == INDEX_NOT_FOUND) {
            return fullFunction;
        }
        int endClassIdx = fullFunction.indexOf('$', firstDollarIdx + 1);
        if (endClassIdx == INDEX_NOT_FOUND) {
            // Just package
            return fullFunction.substring(0, endPkgIdx - 1);
        }
        // Package + class
        return fullFunction.substring(0, endPkgIdx) + fullFunction.substring(firstDollarIdx + 1, endClassIdx);
    }

    @Nullable
    private static String getTopLevelCauseMessage(Throwable t) {
        Throwable topLevelCause = t;
        while (topLevelCause.getCause() != null) {
            topLevelCause = topLevelCause.getCause();
        }
        return topLevelCause.getMessage();
    }

    @SuppressWarnings({ "OverlyLongMethod", "HardCodedStringLiteral", "MagicCharacter", "MagicNumber", "ImplicitNumericConversion" })
    @NonNull
    private static String hash(Class<? extends Throwable> aClass, StackTraceElement[] stackTrace) {
        String result;
        {
            StringBuilder builder = new StringBuilder(INITIAL_SIZE);
            builder.append(aClass.getCanonicalName());
            int total = 0;
            int length = stackTrace.length;
            for (int i = 0; (i < length) && (total < MAX_STACK_TRACE_SIZE); i++) {
                String className = stackTrace[i].getClassName();
                String methodName = plainMethodName(stackTrace[i].getMethodName());
                if (!isLambda(className)) {
                    builder.append('|');
                    builder.append(className);
                    builder.append('/');
                    builder.append(methodName);
                    total += 1;
                }
            }
            result = builder.toString();
        }
        try {
            StringBuilder builder = new StringBuilder(INITIAL_SIZE);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(result.getBytes(StandardCharsets.UTF_8));
            byte[] bytes = md.digest();
            for (byte b : bytes) {
                builder.append(hexChar((b >> 4) & 0xF));
                builder.append(hexChar(b & 0xF));
            }
            result = builder.toString();
        } catch (NoSuchAlgorithmException ignored) {
        }
        {
            StringBuilder builder = new StringBuilder(INITIAL_SIZE);
            String throwableName = aClass.getSimpleName();
            for (int i = 0; i < throwableName.length(); i++) {
                char ch = throwableName.charAt(i);
                if (Character.isUpperCase(ch)) {
                    builder.append(ch);
                }
            }
            builder.append('-');
            builder.append(result);
            result = builder.toString();
        }
        return result;
    }

    @SuppressWarnings({ "ImplicitNumericConversion", "MagicCharacter" })
    private static char hexChar(int x) {
        return (char) ((x <= 9) ? (x + '0') : (('a' + x) - 10));
    }

    private static boolean isLambda(String className) {
        return className.contains(GENERATED_LAMBDA_CLASS_SUFFIX);
    }

    @SuppressWarnings({ "ImplicitNumericConversion", "MagicCharacter" })
    @NonNull
    private static String plainMethodName(String methodName) {
        int startMethodIdx = methodName.indexOf(LAMBDA_METHOD_PREFIX);
        if (startMethodIdx == INDEX_NOT_FOUND) {
            return methodName;
        }
        int endMethodIdx = methodName.indexOf('$', LAMBDA_METHOD_PREFIX.length());
        if (endMethodIdx == INDEX_NOT_FOUND) {
            return methodName;
        }
        return methodName.substring(LAMBDA_METHOD_PREFIX.length(), endMethodIdx);
    }

    @NonNull
    public String createNewGithubIssue(@NonNull Throwable throwable) {
        if (mGithubDirty) {
            throw new IllegalStateException("You forget to commit code before building final release build");
        }
        return createIssue(throwable);
    }

    /**
     * Encodes characters in the given string as '%'-escaped octets
     * using the UTF-8 scheme. Leaves letters ("A-Z", "a-z"), numbers
     * ("0-9"), and unreserved characters ("_-.*") intact. Encodes
     * all other characters.
     *
     * @param s string to encode
     * @return an encoded version of s suitable for use as a query parameter,
     * or null if s is null
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public String encode(@Nullable String s) {
        if (TextUtils.isEmpty(s)) {
            return (s == null) ? "" : s;
        }
        try {
            return URLEncoder.encode(s, UTF_8);
        } catch (UnsupportedEncodingException ignored) {
            // The system should always have the platform default
        }
        return "";
    }

    @NonNull
    public List<String> getPackagesToLink() {
        @NonNls List<String> list = new ArrayList<>();
        list.add("com.getsixtyfour.openvpnmgmt");
        return list;
    }

    @NonNull
    public List<String> getPackagesToStack() {
        @NonNls List<String> list = new ArrayList<>();
        list.add("com.getsixtyfour.openvpnmgmt.android");
        list.add("com.getsixtyfour.openvpnmgmt.api");
        list.add("com.getsixtyfour.openvpnmgmt.cli");
        list.add("com.getsixtyfour.openvpnmgmt.core");
        list.add("com.getsixtyfour.openvpnmgmt.exceptions");
        list.add("com.getsixtyfour.openvpnmgmt.implementation");
        list.add("com.getsixtyfour.openvpnmgmt.listeners");
        list.add("com.getsixtyfour.openvpnmgmt.model");
        list.add("com.getsixtyfour.openvpnmgmt.net");
        list.add("com.getsixtyfour.openvpnmgmt.utils");
        return list;
    }

    @NonNull
    public Uri parse(@NonNull Throwable throwable) {
        return Uri.parse(createNewGithubIssue(throwable));
    }

    @Nullable
    public Intent createIntent(@NonNull Throwable throwable) {
        return (mGithubEnabled && !mGithubDirty) ? new Intent(Intent.ACTION_VIEW, parse(throwable)) : null;
    }

    @SuppressWarnings("OverlyLongMethod")
    @NonNull
    private String createIssue(@NonNull Throwable throwable) {
        Class<? extends Throwable> aClass = throwable.getClass();
        StackTraceElement[] trace = throwable.getStackTrace();
        String hash = hash(aClass, trace);
        String stackTrace = stack(trace);
        String causeMessage = getTopLevelCauseMessage(throwable);
        String firmwareVersion = getFirmwareVersion();
        String simpleName = aClass.getSimpleName();
        String message = throwable.getMessage();
        //noinspection StringBufferReplaceableByString
        @NonNls StringBuilder result = new StringBuilder(INITIAL_SIZE);
        result.append(mGithubIssuesUrl);
        result.append("?body=");
        result.append(encode(hash));
        //
        result.append(URL_LF);
        result.append(URL_LF);
        result.append("**Version%3A**");
        result.append(URL_LF);
        result.append("%60");
        result.append("Android");
        result.append(" ");
        result.append(encode(firmwareVersion));
        result.append("%60");
        result.append(URL_LF);
        result.append("%60");
        result.append("App");
        result.append(" ");
        result.append(encode(mGithubVersionName));
        result.append("%60");
        result.append(URL_LF);
        result.append(mGithubCommitId);
        //
        result.append(URL_LF);
        result.append(URL_LF);
        result.append("**Fatal Exception%3A**");
        result.append(URL_LF);
        result.append(encode(simpleName));
        //
        result.append(URL_LF);
        result.append(URL_LF);
        result.append("**Message%3A**");
        result.append(URL_LF);
        result.append(encode(message));
        //
        result.append(URL_LF);
        result.append(URL_LF);
        result.append("**Stacktrace%3A**");
        result.append(URL_LF);
        result.append(encode(stackTrace));
        //
        result.append("&title=");
        result.append(encode(causeMessage));
        result.append("&labels=");
        result.append(encode(mGithubLabels));
        result.append("&assignees=");
        result.append(encode(mGithubAssignees));
        return result.toString();
    }

    @SuppressWarnings({ "OverlyComplexMethod", "OverlyLongMethod", "IfStatementWithTooManyBranches" })
    @NonNull
    private String stack(StackTraceElement[] trace) {
        @NonNls StringBuilder result = new StringBuilder(INITIAL_SIZE);
        int length = trace.length;
        List<String> strList = new ArrayList<>(length);
        List<List<StackTraceElement>> steList = new ArrayList<>(length);
        String group = StringUtils.EMPTY;
        List<StackTraceElement> elementList = null;
        int total = 0;
        for (int i = 0; (i < length) && (total < MAX_STACK_TRACE_SIZE); i++) {
            StackTraceElement traceElement = trace[i];
            String className = getLambdaName(traceElement.getClassName());
            for (String prefix : getPackagesToStack()) {
                if (className.startsWith(prefix)) {
                    className = prefix;
                    break;
                }
            }
            for (String prefix : PACKAGE_LIST) {
                if (className.startsWith(prefix)) {
                    className = prefix;
                    break;
                }
            }
            if (group.isEmpty()) {
                group = className;
                elementList = new ArrayList<>(length);
                elementList.add(traceElement);
            } else if (!group.startsWith(className)) {
                strList.add(group);
                steList.add(elementList);
                group = className;
                elementList = new ArrayList<>(length);
                elementList.add(traceElement);
            } else {
                group = className;
                elementList.add(traceElement);
            }
            total += 1;
        }
        strList.add(group);
        steList.add(elementList);
        for (int i = 0; i < strList.size(); i++) {
            List<StackTraceElement> list = steList.get(i);
            String packageName = strList.get(i);
            result.append(StringUtils.HASH);
            result.append(StringUtils.HASH);
            result.append(StringUtils.HASH);
            result.append(StringUtils.HASH);
            result.append(StringUtils.HASH);
            result.append(StringUtils.HASH);
            result.append(StringUtils.SPACE);
            result.append(StringUtils.BACKTICK);
            result.append(StringUtils.LEFT_PARENTHESIS);
            result.append(i + 1);
            result.append(StringUtils.RIGHT_PARENTHESIS);
            result.append(StringUtils.SPACE);
            result.append(packageName);
            result.append(StringUtils.BACKTICK);
            result.append(StringUtils.LF);
            for (int j = 0; j < list.size(); j++) {
                StackTraceElement traceElement = list.get(j);
                String className = traceElement.getClassName();
                String methodName = traceElement.getMethodName();
                //TODO: filename vs classname, java extension or not?
                String fileName = traceElement.getFileName();
                Matcher matcher = COMPILE.matcher(className);
                boolean isLambda = isLambda(className);
                boolean isFile = fileName != null;
                boolean isPackage = false;
                for (String prefix : getPackagesToLink()) {
                    if (className.startsWith(prefix)) {
                        isPackage = true;
                        break;
                    }
                }
                if (isPackage && isFile) {
                    result.append(mGithubRepoUrl);
                    result.append("/blob/");
                    result.append(mGithubCommitId);
                    result.append(StringUtils.PATH_SEPARATOR);
                    result.append("app/src/main/java");
                    result.append(StringUtils.PATH_SEPARATOR);
                    result.append(matcher.replaceAll(StringUtils.PATH_SEPARATOR));
                    result.append(StringUtils.DOT);
                    result.append("java");
                    result.append(StringUtils.HASH);
                    result.append("L");
                    result.append(traceElement.getLineNumber());
                    result.append(StringUtils.LF);
                } else if (!isFile && isLambda && isPackage) {
                    // No-op
                } else if (!isFile && isLambda) {
                    className = getLambdaName(className) + GENERATED_LAMBDA_CLASS_SUFFIX;
                    result.append(className).append(methodName);
                    result.append(StringUtils.LF);
                } else if (!isFile) {
                    result.append(className).append(StringUtils.DOT).append(methodName);
                    // result.append(StringUtils.LEFT_PARENTHESIS).append("Unknown Source").append(StringUtils.RIGHT_PARENTHESIS);
                    result.append(StringUtils.LF);
                } else {
                    result.append(className).append(StringUtils.DOT).append(methodName);
                    // result.append(StringUtils.LEFT_PARENTHESIS).append(fileName).append(StringUtils.RIGHT_PARENTHESIS);
                    result.append(StringUtils.LF);
                }
            }
            result.append(StringUtils.LF);
        }
        return result.toString();
    }

    @SuppressWarnings({ "ReturnOfThis", "PublicInnerClass" })
    public static class Builder {

        @Nullable
        private String mUrl = null;

        @Nullable
        private String mId = null;

        private String mAssignees = "";

        private String mLabels = "";

        private String mVersion = "";

        private boolean mDirty;

        private boolean mDisabled;

        @NonNull
        public GitHubCrashIssue build() {
            return new GitHubCrashIssue(Objects.requireNonNull(mUrl), Objects.requireNonNull(mId), mAssignees, mLabels, mVersion, mDirty, !mDisabled);
        }

        @NonNull
        public Builder setAssignees(@NonNull Collection<String> assignees) {
            SortedSet<String> set = Collections.checkedSortedSet(new TreeSet<>(assignees), String.class);
            mAssignees = TextUtils.join(",", set);
            return this;
        }

        @NonNull
        public Builder setDirty(boolean dirty) {
            mDirty = dirty;
            return this;
        }

        @NonNull
        public Builder setDisabled(boolean disabled) {
            mDisabled = disabled;
            return this;
        }

        @NonNull
        public Builder setId(@NonNull String id) {
            mId = id;
            return this;
        }

        @NonNull
        public Builder setLabels(@NonNull Collection<String> labels) {
            SortedSet<String> set = Collections.checkedSortedSet(new TreeSet<>(labels), String.class);
            mLabels = TextUtils.join(",", set);
            return this;
        }

        @NonNull
        public Builder setUrl(@NonNull String url) {
            mUrl = url;
            return this;
        }

        @NonNull
        public Builder setVersion(@NonNull String version) {
            mVersion = version;
            return this;
        }
    }

    @SuppressWarnings("FieldNamingConvention")
    private static final class StringUtils {

        private static final String BACKTICK = "`";

        private static final String DOT = ".";

        private static final String EMPTY = "";

        private static final String HASH = "#";

        private static final String LEFT_PARENTHESIS = "(";

        private static final String LF = "\n";

        private static final String PATH_SEPARATOR = "/";

        private static final String RIGHT_PARENTHESIS = ")";

        private static final String SPACE = " ";
    }
}
