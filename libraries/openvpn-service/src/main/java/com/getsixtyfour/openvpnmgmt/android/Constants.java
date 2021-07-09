package com.getsixtyfour.openvpnmgmt.android;

import org.jetbrains.annotations.NonNls;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "UtilityClass", "ConstantExpression" })
public final class Constants {

    @NonNls
    public static final String BG_CHANNEL_ID = "openvpn_bg";

    static final int BG_NOTIFICATION_ID = BG_CHANNEL_ID.hashCode();

    @NonNls
    public static final String NEW_STATUS_CHANNEL_ID = "openvpn_newstat";

    static final int NEW_STATUS_NOTIFICATION_ID = NEW_STATUS_CHANNEL_ID.hashCode();

    public static final int TRAFFIC_STATS_TAG = 0x42;

    public static final int TYPE_NONE = 0;

    public static final int TYPE_FINISH = 1;

    public static final int TYPE_EXIT = 2;

    public static final int TYPE_KILL = 3;

    @NonNls
    static final String DEFAULT_SERVER_IP = "192.168.1.1";

    static final int DEFAULT_SERVER_PORT = 23;

    @NonNls
    private static final String INTENT_PACKAGE_PREFIX = "com.getsixtyfour.openvpnmgmt.android";

    @NonNls
    public static final String EXTRA_ACTION = INTENT_PACKAGE_PREFIX + ".extra.ACTION";

    @NonNls
    public static final String EXTRA_MESSAGE = INTENT_PACKAGE_PREFIX + ".extra.MESSAGE";

    @NonNls
    public static final String EXTRA_STATE = INTENT_PACKAGE_PREFIX + ".extra.STATE";

    @NonNls
    public static final String ACTION_EXIT = INTENT_PACKAGE_PREFIX + ".action.EXIT";

    @NonNls
    public static final String ACTION_VPN_STATE_CHANGED = INTENT_PACKAGE_PREFIX + ".action.VPN_STATE_CHANGED";

    @NonNls
    public static final String EXTRA_POST_BYTE_COUNT_NOTIFICATION = INTENT_PACKAGE_PREFIX + ".extra.POST_BYTE_COUNT_NOTIFICATION";

    @NonNls
    public static final String EXTRA_POST_STATE_NOTIFICATION = INTENT_PACKAGE_PREFIX + ".extra.POST_STATE_NOTIFICATION";

    @NonNls
    public static final String EXTRA_SEND_STATE_BROADCAST = INTENT_PACKAGE_PREFIX + ".extra.SEND_STATE_BROADCAST";

    @NonNls
    public static final String EXTRA_HOST = INTENT_PACKAGE_PREFIX + ".extra.HOST";

    @NonNls
    public static final String EXTRA_PORT = INTENT_PACKAGE_PREFIX + ".extra.PORT";

    @NonNls
    public static final String EXTRA_PASSWORD = INTENT_PACKAGE_PREFIX + ".extra.PASSWORD";

    @NonNls
    public static final String EXTRA_VPN_USERNAME = INTENT_PACKAGE_PREFIX + ".extra.VPN_USERNAME";

    @NonNls
    public static final String EXTRA_VPN_PASSWORD = INTENT_PACKAGE_PREFIX + ".extra.VPN_PASSWORD";

    private Constants() {
    }
}
