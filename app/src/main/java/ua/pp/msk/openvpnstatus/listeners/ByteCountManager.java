package ua.pp.msk.openvpnstatus.listeners;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import ua.pp.msk.openvpnstatus.core.TrafficHistory;

/**
 * @author 1951FDG
 */

public class ByteCountManager {

    public static class ByteCount {

        private final Long mIn;

        private final Long mOut;

        public ByteCount(@NotNull Long in, @NotNull Long out) {
            mIn = in;
            mOut = out;
        }

        @NotNull
        public Long getIn() {
            return mIn;
        }

        @NotNull
        public Long getOut() {
            return mOut;
        }
    }

    @SuppressWarnings("WeakerAccess")
    @FunctionalInterface
    public interface ByteCountListener {

        void onByteCountChanged(long in, long out, long diffIn, long diffOut);
    }

    private final List<ByteCountListener> mByteCountListeners;

    private final TrafficHistory trafficHistory = new TrafficHistory();

    private ByteCount mByteCount;

    public ByteCountManager(@NotNull List<ByteCountListener> list) {
        mByteCountListeners = list;
    }

    public void addListener(@NotNull ByteCountListener listener) {
        if (!mByteCountListeners.contains(listener)) {
            mByteCountListeners.add(listener);
            // TrafficHistory.LastDiff diff = trafficHistory.getLastDiff(null);
            // listener.onByteCountChanged(diff.getIn(), diff.getOut(), diff.getDiffIn(), diff.getDiffOut());
        }
    }

    public void removeListener(@NotNull ByteCountListener listener) {
        mByteCountListeners.remove(listener);
    }

    public void setByteCount(@NotNull ByteCount byteCount) {
        mByteCount = byteCount;
        notifyListeners();
    }

    private void notifyListeners() {
        Long in = mByteCount.getIn();
        Long out = mByteCount.getOut();
        TrafficHistory.LastDiff diff = trafficHistory.add(in, out);
        for (ByteCountListener listener : mByteCountListeners) {
            listener.onByteCountChanged(in, out, diff.getDiffIn(), diff.getDiffOut());
        }
    }
}