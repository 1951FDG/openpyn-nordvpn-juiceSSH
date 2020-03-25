/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public class TrafficHistory {

    public TrafficHistory() {
    }

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

    @SuppressWarnings("unused")
    public static LinkedList<TrafficDataPoint> getDummyList() {
        LinkedList<TrafficDataPoint> list = new LinkedList<>();
        list.add(new TrafficDataPoint(0L, 0L, System.currentTimeMillis()));
        return list;
    }
    // TODO: one with trafficpoint
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public LastDiff add(long in, long out) {
        TrafficDataPoint tdp = new TrafficDataPoint(in, out, System.currentTimeMillis());
        TrafficDataPoint lastTdp = mSeconds.isEmpty() ? new TrafficDataPoint(0L, 0L, System.currentTimeMillis()) : mSeconds.getLast();
        LastDiff diff = new LastDiff(lastTdp, tdp);
        mSeconds.add(tdp);
        if (mLastSecondUsedForMinute == null) {
            mLastSecondUsedForMinute = new TrafficDataPoint(0L, 0L, 0L);
            mLastMinuteUsedForHours = new TrafficDataPoint(0L, 0L, 0L);
        }
        removeAndAverage(tdp, true);
        return diff;
    }

    @SuppressWarnings("unused")
    public List<TrafficDataPoint> getHours() {
        return Collections.unmodifiableList(mHours);
    }

    @SuppressWarnings("unused")
    public List<TrafficDataPoint> getMinutes() {
        return Collections.unmodifiableList(mMinutes);
    }

    @SuppressWarnings("unused")
    public List<TrafficDataPoint> getSeconds() {
        return Collections.unmodifiableList(mSeconds);
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    private void removeAndAverage(TrafficDataPoint newTdp, boolean useSeconds) {
        HashSet<TrafficDataPoint> toRemove = new HashSet<>(10);
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
        public final long mInBytes;

        /**
         * the number of bytes that have been sent to the server
         */
        public final long mOutBytes;

        public final long mTimestamp;

        public TrafficDataPoint(long inBytes, long outBytes, long timestamp) {
            mInBytes = inBytes;
            mOutBytes = outBytes;
            mTimestamp = timestamp;
        }
    }
}
