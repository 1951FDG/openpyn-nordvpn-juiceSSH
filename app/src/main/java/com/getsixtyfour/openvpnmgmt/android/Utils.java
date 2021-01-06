package com.getsixtyfour.openvpnmgmt.android;

import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
public final class Utils {

    private Utils() {
    }

    public static void doStartService(@NonNull Context context, @Nullable Bundle extras) {
        Intent intent = new Intent(context, OpenVpnService.class);
        intent.replaceExtras(extras);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void doStopService(@NonNull Context context) {
        Intent intent = new Intent(context, OpenVpnService.class);
        context.stopService(intent);
    }

    @CheckResult
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static long getTotalUsage(@NonNull Context context, long start, long end, int uid, int tag) {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        if (networkStatsManager != null) {
            int networkType = NetworkCapabilities.TRANSPORT_WIFI;
            NetworkStats stats = networkStatsManager.queryDetailsForUidTag(networkType, "", start, end, uid, tag);
            return getTotalUsage(stats);
        }
        return 0L;
    }

    @CheckResult
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static long getTotalUsage(@Nullable NetworkStats stats) {
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

    static void doSendBroadcast(@NonNull Context context, @NonNull String state, @NonNull String message) {
        Intent intent = new Intent();
        intent.setAction(Constants.ACTION_VPN_STATE_CHANGED);
        intent.putExtra(Constants.EXTRA_STATE, state);
        intent.putExtra(Constants.EXTRA_MESSAGE, message);
        //noinspection UnnecessaryFullyQualifiedName
        context.sendBroadcast(intent, android.Manifest.permission.ACCESS_NETWORK_STATE);
    }
}
