package com.getsixtyfour.openvpnmgmt.core;

import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.listeners.ConnectionStateListener;
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

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementConnection.class);

    public static final Integer BYTE_COUNT_INTERVAL = 2;

    public static final int SOCKET_CONNECT_TIMEOUT = 1000;

    /**
     * Maximum time to wait on Socket.getInputStream().read() (in milliseconds)
     */
    public static final int SOCKET_READ_TIMEOUT = 2000;

    private static volatile @Nullable ManagementConnection sInstance = null;

    private final CopyOnWriteArraySet<OnByteCountChangedListener> mByteCountChangedListeners;

    private final CopyOnWriteArraySet<OnRecordChangedListener> mRecordChangedListeners;

    private final CopyOnWriteArraySet<OnStateChangedListener> mStateChangedListeners;

    private final TrafficHistory mTrafficHistory;

    private @Nullable AuthenticationHandler mAuthenticationHandler;

    private @Nullable ConnectionStateListener mConnectionStateListener;

    private ConnectionStatus mStatus = ConnectionStatus.LEVEL_NOT_CONNECTED;

    private ManagementConnection() {
        mByteCountChangedListeners = new CopyOnWriteArraySet<>();
        mRecordChangedListeners = new CopyOnWriteArraySet<>();
        mStateChangedListeners = new CopyOnWriteArraySet<>();
        mTrafficHistory = new TrafficHistory();
    }

    @SuppressWarnings("SynchronizeOnThis")
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
    public void connect(@NotNull String host, @NotNull Integer port) throws IOException {
        connect(host, port, null);
    }

    @Override
    public void connect(@NotNull String host, @NotNull Integer port, @Nullable char[] password) throws IOException {
        if (isConnected()) {
            return;
        }
        super.connect(host, port, password);
        onConnect();
    }

    @Override
    public void disconnect() throws IOException {
        if (!isConnected()) {
            return;
        }
        super.disconnect();
        onDisconnect();
    }

    @Override
    public void sendCommand(@NotNull String command) throws IOException {
        if (!isConnected()) {
            throw new IOException(Constants.MSG_SOCKET_IS_NOT_CONNECTED);
        }
        BufferedWriter out = getSocketOutputStream();
        out.write(command);
        out.newLine();
        out.flush();
    }

    @Override
    public @NotNull String sendCommand(@NotNull String command, int timeoutMillis) throws IOException {
        if (!isConnected()) {
            throw new IOException(Constants.MSG_SOCKET_IS_NOT_CONNECTED);
        }
        sendCommand(command);
        getSocket().setSoTimeout(timeoutMillis);
        StringBuilder sb = new StringBuilder(256);
        BufferedReader in = getSocketInputStream();
        while (!Thread.currentThread().isInterrupted()) {
            @NonNls String line = in.readLine();
            // End of stream
            if (line == null) {
                break;
            }
            // For commands which print multiple lines of output, the last line will be "END"
            if ("END".equals(line)) {
                break;
            }
            if (!line.isEmpty()) {
                // LOGGER.info("Read from socket line: {}", line);
                sb.append(line);
                sb.append(System.lineSeparator());
            }
        }
        getSocket().setSoTimeout(getSocketReadTimeout());
        return sb.toString();
    }

    @Override
    public void start(@NotNull String host, @NotNull Integer port, @Nullable char[] password) {
        try {
            connect(host, port, password);
            // Ensures state listeners are notified of current state if OpenVPN is already connected
            String output = sendCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, ""), SOCKET_READ_TIMEOUT);
            String[] lines = output.split(System.lineSeparator());
            String argument = (lines.length >= 1) ? lines[lines.length - 1] : "";
            if (!argument.isEmpty() && !argument.contains(VpnStatus.AUTH_FAILURE)) {
                processState(argument);
            }
            sendCommand(String.format(Locale.ROOT, Commands.BYTECOUNT_COMMAND, BYTE_COUNT_INTERVAL));
            sendCommand(String.format(Locale.ROOT, Commands.STATE_COMMAND, Commands.ARG_ON));
            sendCommand(String.format(Locale.ROOT, Commands.LOG_COMMAND, Commands.ARG_ON));
            sendCommand(String.format(Locale.ROOT, Commands.HOLD_COMMAND, Commands.ARG_RELEASE));
            BufferedReader in = getSocketInputStream();
            while (!Thread.currentThread().isInterrupted()) {
                @NonNls String line = in.readLine();
                // End of stream
                if (line == null) {
                    break;
                }
                if (!line.isEmpty()) {
                    // LOGGER.info("Read from socket line: {}", line);
                    parseInput(line);
                }
            }
            throw new ThreadDeath();
        } catch (IOException e) {
            // UncheckedIOException requires Android N
            //noinspection ProhibitedExceptionThrown
            throw new RuntimeException(e);
        } finally {
            stop();
        }
    }

    @Override
    public void stop() {
        try {
            disconnect();
        } catch (IOException ignored) {
        }
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
    public void setAuthenticationHandler(@Nullable AuthenticationHandler handler) {
        mAuthenticationHandler = handler;
    }

    @Override
    public void setConnectionStateListener(@Nullable ConnectionStateListener listener) {
        mConnectionStateListener = listener;
    }

    @Override
    public @NotNull ConnectionStatus getStatus() {
        return mStatus;
    }

    @Override
    protected @NotNull Logger getLogger() {
        return LOGGER;
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

    private void onConnect() {
        ConnectionStateListener listener = mConnectionStateListener;
        if (listener != null) {
            listener.onConnect(Thread.currentThread());
        }
    }

    private void onDisconnect() {
        ConnectionStateListener listener = mConnectionStateListener;
        if (listener != null) {
            listener.onDisconnect(Thread.currentThread());
        }
    }

    // TODO: add line info to exception messages
    @SuppressWarnings({ "IfStatementWithTooManyBranches", "ProhibitedExceptionCaught" })
    private void parseInput(@NotNull String line) throws IOException {
        if (line.startsWith(">") && line.contains(":")) {
            try {
                process(line);
            } catch (UnsupportedOperationException e) {
                LOGGER.error("Got unsupported line: {}", line);
                throw e;
            } catch (ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException | NumberFormatException e) {
                LOGGER.error("Could not parse line: {}", line);
                throw e;
            }
        } else if (line.startsWith(Constants.PREFIX_SUCCESS)) {
            LOGGER.info(line.substring(Constants.PREFIX_SUCCESS.length() + 1));
        } else if (line.startsWith(Constants.PREFIX_ERROR)) {
            // TODO:
            LOGGER.error(line.substring(Constants.PREFIX_ERROR.length() + 1));
        } else if (line.startsWith(Constants.PREFIX_ENTER_PASSWORD)) {
            parseInput(line.substring(Constants.PREFIX_ENTER_PASSWORD.length()));
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
                throw new UnsupportedOperationException(Constants.MSG_NOT_SUPPORTED_YET);
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

    private void processHold(@NotNull String argument) {
        // Close connection if AUTH has failed
        if ((mStatus == ConnectionStatus.LEVEL_AUTH_FAILED) && argument.startsWith(Constants.PREFIX_WAITING_FOR_HOLD_RELEASE)) {
            LOGGER.error("Verification Error");
            throw new IllegalStateException("AUTH has failed");
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
        if (message.startsWith(Constants.PREFIX_MANAGEMENT_CMD)) {
            logLevel = LogLevel.VERBOSE;
        } else if (message.startsWith(Constants.PREFIX_WARNING)) {
            logLevel = LogLevel.WARNING;
            message = message.substring(Constants.PREFIX_WARNING.length() + 1);
        } else if (message.startsWith(Constants.PREFIX_NOTE)) {
            message = message.substring(Constants.PREFIX_NOTE.length() + 1);
        }
        OpenVpnLogRecord record = new OpenVpnLogRecord(time, logLevel, message);
        dispatchOnRecordChanged(record);
    }

    private void processPassword(@NotNull String argument) throws IOException {
        // Ignore Auth token message, already managed by OpenVPN itself
        if (argument.startsWith(Constants.PREFIX_AUTH_TOKEN)) {
            return;
        }
        if (argument.startsWith(Constants.PREFIX_VERIFICATION_FAILED)) {
            return;
        }
        if (argument.startsWith(Constants.PREFIX_NEED)) {
            String s = "'";
            int p1 = argument.indexOf(s);
            int p2 = argument.indexOf(s, p1 + 1);
            @NonNls String type = argument.substring(p1 + 1, p2);
            LOGGER.info("OpenVPN requires Authentication type {}", type);
            CharSequence strUsername = null;
            CharSequence strPassword = null;
            AuthenticationHandler handler = mAuthenticationHandler;
            if (handler != null) {
                strUsername = handler.getUser();
                strPassword = handler.getPassword();
            }
            String ellipsis = "...";
            CharSequence username = StringUtils.defaultIfBlank(StringUtils.escapeString(strUsername), ellipsis);
            CharSequence password = StringUtils.defaultIfBlank(StringUtils.escapeString(strPassword), ellipsis);
            if ("Auth".equals(type)) {
                sendCommand(String.format(Locale.ROOT, Commands.USERNAME_COMMAND, type, username));
                sendCommand(String.format(Locale.ROOT, Commands.PASSWORD_COMMAND, type, password));
            } else if ("Private Key".equals(type)) {
                sendCommand(String.format(Locale.ROOT, Commands.PASSWORD_COMMAND, type, password));
            } else {
                throw new UnsupportedOperationException(Constants.MSG_NOT_SUPPORTED_YET);
            }
        }
    }

    private void processState(@NotNull String argument) {
        String[] args = argument.split(",", -1);
        OpenVpnNetworkState state = new OpenVpnNetworkState(args[0], args[1], args[2], args[3], args[4], args[5]);
        String name = state.getName();
        String message = state.getDescription();
        // Workaround for OpenVPN doing AUTH and WAIT while being connected, simply ignore these state
        if ((mStatus == ConnectionStatus.LEVEL_CONNECTED) && (VpnStatus.WAIT.equals(name) || VpnStatus.AUTH.equals(name))) {
            LOGGER.info("Ignoring OpenVPN Status in CONNECTED state ({}->{}): {}", name, mStatus, message);
        } else {
            dispatchOnStateChanged(state);
            mStatus = VpnStatus.getLevel(name, message);
            LOGGER.info("New OpenVPN Status ({}->{}): {}", name, mStatus, message);
        }
    }
}
