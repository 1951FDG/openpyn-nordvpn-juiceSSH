package ua.pp.msk.openvpnstatus.core;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings({ "UtilityClass", "WeakerAccess" })
public final class VpnStatus {

    private VpnStatus() {
    }

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

    private static final String[] mConnected = { CONNECTED };

    private static final String[] mNoReply = { CONNECTING, WAIT, RECONNECTING, RESOLVE, TCP_CONNECT };

    private static final String[] mNotConnected = { DISCONNECTED, EXITING };

    private static final String[] mReply = { AUTH, GET_CONFIG, ASSIGN_IP, ADD_ROUTES, AUTH_PENDING };

    @SuppressWarnings({ "OverlyComplexMethod", "MethodWithMultipleReturnPoints" })
    @NotNull
    public static ConnectionStatus getLevel(@NonNls @NotNull String name, @NonNls @Nullable String message) {
        if (RECONNECTING.equals(name) && "auth-failure".equals(message)) {
            return ConnectionStatus.LEVEL_AUTH_FAILED;
        }
        for (String x : mNoReply) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_CONNECTING_NO_SERVER_REPLY_YET;
            }
        }
        for (String x : mReply) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_CONNECTING_SERVER_REPLIED;
            }
        }
        for (String x : mConnected) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_CONNECTED;
            }
        }
        for (String x : mNotConnected) {
            if (name.equals(x)) {
                return ConnectionStatus.LEVEL_NOT_CONNECTED;
            }
        }
        return ConnectionStatus.UNKNOWN_LEVEL;
    }
}
