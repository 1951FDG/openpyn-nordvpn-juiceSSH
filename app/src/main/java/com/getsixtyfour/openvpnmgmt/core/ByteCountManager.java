package com.getsixtyfour.openvpnmgmt.core;

import com.getsixtyfour.openvpnmgmt.core.TrafficHistory.LastDiff;
import com.getsixtyfour.openvpnmgmt.core.TrafficHistory.TrafficDataPoint;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author 1951FDG
 */

public class ByteCountManager {

    private final CopyOnWriteArraySet<OnByteCountChangedListener> mListeners;

    private final TrafficHistory mTrafficHistory = new TrafficHistory();

    private TrafficDataPoint mTrafficDataPoint;

    public ByteCountManager() {
        mListeners = new CopyOnWriteArraySet<>();
    }

    public boolean addListener(@NotNull OnByteCountChangedListener listener) {
        return mListeners.add(listener);
    }

    public boolean removeListener(@NotNull OnByteCountChangedListener listener) {
        return mListeners.remove(listener);
    }

    public void setTrafficDataPoint(@NotNull TrafficDataPoint trafficDataPoint) {
        mTrafficDataPoint = trafficDataPoint;
        notifyListeners();
    }

    private void notifyListeners() {
        long in = mTrafficDataPoint.mInBytes;
        long out = mTrafficDataPoint.mOutBytes;
        LastDiff diff = mTrafficHistory.add(mTrafficDataPoint);
        for (OnByteCountChangedListener listener : mListeners) {
            listener.onByteCountChanged(in, out, diff.getDiffIn(), diff.getDiffOut());
        }
    }
}
