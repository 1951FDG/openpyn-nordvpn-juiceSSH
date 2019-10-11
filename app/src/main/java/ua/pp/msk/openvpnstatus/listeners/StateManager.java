package ua.pp.msk.openvpnstatus.listeners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author 1951FDG
 */

public class StateManager {

    @SuppressWarnings("PublicInnerClass")
    public static class State {

        private final String mDate;

        private final String mLocalAddress;

        private final String mMessage;

        private final String mRemoteAddress;

        private final String mRemotePort;

        private final String mName;

        public State(@NotNull String date, @NotNull String name, @Nullable String message, @Nullable String localAddress,
                     @Nullable String remoteAddress, @Nullable String remotePort) {
            mDate = date;
            mName = name;
            mMessage = message;
            mLocalAddress = localAddress;
            mRemoteAddress = remoteAddress;
            mRemotePort = remotePort;
        }

        @NotNull
        public String getDate() {
            return mDate;
        }

        @Nullable
        public String getLocalAddress() {
            return mLocalAddress;
        }

        @Nullable
        public String getMessage() {
            return mMessage;
        }

        @Nullable
        public String getRemoteAddress() {
            return mRemoteAddress;
        }

        @Nullable
        public String getRemotePort() {
            return mRemotePort;
        }

        @NotNull
        public String getName() {
            return mName;
        }
    }

    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    @FunctionalInterface
    public interface StateListener {

        void onStateChanged(@NotNull State state);
    }

    private final List<StateListener> stateListener;

    private State mState;

    public StateManager() {
        stateListener = new CopyOnWriteArrayList<>();
    }

    public void addListener(@NotNull StateListener listener) {
        if (!stateListener.contains(listener)) {
            stateListener.add(listener);
        }
    }

    public void removeListener(@NotNull StateListener listener) {
        stateListener.remove(listener);
    }

    public void setState(@NotNull State state) {
        mState = state;
        notifyListeners();
    }

    private void notifyListeners() {
        for (StateListener listener : stateListener) {
            listener.onStateChanged(mState);
        }
    }
}
