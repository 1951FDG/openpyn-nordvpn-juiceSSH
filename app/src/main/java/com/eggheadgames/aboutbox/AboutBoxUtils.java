package com.eggheadgames.aboutbox;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

@SuppressWarnings({ "UtilityClass", "HardCodedStringLiteral" })
public final class AboutBoxUtils {

    public static final String playStoreAppURI = "https://play.google.com/store/apps/details?id=";
    public static final String amznStoreAppURI = "https://www.amazon.com/gp/mas/dl/android?p=";

    private AboutBoxUtils() {
        //nothing
    }

    public static void getOpenFacebookIntent(@NonNull Activity activity, @NonNull String name) {
        AboutConfig config = AboutConfig.getInstance();
        try {
            activity.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://profile/" + name));
            ContextCompat.startActivity(activity, intent, null);
        } catch (PackageManager.NameNotFoundException | ActivityNotFoundException e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/" + name));
                ContextCompat.startActivity(activity, intent, null);
            } catch (ActivityNotFoundException e1) {
                if (config.analytics != null) {
                    config.analytics.logException(e1, false);
                }
            }
        }
    }

    public static void startTwitter(@NonNull Activity activity, @NonNull String name) {
        AboutConfig config = AboutConfig.getInstance();
        try {
            activity.getPackageManager().getPackageInfo("com.twitter.android", 0);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("twitter://user?screen_name=" + name));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ContextCompat.startActivity(activity, intent, null);
        } catch (PackageManager.NameNotFoundException | ActivityNotFoundException e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/" + name));
                ContextCompat.startActivity(activity, intent, null);
            } catch (ActivityNotFoundException e1) {
                if (config.analytics != null) {
                    config.analytics.logException(e1, false);
                }
            }
        }
    }

    public static void openApp(@NonNull Activity activity, @NonNull AboutConfig.BuildType buildType, @NonNull String packageName) {
        String appURI = null;
        String webURI = null;
        switch (buildType) {
            case GOOGLE:
                appURI = "market://details?id=" + packageName;
                webURI = playStoreAppURI + packageName;
                break;
            case AMAZON:
                appURI = "amzn://apps/android?p=" + packageName;
                webURI = amznStoreAppURI + packageName;
                break;
        }
        openApplication(activity, appURI, webURI);
    }

    public static void openPublisher(@NonNull Activity activity, @NonNull AboutConfig.BuildType buildType, @NonNull String publisher,
                                     @NonNull String packageName) {
        String appURI = null;
        String webURI = null;
        switch (buildType) {
            case GOOGLE:
                // see:
                // https://developer.android.com/distribute/marketing-tools/linking-to-google-play.html#OpeningPublisher
                // https://stackoverflow.com/questions/32029408/how-to-open-developer-page-on-google-play-store-market
                // https://issuetracker.google.com/65244694
                if (publisher.matches("\\d+")) {
                    webURI = "https://play.google.com/store/apps/dev?id=" + publisher;
                    appURI = webURI;
                } else {
                    appURI = "market://search?q=pub:" + publisher;
                    webURI = "https://play.google.com/store/search?q=pub:" + publisher;
                }
                break;
            case AMAZON:
                appURI = "amzn://apps/android?showAll=1&p=" + packageName;
                webURI = "http://www.amazon.com/gp/mas/dl/android?showAll=1&p=" + packageName;
                break;
        }
        openApplication(activity, appURI, webURI);
    }

    public static void openApplication(@NonNull Activity activity, @NonNull String appURI, @NonNull String webURI) {
        AboutConfig config = AboutConfig.getInstance();
        try {
            ContextCompat.startActivity(activity, new Intent(Intent.ACTION_VIEW, Uri.parse(appURI)), null);
        } catch (ActivityNotFoundException e1) {
            try {
                openHTMLPage(activity, webURI);
            } catch (ActivityNotFoundException e2) {
                if (config.analytics != null) {
                    config.analytics.logException(e2, false);
                }
            }
        }
    }

    public static void openHTMLPage(@NonNull Activity activity, @NonNull String htmlPath) {
        ContextCompat.startActivity(activity, new Intent(Intent.ACTION_VIEW, Uri.parse(htmlPath)), null);
    }
}
