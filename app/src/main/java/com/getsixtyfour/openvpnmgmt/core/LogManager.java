package com.getsixtyfour.openvpnmgmt.core;

import com.getsixtyfour.openvpnmgmt.listeners.OnRecordChangedListener;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

/**
 * @author 1951FDG
 */

public class LogManager {

    private final CopyOnWriteArraySet<OnRecordChangedListener> mListeners;

    private OpenVpnLogRecord mRecord;

    public LogManager() {
        mListeners = new CopyOnWriteArraySet<>();
    }

    public boolean addListener(@NotNull OnRecordChangedListener listener) {
        return mListeners.add(listener);
    }

    public boolean removeListener(@NotNull OnRecordChangedListener listener) {
        return mListeners.remove(listener);
    }

    public void setRecord(@NotNull OpenVpnLogRecord record) {
        mRecord = record;
        notifyListeners();
    }

    private void notifyListeners() {
        for (OnRecordChangedListener listener : mListeners) {
            listener.onRecordChanged(mRecord);
        }
    }

    @SuppressWarnings("PublicInnerClass")
    public static class OpenVpnLogRecord {

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
}
