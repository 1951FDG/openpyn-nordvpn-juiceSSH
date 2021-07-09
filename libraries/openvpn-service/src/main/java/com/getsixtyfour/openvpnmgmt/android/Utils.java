package com.getsixtyfour.openvpnmgmt.android;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.NetworkCapabilities;
import android.os.Build;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
final class Utils {

    private Utils() {
    }

    @Nullable
    static String getTopLevelCauseMessage(@NonNull Throwable t) {
        Throwable topLevelCause = t;
        while (topLevelCause.getCause() != null) {
            topLevelCause = topLevelCause.getCause();
        }
        return topLevelCause.getMessage();
    }

    @CheckResult
    @RequiresApi(api = Build.VERSION_CODES.N)
    static long getTotalUsage(@NonNull Context context, long start, long end, int uid, int tag) {
        long result = 0L;
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        if (networkStatsManager != null) {
            int networkType = NetworkCapabilities.TRANSPORT_WIFI;
            NetworkStats stats = networkStatsManager.queryDetailsForUidTag(networkType, "", start, end, uid, tag);
            result = getTotalUsage(stats);
        }
        return result;
    }

    @CheckResult
    @RequiresApi(api = Build.VERSION_CODES.M)
    static long getTotalUsage(@Nullable NetworkStats stats) {
        long bytes = 0L;
        if (stats != null) {
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            while (stats.hasNextBucket() && stats.getNextBucket(bucket)) {
                bytes += bucket.getRxBytes() + bucket.getTxBytes();
            }
            stats.close();
        }
        return bytes;
    }
}
