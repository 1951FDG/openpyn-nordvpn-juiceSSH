package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.core.ConnectionStatus;
import com.getsixtyfour.openvpnmgmt.core.LogLevel;
import com.getsixtyfour.openvpnmgmt.core.VpnStatus;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnStatus;
import com.getsixtyfour.openvpnmgmt.listeners.ByteCountManager;
import com.getsixtyfour.openvpnmgmt.listeners.ByteCountManager.ByteCount;
import com.getsixtyfour.openvpnmgmt.listeners.ByteCountManager.ByteCountListener;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager.Log;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager.LogListener;
import com.getsixtyfour.openvpnmgmt.listeners.StateManager;
import com.getsixtyfour.openvpnmgmt.listeners.StateManager.State;
import com.getsixtyfour.openvpnmgmt.listeners.StateManager.StateListener;
import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

/**
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings({ "Singleton", "OverlyCoupledClass", "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class ManagementConnection extends AbstractConnection implements Connection {

    public static final Integer BYTE_COUNT_INTERVAL = 2;

    private static final String ARG_INTERACT = "interact";

    private static final String ARG_ON = " on";

    private static final String ARG_RELEASE = " release";

    private static final String ARG_SIGTERM = "SIGTERM";

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementConnection.class);

    @NonNls
    private static final String NOT_SUPPORTED_YET = "Not supported yet";

    @NonNls
    private static final String SOCKET_IS_NOT_CONNECTED = "Socket is not connected";

    @NonNls
    private static final String STREAM_CLOSED = "Stream closed";

    private static volatile ManagementConnection sInstance = null;

    private final ByteCountManager mByteCountManager = new ByteCountManager();

    private final LogManager mLogManager = new LogManager();

    private final StateManager mStateManager = new StateManager();

    private ConnectionListener mConnectionListener;

    private ConnectionStatus mLastLevel = ConnectionStatus.LEVEL_NOT_CONNECTED;

    private UsernamePasswordHandler mUsernamePasswordHandler;

    private ManagementConnection() {
    }

    @NotNull
    @SuppressWarnings({ "DoubleCheckedLocking", "SynchronizeOnThis" })
    public static ManagementConnection getInstance() {
        if (sInstance == null) {
            synchronized (ManagementConnection.class) {
                if (sInstance == null) {
                    sInstance = new ManagementConnection();
                }
            }
        }
        return sInstance;
    }

    @Override
    public boolean addByteCountListener(@NotNull ByteCountListener listener) {
        return mByteCountManager.addListener(Objects.requireNonNull(listener));
    }

    @Override
    public boolean addLogListener(@NotNull LogListener listener) {
        return mLogManager.addListener(Objects.requireNonNull(listener));
    }

    @Override
    public boolean addStateListener(@NotNull StateListener listener) {
        return mStateManager.addListener(Objects.requireNonNull(listener));
    }

    @Override
    public void connect(@NotNull String host, @NotNull Integer port) throws IOException {
        if (!isConnected()) {
            try {
                super.connect(host, port);
                onConnected();
            } catch (IllegalArgumentException | IOException e) {
                onConnectError(e);
            }
        }
        // Ensures state listeners are notified of current state if VPN is already connected
        {
            String result = executeCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, ""));
            String[] lines = result.split(System.lineSeparator());
            String argument = (lines.length >= 1) ? lines[lines.length - 1] : "";
            if (!argument.isEmpty() && !argument.contains(VpnStatus.AUTH_FAILURE)) {
                processState(argument);
            }
        }
    }

    @Override
    public void disconnect() {
        if (isConnected()) {
            super.disconnect();
            onDisconnected();
        }
    }

    @NotNull
    @SuppressWarnings({ "NestedAssignment", "MethodCallInLoopCondition", "MagicNumber" })
    @Override
    public String executeCommand(@NotNull String command) throws IOException {
        if (!isConnected()) {
            throw new IOException(SOCKET_IS_NOT_CONNECTED);
        }
        StringBuilder sb = new StringBuilder(256);
        BufferedReader in = getBufferedReader();
        BufferedWriter out = getBufferedWriter();
        out.write(command);
        out.newLine();
        out.flush();
        @NonNls String line;
        while ((line = in.readLine()) != null) {
            if (!line.isEmpty()) {
                if ("END".equals(line)) {
                    break;
                }
                // LOGGER.info("Read from socket line: {}", line);
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    @NotNull
    @Override
    public String getManagementVersion() throws IOException {
        String result = executeCommand(Commands.VERSION_COMMAND);
        String[] lines = result.split(System.lineSeparator());
        String line = (lines.length >= 1) ? lines[lines.length - 1] : "";
        if (!line.isEmpty() && line.startsWith(Strings.MANAGEMENT_VERSION_PREFIX)) {
            return line.substring(Strings.MANAGEMENT_VERSION_PREFIX.length() + 1);
        }
        return "";
    }

    @NotNull
    @Override
    public Status getOpenVPNStatus() throws OpenVpnParseException, IOException {
        OpenVpnStatus ovs = new OpenVpnStatus();
        ovs.setCommandOutput(executeCommand(Commands.STATUS_COMMAND));
        return ovs;
    }

    @NotNull
    @Override
    public String getOpenVPNVersion() throws IOException {
        String result = executeCommand(Commands.VERSION_COMMAND);
        String[] lines = result.split(System.lineSeparator());
        String line = (lines.length >= 2) ? lines[lines.length - 2] : "";
        if (!line.isEmpty() && line.startsWith(Strings.OPEN_VPN_VERSION_PREFIX)) {
            return line.substring(Strings.OPEN_VPN_VERSION_PREFIX.length() + 1);
        }
        return "";
    }

    @Override
    public boolean isOpenVPNActive() {
        return (mLastLevel != ConnectionStatus.LEVEL_NOT_CONNECTED) && (mLastLevel != ConnectionStatus.LEVEL_AUTH_FAILED);
    }

    @Override
    public boolean removeByteCountListener(@NotNull ByteCountListener listener) {
        return mByteCountManager.removeListener(Objects.requireNonNull(listener));
    }

    @Override
    public boolean removeLogListener(@NotNull LogListener listener) {
        return mLogManager.removeListener(Objects.requireNonNull(listener));
    }

    @Override
    public boolean removeStateListener(@NotNull StateListener listener) {
        return mStateManager.removeListener(Objects.requireNonNull(listener));
    }

    @SuppressWarnings({ "NestedAssignment", "MethodCallInLoopCondition", "ProhibitedExceptionThrown" })
    @Override
    public void run() {
        if (!isConnected()) {
            // UncheckedIOException requires Android N
            throw new RuntimeException(new IOException(SOCKET_IS_NOT_CONNECTED));
        }
        {
            try {
                // managementCommand(String.format(Locale.ROOT, Commands.AUTH_COMMAND, ARG_INTERACT));
                managementCommand(String.format(Locale.ROOT, Commands.BYTECOUNT_COMMAND, BYTE_COUNT_INTERVAL));
                managementCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, ARG_ON));
                managementCommand(String.format(Locale.ROOT, Commands.LOG_COMMAND, ARG_ON));
                managementCommand(String.format(Locale.ROOT, Commands.HOLD_COMMAND, ARG_RELEASE));
                BufferedReader in = getBufferedReader();
                String line;
                while ((line = in.readLine()) != null) {
                    if (!line.isEmpty()) {
                        // LOGGER.info("Read from socket line: {}", line);
                        parseInput(line);
                    }
                }
            } catch (IOException e) {
                String message = e.getMessage();
                if (!STREAM_CLOSED.equals(message)) {
                    LOGGER.error(message, e);
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                LOGGER.error("Could not parse string", e);
            }
        }
        LOGGER.info("TERMINATED");
        throw new ThreadDeath();
    }

    @Override
    public void setConnectionListener(@Nullable ConnectionListener connectionListener) {
        mConnectionListener = connectionListener;
    }

    @Override
    public void setUsernamePasswordHandler(@Nullable UsernamePasswordHandler handler) {
        mUsernamePasswordHandler = handler;
    }

    @Override
    public void stopOpenVPN() throws IOException {
        managementCommand(String.format(Locale.ROOT, Commands.SIGNAL_COMMAND, ARG_SIGTERM));
    }

    private void managementCommand(String command) throws IOException {
        if (!isConnected()) {
            throw new IOException(SOCKET_IS_NOT_CONNECTED);
        }
        BufferedWriter out = getBufferedWriter();
        out.write(command);
        out.newLine();
        out.flush();
    }

    private void onConnectError(@NotNull Throwable e) {
        if (!(e instanceof IllegalArgumentException) && !(e instanceof IOException)) {
            LOGGER.error("Unknown exception thrown");
        }
        LOGGER.error(e.toString());
        ConnectionListener listener = mConnectionListener;
        if (listener != null) {
            listener.onConnectError(e);
        }
    }

    private void onConnected() {
        LOGGER.info("Connected");
        ConnectionListener listener = mConnectionListener;
        if (listener != null) {
            listener.onConnected();
        }
    }

    private void onDisconnected() {
        LOGGER.info("Disconnected");
        ConnectionListener listener = mConnectionListener;
        if (listener != null) {
            listener.onDisconnected();
        }
    }

    @SuppressWarnings({ "IfStatementWithTooManyBranches", "OverlyComplexMethod", "OverlyLongMethod", "SpellCheckingInspection",
            "SwitchStatementWithTooManyBranches" })
    private void parseInput(String line) throws IOException {
        if (line.startsWith(">") && line.contains(":")) {
            String[] parts = line.split(":", 2);
            @NonNls String cmd = parts[0].substring(1);
            String argument = parts[1];
            switch (cmd) {
                case "BYTECOUNT":
                    processByteCount(argument);
                    break;
                case "FATAL":
                    // TODO
                    LOGGER.error(argument);
                    break;
                case "HOLD":
                    processHold(argument);
                    break;
                case "INFO":
                    // Ignore greeting from management
                    break;
                case "INFOMSG":
                    // Undocumented real-time message
                    break;
                case "LOG":
                    processLog(argument);
                    break;
                case "PASSWORD":
                    processPassword(argument);
                    break;
                case "STATE":
                    processState(argument);
                    break;
                case "UPDOWN":
                    // Ignore
                    break;
                case "CLIENT":
                case "ECHO":
                case "NEED-OK":
                case "NEED-STR":
                case "PK_SIGN":
                case "PROXY":
                case "RSA_SIGN":
                    throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
                default:
                    LOGGER.error("Got unrecognized argument: {}", argument);
                    break;
            }
        } else if (line.startsWith(Strings.SUCCESS_PREFIX)) {
            LOGGER.info(line);
        } else if (line.startsWith(Strings.ERROR_PREFIX)) {
            // TODO
            LOGGER.error(line);
            // throw new IOException("Stream closed");
        } else {
            LOGGER.error("Got unrecognized line: {}", line);
        }
    }

    private void processByteCount(String argument) {
        int comma = argument.indexOf(",");
        Long in = Long.valueOf(argument.substring(0, comma));
        Long out = Long.valueOf(argument.substring(comma + 1));
        ByteCount byteCount = new ByteCount(in, out);
        mByteCountManager.setByteCount(byteCount);
    }

    private void processHold(String argument) throws IOException {
        // Close connection if AUTH has failed
        if (argument.startsWith(Strings.WAITING_FOR_HOLD_RELEASE_PREFIX) && (mLastLevel == ConnectionStatus.LEVEL_AUTH_FAILED)) {
            LOGGER.error("Verification Error");
            throw new IOException(STREAM_CLOSED);
        }
    }

    @SuppressWarnings("OverlyLongMethod")
    private void processLog(String argument) {
        String[] args = argument.split(",", 3);
        String date = args[0];
        @NonNls String level = args[1];
        String message = args[2];
        LogLevel logLevel;
        switch (level) {
            case "I":
                // I -- informational
                logLevel = LogLevel.INFO;
                break;
            case "F":
            case "N":
                // F -- fatal error
                // N -- non-fatal error
                logLevel = LogLevel.ERROR;
                break;
            case "W":
                // W -- warning
                logLevel = LogLevel.WARNING;
                break;
            case "D":
                // D -- debug
                logLevel = LogLevel.DEBUG;
                break;
            default:
                logLevel = LogLevel.VERBOSE;
                break;
        }
        if (message.startsWith(Strings.MANAGEMENT_CMD_PREFIX)) {
            logLevel = LogLevel.VERBOSE;
        } else if (message.startsWith(Strings.WARNING_PREFIX)) {
            logLevel = LogLevel.WARNING;
            message = message.substring(Strings.WARNING_PREFIX.length() + 1);
        } else if (message.startsWith(Strings.NOTE_PREFIX)) {
            message = message.substring(Strings.NOTE_PREFIX.length() + 1);
        }
        Log log = new Log(date, logLevel, message);
        mLogManager.setLog(log);
    }

    private void processPassword(String argument) throws IOException {
        // Ignore Auth token message, already managed by OpenVPN itself
        if (argument.startsWith(Strings.AUTH_TOKEN_PREFIX)) {
            return;
        }
        if (argument.startsWith(Strings.VERIFICATION_FAILED_PREFIX)) {
            return;
        }
        if (argument.startsWith(Strings.NEED_PREFIX)) {
            String s = "\'";
            int p1 = argument.indexOf(s);
            int p2 = argument.indexOf(s, p1 + 1);
            @NonNls String type = argument.substring(p1 + 1, p2);
            LOGGER.info("OpenVPN requires Authentication type {}", type);
            String handlerUsername = null;
            String handlerPassword = null;
            UsernamePasswordHandler handler = mUsernamePasswordHandler;
            if (handler != null) {
                handlerUsername = handler.getUserName();
                handlerPassword = handler.getUserPass();
            }

            /*
             *  simulate service restart
             *  handler = null;
             *  handlerUsername = handler.getUserName();
             */
            String username = StringUtils.isBlank(handlerUsername) ? "..." : StringUtils.escapeOpenVPN(handlerUsername);
            String password = StringUtils.isBlank(handlerPassword) ? "..." : StringUtils.escapeOpenVPN(handlerPassword);
            if ("Auth".equals(type)) {
                managementCommand(String.format(Locale.ROOT, Commands.USERNAME_COMMAND, type, username));
                managementCommand(String.format(Locale.ROOT, Commands.PASSWORD_COMMAND, type, password));
            } else if ("Private Key".equals(type)) {
                managementCommand(String.format(Locale.ROOT, Commands.PASSWORD_COMMAND, type, password));
            } else {
                throw new UnsupportedOperationException(NOT_SUPPORTED_YET);
            }
        }
    }

    private void processState(String argument) {
        String[] args = argument.split(",", -1);
        State state = new State(args[0], args[1], args[2], args[3], args[4], args[5]);
        String name = state.getName();
        String message = state.getMessage();
        // Workaround for OpenVPN doing AUTH and WAIT while being connected, simply ignore these state
        if ((mLastLevel == ConnectionStatus.LEVEL_CONNECTED) && (VpnStatus.WAIT.equals(name) || VpnStatus.AUTH.equals(name))) {
            LOGGER.info("Ignoring OpenVPN Status in CONNECTED state ({}->{}): {}", name, mLastLevel, message);
        } else {
            mStateManager.setState(state);
            mLastLevel = VpnStatus.getLevel(name, message);
            LOGGER.info("New OpenVPN Status ({}->{}): {}", name, mLastLevel, message);
        }
    }
}
