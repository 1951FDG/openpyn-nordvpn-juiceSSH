package com.getsixtyfour.openvpnmgmt.model;

import com.getsixtyfour.openvpnmgmt.core.LogLevel;

import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

public class OpenVpnLogRecord {

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

    @NotNull
    public String getTime() {
        return mTime;
    }

    @NotNull
    public LogLevel getLevel() {
        return mLevel;
    }

    @NotNull
    public String getMessage() {
        return mMessage;
    }

    @NotNull
    public Long getMillis() {
        return TimeUnit.MILLISECONDS.convert(Long.parseLong(mTime), TimeUnit.SECONDS);
    }
}
