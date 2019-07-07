/*
 * Copyright (c) 2012-2017 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package ua.pp.msk.openvpnstatus.core;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

public class TrafficHistory implements Parcelable {

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

    @SuppressWarnings("WeakerAccess")
    TrafficHistory(Parcel source, ClassLoader loader) {
        source.readList(seconds, loader);
        source.readList(minutes, loader);
        source.readList(hours, loader);
        lastSecondUsedForMinute = source.readParcelable(loader);
        lastMinuteUsedForHours = source.readParcelable(loader);
    }

    public static final Creator<TrafficHistory> CREATOR = new ClassLoaderCreator<TrafficHistory>() {
        @Override
        public TrafficHistory createFromParcel(Parcel source, ClassLoader loader) {
            return new TrafficHistory(source, loader);
        }

        @Override
        public TrafficHistory createFromParcel(Parcel source) {
            return new TrafficHistory(source, null);
        }

        @Override
        public TrafficHistory[] newArray(int size) {
            return new TrafficHistory[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeList(seconds);
        dest.writeList(minutes);
        dest.writeList(hours);
        dest.writeParcelable(lastSecondUsedForMinute, 0);
        dest.writeParcelable(lastMinuteUsedForHours, 0);
    }

    @SuppressWarnings("unused")
    public LinkedList<TrafficDataPoint> getHours() {
        return hours;
    }

    @SuppressWarnings("unused")
    public LinkedList<TrafficDataPoint> getMinutes() {
        return minutes;
    }

    @SuppressWarnings("unused")
    public LinkedList<TrafficDataPoint> getSeconds() {
        return seconds;
    }

    @SuppressWarnings("unused")
    public static LinkedList<TrafficDataPoint> getDummyList() {
        LinkedList<TrafficDataPoint> list = new LinkedList<>();
        list.add(new TrafficDataPoint(0L, 0L, System.currentTimeMillis()));
        return list;
    }

    @SuppressWarnings("PackageVisibleField")
    private static final class TrafficDataPoint implements Parcelable {

        final long mTimestamp;

        final long mIn;

        final long mOut;

        TrafficDataPoint(long inBytes, long outBytes, long timestamp) {
            mIn = inBytes;
            mOut = outBytes;
            mTimestamp = timestamp;
        }

        TrafficDataPoint(Parcel source) {
            mTimestamp = source.readLong();
            mIn = source.readLong();
            mOut = source.readLong();
        }

        public static final Creator<TrafficDataPoint> CREATOR = new Creator<TrafficDataPoint>() {
            @Override
            public TrafficDataPoint createFromParcel(Parcel source) {
                return new TrafficDataPoint(source);
            }

            @Override
            public TrafficDataPoint[] newArray(int size) {
                return new TrafficDataPoint[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mTimestamp);
            dest.writeLong(mIn);
            dest.writeLong(mOut);
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
