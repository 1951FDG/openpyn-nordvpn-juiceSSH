package com.getsixtyfour.openvpnmgmt.net;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "UtilityClass", "SpellCheckingInspection", "WeakerAccess" })
public final class Commands {

    /**
     * auth-retry none — Client will exit with a fatal error.
     * auth-retry nointeract — Client will retry the connection without requerying for an username/password.
     * auth-retry interact — Client will requery for an username/password and/or private key password before attempting a reconnection.
     */

    public static final String AUTH_COMMAND = "auth-retry %s";

    public static final String BYTECOUNT_COMMAND = "bytecount %d";

    public static final String HELP_COMMAND = "help";

    /**
     * hold         -- show current hold flag, 0=off, 1=on.
     * hold on      -- turn on hold flag so that future restarts will hold.
     * hold off     -- turn off hold flag so that future restarts will not hold.
     * hold release -- leave hold state and start OpenVPN, but do not alter the current hold flag setting.
     */

    public static final String HOLD_COMMAND = "hold%s";

    /**
     * log on     -- Enable real-time output of log messages.
     * log all    -- Show currently cached log file history.
     * log on all -- Atomically show all currently cached log file history then enable real-time notification of new log file messages.
     * log off    -- Turn off real-time notification of log messages.
     * log 20     -- Show the most recent 20 lines of log file history.
     */

    public static final String LOG_COMMAND = "log%s";

    public static final String PASSWORD_COMMAND = "password '%s' %s";

    /**
     * The signal command will send a signal to the OpenVPN daemon.
     * The signal can be one of SIGHUP, SIGTERM, SIGUSR1, or SIGUSR2.
     */

    public static final String SIGNAL_COMMAND = "signal %s";

    /**
     * state        -- Print current OpenVPN state.
     * state on     -- Enable real-time notification of state changes.
     * state off    -- Disable real-time notification of state changes.
     * state all    -- Print current state history.
     * state 3      -- Print the 3 most recent state transitions.
     * state on all -- Atomically show state history while at the same time enable real-time state notification of future state transitions.
     */

    public static final String STATE_COMMAND = "state%s";

    public static final String STATUS_COMMAND = "status";

    public static final String USERNAME_COMMAND = "username '%s' %s";

    public static final String VERSION_COMMAND = "version";

    private Commands() {
    }
}
