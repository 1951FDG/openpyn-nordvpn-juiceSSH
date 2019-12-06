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

    private static volatile ManagementConnection mInstance = null;

    private static final String ARG_INTERACT = "interact";

    private static final String ARG_ON = " on";

    private static final String ARG_RELEASE = " release";

    private static final String ARG_SIGTERM = "SIGTERM";

    public static final Integer BYTE_COUNT_INTERVAL = 2;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementConnection.class);

    @NonNls
    public static final String NOT_SUPPORTED_YET = "Not supported yet";

    @NonNls
    public static final String SOCKET_IS_NOT_CONNECTED = "Socket is not connected";

    @NonNls
    public static final String STREAM_CLOSED = "Stream closed";

    private final ByteCountManager mByteCountManager = new ByteCountManager();

    private final LogManager mLogManager = new LogManager();

    private final StateManager mStateManager = new StateManager();

    private boolean isRunning = false;

    private ConnectionListener mConnectionListener;

    private ConnectionStatus mLastLevel = ConnectionStatus.LEVEL_NOT_CONNECTED;

    private UsernamePasswordHandler mUsernamePasswordHandler;

    @NotNull
    @SuppressWarnings({ "DoubleCheckedLocking", "SynchronizeOnThis" })
    public static ManagementConnection getInstance() {
        if (mInstance == null) {
            synchronized (ManagementConnection.class) {
                if (mInstance == null) {
                    mInstance = new ManagementConnection();
                }
            }
        }
        return mInstance;
    }

    private ManagementConnection() {
    }

    @Override
    public void addByteCountListener(@NotNull ByteCountListener listener) {
        mByteCountManager.addListener(Objects.requireNonNull(listener));
    }

    @Override
    public void addLogListener(@NotNull LogListener listener) {
        mLogManager.addListener(Objects.requireNonNull(listener));
    }

    @Override
    public void addStateListener(@NotNull StateListener listener) {
        mStateManager.addListener(Objects.requireNonNull(listener));
    }

    // todo safe split
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
        {
            String result = executeCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, ""));
            String[] lines = result.split(System.lineSeparator());
            String argument = lines[lines.length - 1];
            if (!argument.contains(VpnStatus.AUTH_FAILURE)) {
                processState(argument);
            }
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();
        onDisconnected();
    }

    @NotNull
    @SuppressWarnings({ "NestedAssignment", "MethodCallInLoopCondition" })
    @Override
    public String executeCommand(@NotNull String command) throws IOException {
        if (!isConnected()) {
            throw new IOException(SOCKET_IS_NOT_CONNECTED);
        }
        // todo measure number of chars during usages
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
        String line = lines[lines.length - 2];
        if (line.startsWith(Strings.OPEN_VPN_VERSION_PREFIX)) {
            return line.substring(Strings.OPEN_VPN_VERSION_PREFIX.length() + 1);
        }
        return "";
    }

    @Override
    public boolean isOpenVPNActive() {
        return (mLastLevel != ConnectionStatus.LEVEL_NOT_CONNECTED) && (mLastLevel != ConnectionStatus.LEVEL_AUTH_FAILED);
    }

    @Override
    public void removeByteCountListener(@NotNull ByteCountListener listener) {
        mByteCountManager.removeListener(Objects.requireNonNull(listener));
    }

    @Override
    public void removeLogListener(@NotNull LogListener listener) {
        mLogManager.removeListener(Objects.requireNonNull(listener));
    }

    @Override
    public void removeStateListener(@NotNull StateListener listener) {
        mStateManager.removeListener(Objects.requireNonNull(listener));
    }

    @SuppressWarnings({ "NestedAssignment", "MethodCallInLoopCondition", "ProhibitedExceptionThrown" })
    @Override
    public void run() {
        if (!isConnected()) {
            // UncheckedIOException requires Android N
            throw new RuntimeException(new IOException(SOCKET_IS_NOT_CONNECTED));
        }
        isRunning = true;
        {
            try {
                managementCommand(String.format(Locale.ROOT, Commands.AUTH_COMMAND, ARG_INTERACT));
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
            LOGGER.info("TERMINATED");
            disconnect();
        }
        isRunning = false;
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
        if (!isRunning) {
            disconnect();
        }
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

    // todo see all magic characters, cosntants
    @SuppressWarnings({ "IfStatementWithTooManyBranches", "OverlyComplexMethod", "OverlyLongMethod", "SpellCheckingInspection",
            "SwitchStatementWithTooManyBranches" })
    private void parseInput(String line) throws IOException {
        if ((Character.compare(line.charAt(0), '>') == 0) && line.contains(":")) {
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
        int comma = argument.indexOf(',');
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
            char ch = '\'';
            int p1 = argument.indexOf(ch);
            int p2 = argument.indexOf(ch, p1 + 1);
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
