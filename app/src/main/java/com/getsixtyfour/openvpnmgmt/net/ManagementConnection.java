package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.core.ConnectionStatus;
import com.getsixtyfour.openvpnmgmt.core.LogLevel;
import com.getsixtyfour.openvpnmgmt.core.TrafficHistory;
import com.getsixtyfour.openvpnmgmt.core.VpnStatus;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnStatus;
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
 * @author Arne Schwabe
 * @author 1951FDG
 */

@SuppressWarnings({ "Singleton", "OverlyCoupledClass", "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class ManagementConnection extends AbstractConnection implements Connection {

    public static final Integer BYTE_COUNT_INTERVAL = 2;

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementConnection.class);

    private static volatile @Nullable ManagementConnection sInstance = null;

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

    @SuppressWarnings({ "DoubleCheckedLocking", "SynchronizeOnThis" })
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
    public void connect(@NotNull String host, @NotNull Integer port) throws IOException {
        connect(host, port, null);
    }

    @Override
    public void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password) throws IOException {
        if (isConnected()) {
            return;
        }
        //noinspection OverlyBroadCatchBlock
        try {
            super.connect(host, port, password);
            // Ensures state listeners are notified of current state if OpenVPN is already connected
            String result = executeCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, ""));
            String[] lines = result.split(System.lineSeparator());
            String line = (lines.length >= 1) ? lines[lines.length - 1] : "";
            if (!line.isEmpty() && !line.contains(VpnStatus.AUTH_FAILURE)) {
                processState(line);
            }
            onConnected();
        } catch (IOException e) {
            onConnectError(e);
            throw e;
        } catch (Exception e) {
            onConnectError(e);
            throw new IOException(e);
        }
    }

    @Override
    public void disconnect() {
        if (!isConnected()) {
            return;
        }
        super.disconnect();
        onDisconnected();
    }

    @Override
    public boolean isConnected() {
        return super.isConnected();
    }

    @Override
    @SuppressWarnings({ "NestedAssignment", "MagicNumber" })
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
    protected @NotNull Logger getLogger() {
        return LOGGER;
    }

    @Override
    public @NotNull String getManagementVersion() throws IOException {
        String result = executeCommand(Commands.VERSION_COMMAND);
        String[] lines = result.split(System.lineSeparator());
        String line = (lines.length >= 1) ? lines[lines.length - 1] : "";
        if (!line.isEmpty() && line.startsWith(Constants.MANAGEMENT_VERSION_PREFIX)) {
            return line.substring(Constants.MANAGEMENT_VERSION_PREFIX.length() + 1);
        }
        return "";
    }

    @Override
    public @NotNull Status getVpnStatus() throws IOException {
        try {
            String output = executeCommand(Commands.STATUS_COMMAND);
            OpenVpnStatus ovs = new OpenVpnStatus();
            ovs.setCommandOutput(output);
            return ovs;
        } catch (OpenVpnParseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public @NotNull String getVpnVersion() throws IOException {
        String result = executeCommand(Commands.VERSION_COMMAND);
        String[] lines = result.split(System.lineSeparator());
        String line = (lines.length >= 2) ? lines[lines.length - 2] : "";
        if (!line.isEmpty() && line.startsWith(Constants.OPEN_VPN_VERSION_PREFIX)) {
            return line.substring(Constants.OPEN_VPN_VERSION_PREFIX.length() + 1);
        }
        return "";
    }

    @Override
    public boolean isVpnActive() {
        return (mLastLevel != ConnectionStatus.LEVEL_NOT_CONNECTED) && (mLastLevel != ConnectionStatus.LEVEL_AUTH_FAILED);
    }

    @Override
    public void stopVpn() throws IOException {
        managementCommand(String.format(Locale.ROOT, Commands.SIGNAL_COMMAND, Constants.ARG_SIGTERM));
    }
    @SuppressWarnings({ "NestedAssignment", "ThrowSpecificExceptions" })
    @Override
    public void run() {
        if (!isConnected()) {
            // UncheckedIOException requires Android N
            //noinspection ProhibitedExceptionThrown
            throw new RuntimeException(new IOException(Constants.SOCKET_IS_NOT_CONNECTED));
        }

        try {
            managementCommand(String.format(Locale.ROOT, Commands.BYTECOUNT_COMMAND, BYTE_COUNT_INTERVAL));
            managementCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, Constants.ARG_ON));
            managementCommand(String.format(Locale.ROOT, Commands.LOG_COMMAND, Constants.ARG_ON));
            managementCommand(String.format(Locale.ROOT, Commands.HOLD_COMMAND, Constants.ARG_RELEASE));
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
        LOGGER.info("TERMINATED");

        Thread thread = Thread.currentThread();
        Thread.UncaughtExceptionHandler eh = thread.getUncaughtExceptionHandler();
        if ((eh != null) && !(eh instanceof ThreadGroup)) {
            eh.uncaughtException(thread, new ThreadDeath());
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

    private void dispatchOnByteCountChanged(TrafficHistory.TrafficDataPoint tdp) {
        TrafficHistory.LastDiff diff = mTrafficHistory.add(tdp);
        for (OnByteCountChangedListener listener : mByteCountChangedListeners) {
            listener.onByteCountChanged(tdp.mInBytes, tdp.mOutBytes, diff.getDiffIn(), diff.getDiffOut());
        }
    }

    private void dispatchOnRecordChanged(OpenVpnLogRecord record) {
        for (OnRecordChangedListener listener : mRecordChangedListeners) {
            listener.onRecordChanged(record);
        }
    }

    private void dispatchOnStateChanged(OpenVpnNetworkState state) {
        for (OnStateChangedListener listener : mStateChangedListeners) {
            listener.onStateChanged(state);
        }
    }

    private void managementCommand(String command) throws IOException {
        if (!isConnected()) {
            throw new IOException(Constants.SOCKET_IS_NOT_CONNECTED);
        }
        BufferedWriter out = getBufferedWriter();
        out.write(command);
        out.newLine();
        out.flush();
    }

    private void onConnectError(@NotNull Throwable e) {
        if ((e instanceof IllegalArgumentException) || (e instanceof IOException)) {
            LOGGER.error(e.getMessage());
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

    @SuppressWarnings({ "IfStatementWithTooManyBranches", "ProhibitedExceptionCaught" })
    private void parseInput(String line) throws IOException {
        if (line.startsWith(">") && line.contains(":")) {
            try {
                process(line);
            } catch (UnsupportedOperationException e) {
                LOGGER.error("Got unsupported line: {}", line);
                throw new IOException(e);
            } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | NumberFormatException e) {
                LOGGER.error("Could not parse line: {}", line);
                throw new IOException(e);
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
    private void process(String line) throws IOException {
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
                LOGGER.error("Got unrecognized command: {}", cmd);
                break;
        }
    }

    private void processByteCount(String argument) {
        int comma = argument.indexOf(",");
        long in = Long.parseLong(argument.substring(0, comma));
        long out = Long.parseLong(argument.substring(comma + 1));
        TrafficHistory.TrafficDataPoint tdp = new TrafficHistory.TrafficDataPoint(in, out, 0L);
        dispatchOnByteCountChanged(tdp);
    }

    private void processHold(String argument) throws IOException {
        // Close connection if AUTH has failed
        if ((mLastLevel == ConnectionStatus.LEVEL_AUTH_FAILED) && argument.startsWith(Constants.WAITING_FOR_HOLD_RELEASE_PREFIX)) {
            LOGGER.error("Verification Error");
            throw new IOException(Constants.STREAM_CLOSED);
        }
    }

    @SuppressWarnings("OverlyLongMethod")
    private void processLog(String argument) {
        String[] args = argument.split(",", 3);
        String time = args[0];
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
            case "":
                logLevel = LogLevel.VERBOSE;
                break;
            default:
                LOGGER.error("Unknown log level {} for message {}", level, message);
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

    private void processPassword(String argument) throws IOException {
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

    private void processState(String argument) {
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
