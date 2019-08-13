package ua.pp.msk.openvpnstatus.listeners;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ua.pp.msk.openvpnstatus.core.LogLevel;

/**
 * @author 1951FDG
 */

public class LogManager {

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

    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    @FunctionalInterface
    public interface LogListener {

        void onLog(@NotNull Log log);
    }

    private final List<LogListener> logListener;

    private Log mLog;

    public LogManager() {
        logListener = new CopyOnWriteArrayList<>();
    }

    public void addListener(@NotNull LogListener listener) {
        if (!logListener.contains(listener)) {
            logListener.add(listener);
        }
    }

    public void removeListener(@NotNull LogListener listener) {
        logListener.remove(listener);
    }

    public void setLog(@NotNull Log log) {
        mLog = log;
        notifyListeners();
    }

    private void notifyListeners() {
        for (LogListener listener : logListener) {
            listener.onLog(mLog);
        }
    }
}
