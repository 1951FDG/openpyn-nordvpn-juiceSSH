package com.getsixtyfour.openvpnmgmt.listeners;

import com.getsixtyfour.openvpnmgmt.core.TrafficHistory;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author 1951FDG
 */

public class ByteCountManager {

    private final CopyOnWriteArraySet<ByteCountListener> mListeners;

    private final TrafficHistory mTrafficHistory = new TrafficHistory();

    private ByteCount mByteCount;

    public ByteCountManager() {
        mListeners = new CopyOnWriteArraySet<>();
    }

    public boolean addListener(@NotNull ByteCountListener listener) {
        return mListeners.add(listener);
    }

    public boolean removeListener(@NotNull ByteCountListener listener) {
        return mListeners.remove(listener);
    }

    public void setByteCount(@NotNull ByteCount byteCount) {
        mByteCount = byteCount;
        notifyListeners();
    }

    private void notifyListeners() {
        long in = mByteCount.getInBytes();
        long out = mByteCount.getOutBytes();
        TrafficHistory.LastDiff diff = mTrafficHistory.add(in, out);
        for (ByteCountListener listener : mListeners) {
            listener.onByteCountChanged(in, out, diff.getDiffIn(), diff.getDiffOut());
        }
    }

    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    @FunctionalInterface
    public interface ByteCountListener {

        void onByteCountChanged(long in, long out, long diffIn, long diffOut);
    }

    @SuppressWarnings("PublicInnerClass")
    public static class ByteCount {

        private final Long mInBytes;

        private final Long mOutBytes;

        public ByteCount(@NotNull Long inBytes, @NotNull Long outBytes) {
            mInBytes = inBytes;
            mOutBytes = outBytes;
        }

        @NotNull
        public Long getInBytes() {
            return mInBytes;
        }

        @NotNull
        public Long getOutBytes() {
            return mOutBytes;
        }
    }
}
