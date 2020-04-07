package com.getsixtyfour.openvpnmgmt.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public class TrafficHistory {

    @SuppressWarnings("WeakerAccess")
    public static final long PERIODS_TO_KEEP = 5L;

    @SuppressWarnings("WeakerAccess")
    public static final long TIME_PERIOD_HOURS = 3600L * 1000L;

    @SuppressWarnings("WeakerAccess")
    public static final long TIME_PERIOD_MINUTES = 60L * 1000L;

    private final LinkedList<TrafficDataPoint> mHours = new LinkedList<>();

    private final LinkedList<TrafficDataPoint> mMinutes = new LinkedList<>();

    private final LinkedList<TrafficDataPoint> mSeconds = new LinkedList<>();

    private TrafficDataPoint mLastMinuteUsedForHours;

    private TrafficDataPoint mLastSecondUsedForMinute;

    @NotNull
    public static LinkedList<TrafficDataPoint> getDummyList() {
        LinkedList<TrafficDataPoint> list = new LinkedList<>();
        list.add(new TrafficDataPoint(0L, 0L, System.currentTimeMillis()));
        return list;
    }

    @NotNull
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public LastDiff add(long in, long out) {
        TrafficDataPoint tdp = new TrafficDataPoint(in, out, System.currentTimeMillis());
        return add(tdp);
    }

    @NotNull
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public LastDiff add(long in, long out, long timestamp) {
        TrafficDataPoint tdp = new TrafficDataPoint(in, out, timestamp);
        return add(tdp);
    }

    @NotNull
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public LastDiff add(@NotNull TrafficDataPoint tdp) {
        TrafficDataPoint lastTdp = mSeconds.isEmpty() ? new TrafficDataPoint(0L, 0L, tdp.mTimestamp) : mSeconds.getLast();
        LastDiff diff = new LastDiff(lastTdp, tdp);
        mSeconds.add(tdp);
        if (mLastSecondUsedForMinute == null) {
            mLastSecondUsedForMinute = new TrafficDataPoint(0L, 0L, 0L);
            mLastMinuteUsedForHours = new TrafficDataPoint(0L, 0L, 0L);
        }
        removeAndAverage(tdp, true);
        return diff;
    }

    @NotNull
    public List<TrafficDataPoint> getHours() {
        return Collections.unmodifiableList(mHours);
    }

    @NotNull
    public List<TrafficDataPoint> getMinutes() {
        return Collections.unmodifiableList(mMinutes);
    }

    @NotNull
    public List<TrafficDataPoint> getSeconds() {
        return Collections.unmodifiableList(mSeconds);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    private void removeAndAverage(TrafficDataPoint newTdp, boolean useSeconds) {
        long timePeriod;
        LinkedList<TrafficDataPoint> tpList;
        LinkedList<TrafficDataPoint> nextList;
        TrafficDataPoint lastTsPeriod;
        if (useSeconds) {
            timePeriod = TIME_PERIOD_MINUTES;
            tpList = mSeconds;
            nextList = mMinutes;
            lastTsPeriod = mLastSecondUsedForMinute;
        } else {
            timePeriod = TIME_PERIOD_HOURS;
            tpList = mMinutes;
            nextList = mHours;
            lastTsPeriod = mLastMinuteUsedForHours;
        }
        if ((newTdp.mTimestamp / timePeriod) > (lastTsPeriod.mTimestamp / timePeriod)) {
            nextList.add(newTdp);
            if (useSeconds) {
                mLastSecondUsedForMinute = newTdp;
                removeAndAverage(newTdp, false);
            } else {
                mLastMinuteUsedForHours = newTdp;
            }
            HashSet<TrafficDataPoint> toRemove = new HashSet<>(10);
            for (TrafficDataPoint tph : tpList) {
                // List is iterated from oldest to newest, remember first one that we did not
                if (((newTdp.mTimestamp - tph.mTimestamp) / timePeriod) >= PERIODS_TO_KEEP) {
                    toRemove.add(tph);
                }
            }
            tpList.removeAll(toRemove);
        }
    }

    @SuppressWarnings("PublicInnerClass")
    public static final class LastDiff {

        private final TrafficDataPoint mFirstTdp;

        private final TrafficDataPoint mLastTdp;

        LastDiff(TrafficDataPoint lastTdp, TrafficDataPoint firstTdp) {
            mLastTdp = lastTdp;
            mFirstTdp = firstTdp;
        }

        public long getDiffIn() {
            return Math.max(0L, mFirstTdp.mInBytes - mLastTdp.mInBytes);
        }

        public long getDiffOut() {
            return Math.max(0L, mFirstTdp.mOutBytes - mLastTdp.mOutBytes);
        }

        public long getIn() {
            return mFirstTdp.mInBytes;
        }

        public long getOut() {
            return mFirstTdp.mOutBytes;
        }
    }

    @SuppressWarnings("PublicInnerClass")
    public static final class TrafficDataPoint {

        /**
         * the number of bytes that have been received from the server
         */
        final long mInBytes;

        /**
         * the number of bytes that have been sent to the server
         */
        final long mOutBytes;

        final long mTimestamp;

        public TrafficDataPoint(long inBytes, long outBytes, long timestamp) {
            mInBytes = inBytes;
            mOutBytes = outBytes;
            mTimestamp = timestamp;
        }
    }
}
