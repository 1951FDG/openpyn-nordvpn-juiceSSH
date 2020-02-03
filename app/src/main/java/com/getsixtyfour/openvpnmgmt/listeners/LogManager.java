package com.getsixtyfour.openvpnmgmt.listeners;

import com.getsixtyfour.openvpnmgmt.core.LogLevel;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author 1951FDG
 */

public class LogManager {

    private final CopyOnWriteArraySet<LogListener> mListeners;

    private Log mLog;

    public LogManager() {
        mListeners = new CopyOnWriteArraySet<>();
    }

    public boolean addListener(@NotNull LogListener listener) {
        return mListeners.add(listener);
    }

    public boolean removeListener(@NotNull LogListener listener) {
        return mListeners.remove(listener);
    }

    public void setLog(@NotNull Log log) {
        mLog = log;
        notifyListeners();
    }

    private void notifyListeners() {
        for (LogListener listener : mListeners) {
            listener.onLog(mLog);
        }
    }

    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    @FunctionalInterface
    public interface LogListener {

        void onLog(@NotNull Log log);
    }

    @SuppressWarnings("PublicInnerClass")
    public static class Log {

        private final String mDate;

        private final LogLevel mLevel;

        private final String mMessage;

        public Log(@NotNull String date, @NotNull LogLevel level, @NotNull String message) {
            mDate = date;
            mLevel = level;
            mMessage = message;
        }

        @NotNull
        public String getDate() {
            return mDate;
        }

        @NotNull
        public LogLevel getLevel() {
            return mLevel;
        }

        @NotNull
        public String getMessage() {
            return mMessage;
        }
    }
}
