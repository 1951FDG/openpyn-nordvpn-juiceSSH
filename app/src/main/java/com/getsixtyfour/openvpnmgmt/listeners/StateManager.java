package com.getsixtyfour.openvpnmgmt.listeners;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author 1951FDG
 */

public class StateManager {

    private final CopyOnWriteArraySet<StateListener> mListeners;

    private State mState;

    public StateManager() {
        mListeners = new CopyOnWriteArraySet<>();
    }

    public boolean addListener(@NotNull StateListener listener) {
        return mListeners.add(listener);
    }

    public boolean removeListener(@NotNull StateListener listener) {
        return mListeners.remove(listener);
    }

    public void setState(@NotNull State state) {
        mState = state;
        notifyListeners();
    }

    private void notifyListeners() {
        for (StateListener listener : mListeners) {
            listener.onStateChanged(mState);
        }
    }

    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    @FunctionalInterface
    public interface StateListener {

        void onStateChanged(@NotNull State state);
    }

    @SuppressWarnings("PublicInnerClass")
    public static class State {

        private final String mDate;

        private final String mLocalAddress;

        private final String mMessage;

        private final String mName;

        private final String mRemoteAddress;

        private final String mRemotePort;

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

        @NotNull
        public String getName() {
            return mName;
        }

        @Nullable
        public String getRemoteAddress() {
            return mRemoteAddress;
        }

        @Nullable
        public String getRemotePort() {
            return mRemotePort;
        }
    }
}
