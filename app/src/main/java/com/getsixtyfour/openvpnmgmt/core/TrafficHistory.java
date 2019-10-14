/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public class TrafficHistory {

    @SuppressWarnings("WeakerAccess")
    public static final long PERIODS_TO_KEEP = 5L;

    @SuppressWarnings("WeakerAccess")
    public static final long TIME_PERIOD_MINUTES = 60L * 1000L;

    @SuppressWarnings("WeakerAccess")
    public static final long TIME_PERIOD_HOURS = 3600L * 1000L;

    private final LinkedList<TrafficDataPoint> seconds = new LinkedList<>();

    private final LinkedList<TrafficDataPoint> minutes = new LinkedList<>();

    private final LinkedList<TrafficDataPoint> hours = new LinkedList<>();

    private TrafficDataPoint lastSecondUsedForMinute;

    private TrafficDataPoint lastMinuteUsedForHours;

    public TrafficHistory() {
    }

    public LastDiff getLastDiff(TrafficDataPoint tdp) {
        TrafficDataPoint newTdp = tdp;
        TrafficDataPoint lastTdp;
        if (seconds.isEmpty()) {
            lastTdp = new TrafficDataPoint(0L, 0L, System.currentTimeMillis());
        } else {
            lastTdp = seconds.getLast();
        }
        if (newTdp == null) {
            if (seconds.size() < 2) {
                newTdp = lastTdp;
            } else {
                Iterator<TrafficDataPoint> iterator = seconds.descendingIterator();
                newTdp = iterator.next();
            }
        }
        return new LastDiff(lastTdp, newTdp);
    }

    @SuppressWarnings("unused")
    public List<TrafficDataPoint> getHours() {
        return Collections.unmodifiableList(hours);
    }

    @SuppressWarnings("unused")
    public List<TrafficDataPoint> getMinutes() {
        return Collections.unmodifiableList(minutes);
    }

    @SuppressWarnings("unused")
    public List<TrafficDataPoint> getSeconds() {
        return Collections.unmodifiableList(seconds);
    }

    @SuppressWarnings("unused")
    public static LinkedList<TrafficDataPoint> getDummyList() {
        LinkedList<TrafficDataPoint> list = new LinkedList<>();
        list.add(new TrafficDataPoint(0L, 0L, System.currentTimeMillis()));
        return list;
    }

    @SuppressWarnings("PackageVisibleField")
    private static final class TrafficDataPoint {

        final long mTimestamp;

        final long mIn;

        final long mOut;

        TrafficDataPoint(long inBytes, long outBytes, long timestamp) {
            mIn = inBytes;
            mOut = outBytes;
            mTimestamp = timestamp;
        }
    }

    public LastDiff add(long in, long out) {
        TrafficDataPoint tdp = new TrafficDataPoint(in, out, System.currentTimeMillis());
        LastDiff diff = getLastDiff(tdp);
        seconds.add(tdp);
        if (lastSecondUsedForMinute == null) {
            lastSecondUsedForMinute = new TrafficDataPoint(0L, 0L, 0L);
            lastMinuteUsedForHours = new TrafficDataPoint(0L, 0L, 0L);
        }
        removeAndAverage(tdp, true);
        return diff;
    }

    private void removeAndAverage(TrafficDataPoint newTdp, boolean useSeconds) {
        HashSet<TrafficDataPoint> toRemove = new HashSet<>(10);
        long timePeriod;
        LinkedList<TrafficDataPoint> tpList;
        LinkedList<TrafficDataPoint> nextList;
        TrafficDataPoint lastTsPeriod;
        if (useSeconds) {
            timePeriod = TIME_PERIOD_MINUTES;
            tpList = seconds;
            nextList = minutes;
            lastTsPeriod = lastSecondUsedForMinute;
        } else {
            timePeriod = TIME_PERIOD_HOURS;
            tpList = minutes;
            nextList = hours;
            lastTsPeriod = lastMinuteUsedForHours;
        }
        if ((newTdp.mTimestamp / timePeriod) > (lastTsPeriod.mTimestamp / timePeriod)) {
            nextList.add(newTdp);
            if (useSeconds) {
                lastSecondUsedForMinute = newTdp;
                removeAndAverage(newTdp, false);
            } else {
                lastMinuteUsedForHours = newTdp;
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

        private final TrafficDataPoint mTdp;

        private final TrafficDataPoint mLastTdp;

        LastDiff(TrafficDataPoint lastTdp, TrafficDataPoint tdp) {
            mLastTdp = lastTdp;
            mTdp = tdp;
        }

        public long getDiffOut() {
            return Math.max(0L, mTdp.mOut - mLastTdp.mOut);
        }

        public long getDiffIn() {
            return Math.max(0L, mTdp.mIn - mLastTdp.mIn);
        }

        public long getIn() {
            return mTdp.mIn;
        }

        public long getOut() {
            return mTdp.mOut;
        }
    }
}
