package com.getsixtyfour.openvpnmgmt.android.constant;

import org.jetbrains.annotations.NonNls;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
public final class IntentConstants {

    @NonNls
    private static final String INTENT_PACKAGE_PREFIX = "com.getsixtyfour.openvpnmgmt.android";

    @NonNls
    public static final String EXTRA_MESSAGE = INTENT_PACKAGE_PREFIX + ".extra.MESSAGE";

    @NonNls
    public static final String EXTRA_STATE = INTENT_PACKAGE_PREFIX + ".extra.STATE";

    @NonNls
    public static final String ACTION_VPN_STATE_CHANGED = INTENT_PACKAGE_PREFIX + ".action.VPN_STATE_CHANGED";

    @NonNls
    public static final String EXTRA_POST_NOTIFICATION = INTENT_PACKAGE_PREFIX + ".extra.POST_NOTIFICATION";

    @NonNls
    public static final String EXTRA_SEND_BROADCAST = INTENT_PACKAGE_PREFIX + ".extra.SEND_BROADCAST";

    @NonNls
    public static final String EXTRA_HOST = INTENT_PACKAGE_PREFIX + ".extra.HOST";

    @NonNls
    public static final String EXTRA_PORT = INTENT_PACKAGE_PREFIX + ".extra.PORT";

    @NonNls
    public static final String ACTION_START_SERVICE_NOT_STICKY = INTENT_PACKAGE_PREFIX + ".action.START_SERVICE_NOT_STICKY";

    @NonNls
    public static final String ACTION_START_SERVICE_STICKY = INTENT_PACKAGE_PREFIX + ".action.START_SERVICE_STICKY";

    private IntentConstants() {
    }
}
