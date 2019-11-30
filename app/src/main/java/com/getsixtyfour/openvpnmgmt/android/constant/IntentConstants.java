package com.getsixtyfour.openvpnmgmt.android.constant;

import org.jetbrains.annotations.NonNls;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
public final class IntentConstants {

    @NonNls
    public static final String EXTRA_MESSAGE = "message";

    @NonNls
    public static final String EXTRA_STATE = "state";

    @NonNls
    public static final String INTENT_PACKAGE_PREFIX = "com.getsixtyfour.openvpnmgmt.android.action.";

    @NonNls
    public static final String INTENT_ACTION_VPN_STATUS = INTENT_PACKAGE_PREFIX + "VPN_STATUS";

    private IntentConstants() {
    }
}
