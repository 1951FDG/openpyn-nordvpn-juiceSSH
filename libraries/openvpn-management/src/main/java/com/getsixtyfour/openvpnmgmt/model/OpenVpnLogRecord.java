package com.getsixtyfour.openvpnmgmt.model;

import com.getsixtyfour.openvpnmgmt.core.LogLevel;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

public final class OpenVpnLogRecord {

    /**
     * time in UTC seconds
     */
    private final String mTime;

    /**
     * zero or more message flags in a single string
     */
    private final LogLevel mLevel;

    /**
     * message text
     */
    private final String mMessage;

    public OpenVpnLogRecord(@NotNull String unixTime, @NotNull LogLevel logLevel, @NotNull String message) {
        mTime = unixTime;
        mLevel = logLevel;
        mMessage = message;
    }

    public @NotNull String getTime() {
        return mTime;
    }

    public @NotNull LogLevel getLevel() {
        return mLevel;
    }

    public @NotNull String getMessage() {
        return mMessage;
    }

    public @NotNull Long getMillis() {
        return TimeUnit.MILLISECONDS.convert(Long.parseLong(mTime), TimeUnit.SECONDS);
    }
}
