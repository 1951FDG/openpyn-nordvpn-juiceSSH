/*
 * Copyright (c) 2012-2016 Arne Schwabe
 * Distributed under the GNU GPL v2 with additional terms. For full terms see the file doc/LICENSE.txt
 */

package com.getsixtyfour.openvpnmgmt.android;

import android.content.Context;
import android.content.Intent;
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

    public static void doStartService(@NonNull Context context, @Nullable Bundle extras) {
        Intent intent = new Intent(context, OpenVPNService.class);
        intent.replaceExtras(extras);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void doStopService(@NonNull Context context) {
        Intent intent = new Intent(context, OpenVPNService.class);
        context.stopService(intent);
    }
}
