package com.getsixtyfour.openvpnmgmt.core;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings("UtilityClass")
public final class VpnStatus {

    @NonNls
    public static final String ADD_ROUTES = "ADD_ROUTES";

    @NonNls
    public static final String ASSIGN_IP = "ASSIGN_IP";

    @NonNls
    public static final String AUTH = "AUTH";

    @NonNls
    public static final String AUTH_FAILED = "AUTH_FAILED";

    @NonNls
    public static final String AUTH_PENDING = "AUTH_PENDING";

    @NonNls
    public static final String CONNECTED = "CONNECTED";

    @NonNls
    public static final String CONNECTING = "CONNECTING";

    @NonNls
    public static final String DISCONNECTED = "DISCONNECTED";

    @NonNls
    public static final String EXITING = "EXITING";

    @NonNls
    public static final String GET_CONFIG = "GET_CONFIG";

    @NonNls
    public static final String RECONNECTING = "RECONNECTING";

    @NonNls
    public static final String RESOLVE = "RESOLVE";

    @NonNls
    public static final String TCP_CONNECT = "TCP_CONNECT";

    @NonNls
    public static final String WAIT = "WAIT";

    @NonNls
    public static final String AUTH_FAILURE = "auth-failure";

    private static final String[] CONNECTED_STRINGS = { CONNECTED };

    private static final String[] NOT_CONNECTED_STRINGS = { DISCONNECTED, EXITING };

    private static final String[] NO_REPLY_STRINGS = { CONNECTING, WAIT, RECONNECTING, RESOLVE, TCP_CONNECT };

    private static final String[] REPLY_STRINGS = { AUTH, GET_CONFIG, ASSIGN_IP, ADD_ROUTES, AUTH_PENDING };

    private VpnStatus() {
    }

    @SuppressWarnings({ "OverlyComplexMethod", "MethodWithMultipleReturnPoints" })
    public static @NotNull ConnectionStatus getLevel(@NotNull String name, @Nullable String message) {
        if (RECONNECTING.equals(name) && AUTH_FAILURE.equals(message)) {
            return ConnectionStatus.LEVEL_AUTH_FAILED;
        }
        for (String x : CONNECTED_STRINGS) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_CONNECTED;
            }
        }
        for (String x : NOT_CONNECTED_STRINGS) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_NOT_CONNECTED;
            }
        }
        for (String x : NO_REPLY_STRINGS) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
            }
        }
        for (String x : REPLY_STRINGS) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;
            }
        }
        return ConnectionStatus.LEVEL_UNKNOWN;
    }
}
