package com.eggheadgames.aboutbox.share;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.eggheadgames.aboutbox.AboutBoxUtils;
import com.eggheadgames.aboutbox.AboutConfig;

@SuppressWarnings("UtilityClass")
public final class ShareUtil {

    private ShareUtil() {
        // Utility class
    }

    public static void share(@NonNull Activity activity) {
        AboutConfig config = AboutConfig.getInstance();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("text/plain");

        String shareMessage = config.shareMessage;

        if (!TextUtils.isEmpty(config.packageName) && (config.buildType != null)) {
            switch (config.buildType) {
                case GOOGLE:
                    shareMessage = shareMessage + AboutBoxUtils.playStoreAppURI + config.packageName;
                    break;
                case AMAZON:
                    shareMessage = shareMessage + AboutBoxUtils.amznStoreAppURI + config.packageName;
                    break;
            }
        }

        intent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        ContextCompat.startActivity(activity, Intent.createChooser(intent, config.sharingTitle), null);
    }
}
