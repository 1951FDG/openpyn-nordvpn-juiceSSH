package com.getsixtyfour.openvpnmgmt.core;

import org.jetbrains.annotations.NonNls;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
final class Constants {

    @NonNls
    static final String MSG_NOT_SUPPORTED_YET = "Not supported yet";

    @NonNls
    static final String MSG_SOCKET_IS_NOT_CONNECTED = "Socket is not connected";

    @NonNls
    static final String PREFIX_AUTH_TOKEN = "Auth-Token:";

    @NonNls
    static final String PREFIX_ENTER_PASSWORD = "ENTER PASSWORD:";

    @NonNls
    static final String PREFIX_ERROR = "ERROR:";

    @NonNls
    static final String PREFIX_MANAGEMENT_CMD = "MANAGEMENT: CMD";

    @NonNls
    static final String PREFIX_NEED = "Need";

    @NonNls
    static final String PREFIX_NOTE = "NOTE:";

    @NonNls
    static final String PREFIX_SUCCESS = "SUCCESS:";

    @NonNls
    static final String PREFIX_VERIFICATION_FAILED = "Verification Failed";

    @NonNls
    static final String PREFIX_WAITING_FOR_HOLD_RELEASE = "Waiting for hold release";

    @NonNls
    static final String PREFIX_WARNING = "WARNING:";

    private Constants() {
    }
}
