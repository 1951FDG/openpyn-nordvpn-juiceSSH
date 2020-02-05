/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.getsixtyfour.openvpnmgmt.android.core.OpenVPNService;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
public final class VPNLaunchHelper {

    private VPNLaunchHelper() {
    }

    public static void startOpenVPNService(@NonNull Context context, @NonNull Bundle extras) {
        Intent intent = prepareStartService(context, extras);
        if (intent != null) {
            ContextCompat.startForegroundService(context, intent);
        }
    }

    @Nullable
    private static Intent getStartServiceIntent(Context context) {
        Intent intent = new Intent(context, OpenVPNService.class);
        PackageManager packageManager = context.getPackageManager();
        return (packageManager.resolveService(intent, 0) != null) ? intent : null;
    }

    private static Intent prepareStartService(Context context, Bundle extras) {
        Intent intent = getStartServiceIntent(context);
        if (intent != null) {
            intent.replaceExtras(extras);
        }
        return intent;
    }
}
