package com.getsixtyfour.openvpnmgmt.net;

import org.jetbrains.annotations.NonNls;

/**
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
final class Constants {

    @NonNls
    static final String AUTH_TOKEN_PREFIX = "Auth-Token:";

    @NonNls
    static final String ERROR_PREFIX = "ERROR:";

    @NonNls
    static final String MANAGEMENT_CMD_PREFIX = "MANAGEMENT: CMD";

    @NonNls
    static final String MANAGEMENT_VERSION_PREFIX = "Management Version:";

    @NonNls
    static final String NEED_PREFIX = "Need";

    @NonNls
    static final String NOTE_PREFIX = "NOTE:";

    @NonNls
    static final String OPEN_VPN_VERSION_PREFIX = "OpenVPN Version:";

    @NonNls
    static final String SUCCESS_PREFIX = "SUCCESS:";

    @NonNls
    static final String VERIFICATION_FAILED_PREFIX = "Verification Failed";

    @NonNls
    static final String WAITING_FOR_HOLD_RELEASE_PREFIX = "Waiting for hold release";

    @NonNls
    static final String WARNING_PREFIX = "WARNING:";

    @NonNls
    static final String ARG_INTERACT = "interact";

    @NonNls
    static final String ARG_ON = " on";

    @NonNls
    static final String ARG_RELEASE = " release";

    @NonNls
    static final String ARG_SIGTERM = "SIGTERM";

    @NonNls
    static final String NOT_SUPPORTED_YET = "Not supported yet";

    @NonNls
    static final String SOCKET_IS_NOT_CONNECTED = "Socket is not connected";

    @NonNls
    static final String STREAM_CLOSED = "Stream closed";

    private Constants() {
    }
}
