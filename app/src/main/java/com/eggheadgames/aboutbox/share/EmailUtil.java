package com.eggheadgames.aboutbox.share;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.eggheadgames.aboutbox.AboutConfig;

@SuppressWarnings({ "UtilityClass", "HardCodedStringLiteral", "StringConcatenation" })
public final class EmailUtil {

    private EmailUtil() {
        // Utility class
    }

    @SuppressWarnings("HardcodedLineSeparator")
    public static void contactUs(@NonNull Activity activity) {
        AboutConfig config = AboutConfig.getInstance();

        Uri mailto = Uri.fromParts("mailto", config.emailAddress, null);

        String emailBody = config.emailBody;
        if (TextUtils.isEmpty(emailBody)) {
            String deviceInfo = "";
            deviceInfo += "\n App version: " + config.version;
            deviceInfo += "\n Android version: " + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")";
            deviceInfo += "\n Device: " + Build.MODEL + " (" + Build.PRODUCT + ")";
            deviceInfo += "\n Platform: " + platformName(config.buildType);
            emailBody = config.emailBodyPrompt + "\n\n\n\n\n" + "---------------------------" + deviceInfo;
        }

        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, mailto);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, config.emailSubject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
            ContextCompat.startActivity(activity, Intent.createChooser(emailIntent, "Send email..."), null);
        } catch (ActivityNotFoundException e) {
            if (config.analytics != null) {
                config.analytics.logException(e, false);
            }
        }
    }

    @SuppressWarnings("MethodWithMultipleReturnPoints")
    private static String platformName(AboutConfig.BuildType buildType) {
        switch (buildType) {
            case GOOGLE:
                return "Google Play";
            case AMAZON:
                return "Amazon Kindle";
            default:
                return "Unknown";
        }
    }
}
