package com.getsixtyfour.openvpnmgmt.core;

import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.listeners.ConnectionListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnRecordChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnStateChangedListener;
import com.getsixtyfour.openvpnmgmt.model.OpenVpnLogRecord;
import com.getsixtyfour.openvpnmgmt.model.OpenVpnNetworkState;
import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "Singleton", "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class ManagementConnection extends AbstractConnection implements Connection {

    public static final Integer BYTE_COUNT_INTERVAL = 2;

    @NonNls
    private static final @NotNull Logger LOGGER = LoggerFactory.getLogger(ManagementConnection.class);

    @Nullable
    private static volatile ManagementConnection sInstance = null;

    private final CopyOnWriteArraySet<OnByteCountChangedListener> mByteCountChangedListeners;

    private final CopyOnWriteArraySet<OnRecordChangedListener> mRecordChangedListeners;

    private final CopyOnWriteArraySet<OnStateChangedListener> mStateChangedListeners;

    private final TrafficHistory mTrafficHistory;

    private @Nullable ConnectionListener mConnectionListener;

    private ConnectionStatus mLastLevel = ConnectionStatus.LEVEL_NOT_CONNECTED;

    private @Nullable UsernamePasswordHandler mUsernamePasswordHandler;

    private ManagementConnection() {
        mByteCountChangedListeners = new CopyOnWriteArraySet<>();
        mRecordChangedListeners = new CopyOnWriteArraySet<>();
        mStateChangedListeners = new CopyOnWriteArraySet<>();
        mTrafficHistory = new TrafficHistory();
    }

    @SuppressWarnings({ "SynchronizeOnThis" })
    public static @NotNull ManagementConnection getInstance() {
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
    public boolean addOnByteCountChangedListener(@NotNull OnByteCountChangedListener listener) {
        return mByteCountChangedListeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public boolean removeOnByteCountChangedListener(@NotNull OnByteCountChangedListener listener) {
        return mByteCountChangedListeners.remove(Objects.requireNonNull(listener));
    }

    @Override
    public void clearOnByteCountChangedListeners() {
        mByteCountChangedListeners.clear();
    }

    @Override
    public boolean addOnRecordChangedListener(@NotNull OnRecordChangedListener listener) {
        return mRecordChangedListeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public boolean removeOnRecordChangedListener(@NotNull OnRecordChangedListener listener) {
        return mRecordChangedListeners.remove(Objects.requireNonNull(listener));
    }

    @Override
    public void clearOnRecordChangedListeners() {
        mRecordChangedListeners.clear();
    }

    @Override
    public boolean addOnStateChangedListener(@NotNull OnStateChangedListener listener) {
        return mStateChangedListeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public boolean removeOnStateChangedListener(@NotNull OnStateChangedListener listener) {
        return mStateChangedListeners.remove(Objects.requireNonNull(listener));
    }

    @Override
    public void clearOnStateChangedListeners() {
        mStateChangedListeners.clear();
    }

    @Override
    public void connect(@NotNull String host, @NotNull Integer port) {
        connect(host, port, null);
    }

    @Override
    public void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password) {
        if (isConnected()) {
            return;
        }
        try {
            super.connect(host, port, password);
        } catch (IOException e) {
            onConnectError(e);
            return;
        }
        try {
            // Ensures state listeners are notified of current state if OpenVPN is already connected
            String output = executeCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, ""));
            String[] lines = output.split(System.lineSeparator());
            String line = (lines.length >= 1) ? lines[lines.length - 1] : "";
            if (!line.isEmpty() && !line.contains(VpnStatus.AUTH_FAILURE)) {
                processState(line);
            }
        } catch (IOException e) {
            onConnectError(e);
            return;
        }
        onConnected();
    }

    @Override
    public void disconnect() {
        if (!isConnected()) {
            return;
        }
        try {
            super.disconnect();
        } catch (IOException ignored) {
        }
        onDisconnected();
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    @SuppressWarnings({ "NestedAssignment" })
    public @NotNull String executeCommand(@NotNull String command) throws IOException {
        if (!isConnected()) {
            throw new IOException(Constants.SOCKET_IS_NOT_CONNECTED);
        }
        StringBuilder sb = new StringBuilder(256);
        BufferedWriter out = getBufferedWriter();
        out.write(command);
        out.newLine();
        out.flush();
        BufferedReader in = getBufferedReader();
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

    @Override
    public void managementCommand(@NotNull String command) throws IOException {
        if (!isConnected()) {
            throw new IOException(Constants.SOCKET_IS_NOT_CONNECTED);
        }
        BufferedWriter out = getBufferedWriter();
        out.write(command);
        out.newLine();
        out.flush();
    }

    @Override
    protected @NotNull Logger getLogger() {
        return LOGGER;
    }

    @Override
    public boolean isVpnActive() {
        return (mLastLevel != ConnectionStatus.LEVEL_NOT_CONNECTED) && (mLastLevel != ConnectionStatus.LEVEL_AUTH_FAILED);
    }

    @SuppressWarnings({ "NestedAssignment", "ThrowSpecificExceptions" })
    @Override
    public void run() {
        if (!isConnected()) {
            // UncheckedIOException requires Android N
            //noinspection ProhibitedExceptionThrown
            throw new RuntimeException(new IOException(Constants.SOCKET_IS_NOT_CONNECTED));
        }
        Thread t = Thread.currentThread();
        LOGGER.info("OpenVPN Management started in background thread: \"{}\"", t.getName());
        try {
            managementCommand(String.format(Locale.ROOT, Commands.BYTECOUNT_COMMAND, BYTE_COUNT_INTERVAL));
            managementCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, Commands.ARG_ON));
            managementCommand(String.format(Locale.ROOT, Commands.LOG_COMMAND, Commands.ARG_ON));
            managementCommand(String.format(Locale.ROOT, Commands.HOLD_COMMAND, Commands.ARG_RELEASE));
            BufferedReader in = getBufferedReader();
            @NonNls String line;
            while ((line = in.readLine()) != null) {
                if (!line.isEmpty()) {
                    // LOGGER.info("Read from socket line: {}", line);
                    parseInput(line);
                }
            }
        } catch (IOException e) {
            if (!Constants.STREAM_CLOSED.equals(e.getMessage())) {
                LOGGER.error("", e);
            }
        }
        LOGGER.info("OpenVPN Management stopped in background thread: \"{}\"", t.getName());
        Thread.UncaughtExceptionHandler eh = t.getUncaughtExceptionHandler();
        if ((eh != null) && !(eh instanceof ThreadGroup)) {
            eh.uncaughtException(t, new ThreadDeath());
        }
    }

    @Override
    public void setConnectionListener(@Nullable ConnectionListener connectionListener) {
        mConnectionListener = connectionListener;
    }

    @Override
    public void setUsernamePasswordHandler(@Nullable UsernamePasswordHandler handler) {
        mUsernamePasswordHandler = handler;
    }

    private void dispatchOnByteCountChanged(@NotNull TrafficHistory.TrafficDataPoint tdp) {
        TrafficHistory.LastDiff diff = mTrafficHistory.add(tdp);
        for (OnByteCountChangedListener listener : mByteCountChangedListeners) {
            listener.onByteCountChanged(tdp.mInBytes, tdp.mOutBytes, diff.getDiffIn(), diff.getDiffOut());
        }
    }

    private void dispatchOnRecordChanged(@NotNull OpenVpnLogRecord record) {
        for (OnRecordChangedListener listener : mRecordChangedListeners) {
            listener.onRecordChanged(record);
        }
    }

    private void dispatchOnStateChanged(@NotNull OpenVpnNetworkState state) {
        for (OnStateChangedListener listener : mStateChangedListeners) {
            listener.onStateChanged(state);
        }
    }

    private void onConnectError(@NotNull Throwable e) {
        if ((e instanceof IllegalArgumentException) || (e instanceof IOException)) {
            LOGGER.error("", e);
        } else {
            LOGGER.error("Unknown exception thrown:", e);
        }
        ConnectionListener listener = mConnectionListener;
        if (listener != null) {
            listener.onConnectError(Thread.currentThread(), e);
        }
    }

    private void onConnected() {
        LOGGER.info("Connected");
        ConnectionListener listener = mConnectionListener;
        if (listener != null) {
            listener.onConnected(Thread.currentThread());
        }
    }

    private void onDisconnected() {
        LOGGER.info("Disconnected");
        ConnectionListener listener = mConnectionListener;
        if (listener != null) {
            listener.onDisconnected(Thread.currentThread());
        }
    }

    @SuppressWarnings({ "IfStatementWithTooManyBranches", "ProhibitedExceptionCaught" })
    private void parseInput(@NotNull String line) throws IOException {
        if (line.startsWith(">") && line.contains(":")) {
            try {
                process(line);
            } catch (UnsupportedOperationException e) {
                LOGGER.error("Got unsupported line: {}", line);
                throw new IOException(e);
            } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | NumberFormatException e) {
                LOGGER.error("Could not parse line: {}", line);
                throw e;
            }
        } else if (line.startsWith(Constants.SUCCESS_PREFIX)) {
            LOGGER.info(line.substring(Constants.SUCCESS_PREFIX.length() + 1));
        } else if (line.startsWith(Constants.ERROR_PREFIX)) {
            // TODO:
            LOGGER.error(line.substring(Constants.ERROR_PREFIX.length() + 1));
            // throw new IOException(STREAM_CLOSED);
        } else if (line.startsWith(Constants.ENTER_PASSWORD_PREFIX)) {
            parseInput(line.substring(Constants.ENTER_PASSWORD_PREFIX.length()));
        } else {
            LOGGER.error("Got unrecognized line: {}", line);
        }
    }

    @SuppressWarnings({ "OverlyComplexMethod", "OverlyLongMethod", "SwitchStatementWithTooManyBranches", "SpellCheckingInspection" })
    private void process(@NotNull String line) throws IOException {
        String[] parts = line.split(":", 2);
        @NonNls String cmd = parts[0].substring(1);
        String argument = parts[1];
        switch (cmd) {
            case "BYTECOUNT":
                processByteCount(argument);
                break;
            case "FATAL":
                // TODO:
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
                throw new UnsupportedOperationException(Constants.NOT_SUPPORTED_YET);
            default:
                LOGGER.error("Got unrecognized command: {}", cmd); //NON-NLS
                break;
        }
    }

    private void processByteCount(@NotNull String argument) {
        int comma = argument.indexOf(",");
        long in = Long.parseLong(argument.substring(0, comma));
        long out = Long.parseLong(argument.substring(comma + 1));
        TrafficHistory.TrafficDataPoint tdp = new TrafficHistory.TrafficDataPoint(in, out, 0L);
        dispatchOnByteCountChanged(tdp);
    }

    private void processHold(@NotNull String argument) throws IOException {
        // Close connection if AUTH has failed
        if ((mLastLevel == ConnectionStatus.LEVEL_AUTH_FAILED) && argument.startsWith(Constants.WAITING_FOR_HOLD_RELEASE_PREFIX)) {
            LOGGER.error("Verification Error");
            throw new IOException(Constants.STREAM_CLOSED);
        }
    }

    @SuppressWarnings("OverlyLongMethod")
    private void processLog(@NotNull String argument) {
        String[] args = argument.split(",", 3);
        @NonNls String time = args[0];
        @NonNls String level = args[1];
        @NonNls String message = args[2];
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
            case "":
                logLevel = LogLevel.VERBOSE;
                break;
            default:
                LOGGER.error("Unknown log level {} for message {}", level, message); //NON-NLS
                logLevel = LogLevel.UNKNOWN;
                break;
        }
        if (message.startsWith(Constants.MANAGEMENT_CMD_PREFIX)) {
            logLevel = LogLevel.VERBOSE;
        } else if (message.startsWith(Constants.WARNING_PREFIX)) {
            logLevel = LogLevel.WARNING;
            message = message.substring(Constants.WARNING_PREFIX.length() + 1);
        } else if (message.startsWith(Constants.NOTE_PREFIX)) {
            message = message.substring(Constants.NOTE_PREFIX.length() + 1);
        }
        OpenVpnLogRecord record = new OpenVpnLogRecord(time, logLevel, message);
        dispatchOnRecordChanged(record);
    }

    private void processPassword(@NotNull String argument) throws IOException {
        // Ignore Auth token message, already managed by OpenVPN itself
        if (argument.startsWith(Constants.AUTH_TOKEN_PREFIX)) {
            return;
        }
        if (argument.startsWith(Constants.VERIFICATION_FAILED_PREFIX)) {
            return;
        }
        if (argument.startsWith(Constants.NEED_PREFIX)) {
            String s = "'";
            int p1 = argument.indexOf(s);
            int p2 = argument.indexOf(s, p1 + 1);
            @NonNls String type = argument.substring(p1 + 1, p2);
            LOGGER.info("OpenVPN requires Authentication type {}", type);
            CharSequence strUsername = null;
            CharSequence strPassword = null;
            UsernamePasswordHandler handler = mUsernamePasswordHandler;
            if (handler != null) {
                strUsername = handler.getUser();
                strPassword = handler.getPassword();
            }
            String ellipsis = "...";
            CharSequence username = StringUtils.defaultIfBlank(StringUtils.escapeString(strUsername), ellipsis);
            CharSequence password = StringUtils.defaultIfBlank(StringUtils.escapeString(strPassword), ellipsis);
            if ("Auth".equals(type)) {
                managementCommand(String.format(Locale.ROOT, Commands.USERNAME_COMMAND, type, username));
                managementCommand(String.format(Locale.ROOT, Commands.PASSWORD_COMMAND, type, password));
            } else if ("Private Key".equals(type)) {
                managementCommand(String.format(Locale.ROOT, Commands.PASSWORD_COMMAND, type, password));
            } else {
                throw new UnsupportedOperationException(Constants.NOT_SUPPORTED_YET);
            }
        }
    }

    private void processState(@NotNull String argument) {
        String[] args = argument.split(",", -1);
        OpenVpnNetworkState state = new OpenVpnNetworkState(args[0], args[1], args[2], args[3], args[4], args[5]);
        String name = state.getName();
        String message = state.getDescription();
        // Workaround for OpenVPN doing AUTH and WAIT while being connected, simply ignore these state
        if ((mLastLevel == ConnectionStatus.LEVEL_CONNECTED) && (VpnStatus.WAIT.equals(name) || VpnStatus.AUTH.equals(name))) {
            LOGGER.info("Ignoring OpenVPN Status in CONNECTED state ({}->{}): {}", name, mLastLevel, message);
        } else {
            dispatchOnStateChanged(state);
            mLastLevel = VpnStatus.getLevel(name, message);
            LOGGER.info("New OpenVPN Status ({}->{}): {}", name, mLastLevel, message);
        }
    }
}
