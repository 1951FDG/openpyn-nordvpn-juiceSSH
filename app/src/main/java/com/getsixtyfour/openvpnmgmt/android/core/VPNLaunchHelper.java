/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.android.core;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings({ "UtilityClass", "ClassWithTooManyTransitiveDependencies" })
public final class VPNLaunchHelper {

    public static void startOpenVPNService(@NonNull Context context) {
        Intent intent = prepareStartService(context);
        if (intent != null) {
            ContextCompat.startForegroundService(context, intent);
        }
    }

    private VPNLaunchHelper() {
    }

    @Nullable
    private static Intent getStartServiceIntent(Context context) {
        Intent intent = new Intent(context, OpenVPNService.class);
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveService(intent, 0);
        return (resolveInfo == null) ? null : intent;
    }

    private static Intent prepareStartService(Context context) {
        Intent intent = getStartServiceIntent(context);
        if (intent != null) {
            intent.putExtra(OpenVPNService.EXTRA_ALWAYS_SHOW_NOTIFICATION, true);
            intent.putExtra(OpenVPNService.EXTRA_HOST, VPNAuthenticationHandler.getHost(context));
            intent.putExtra(OpenVPNService.EXTRA_PORT, VPNAuthenticationHandler.getPort(context));
        }
        return intent;
    }
}
