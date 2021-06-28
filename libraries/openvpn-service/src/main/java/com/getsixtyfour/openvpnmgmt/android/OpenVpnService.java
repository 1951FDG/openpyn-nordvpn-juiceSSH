package com.getsixtyfour.openvpnmgmt.android;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.text.format.DateUtils;

import androidx.annotation.CheckResult;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.getsixtyfour.openvpnmgmt.R;
import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.core.Commands;
import com.getsixtyfour.openvpnmgmt.core.ConnectionStatus;
import com.getsixtyfour.openvpnmgmt.core.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.core.VpnStatus;
import com.getsixtyfour.openvpnmgmt.listeners.ConnectionStateListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnStateChangedListener;
import com.getsixtyfour.openvpnmgmt.model.OpenVpnLogRecord;
import com.getsixtyfour.openvpnmgmt.model.OpenVpnNetworkState;
import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketTimeoutException;
import java.util.Locale;

import org.jetbrains.annotations.NonNls;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "OverlyComplexClass", "ClassWithTooManyDependencies" })
public final class OpenVpnService extends Service implements ConnectionStateListener, OnByteCountChangedListener, OnStateChangedListener, UncaughtExceptionHandler {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenVpnService.class);

    private static final boolean DEBUG = false;

    // How long the startForegroundService() grace period is to get around to calling startForeground() before we ANR
    private static final int SERVICE_START_FOREGROUND_TIMEOUT = 10 * 1000;

    private final IBinder mBinder = new IOpenVpnService.Stub() {
        @Override
        public void disconnectVpn() {
            Thread thread = new Thread(() -> {
                Process.setThreadPriority(Process.THREAD_PRIORITY_MORE_FAVORABLE);
                try {
                    Connection connection = ManagementConnection.getInstance();
                    connection.sendCommand(String.format(Locale.ROOT, Commands.SIGNAL_COMMAND, Commands.ARG_SIGTERM));
                } catch (IOException ignored) {
                }
            });
            thread.start();
        }
    };

    @Nullable
    private Handler mHandler = null;

    @Nullable
    private Thread mThread = null;

    private boolean mPostByteCountNotification = false;

    private boolean mPostStateNotification = false;

    // TODO: test
    private boolean mSendStateBroadcast = false;

    private int mStartId = 0;

    /**
     * the connection start time in UTC milliseconds (could be some time in the past)
     */
    private long mStartTime = 0L;

    @CheckResult
    @DrawableRes
    @SuppressWarnings({ "MethodWithMultipleReturnPoints", "WeakerAccess" })
    public static int getIconByConnectionStatus(@NonNull ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.ic_stat_shield;
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_shield_check_outline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_NOT_CONNECTED:
            case LEVEL_AUTH_FAILED:
            case LEVEL_UNKNOWN:
            default:
                return R.drawable.ic_stat_shield_outline;
        }
    }

    @CheckResult
    @StringRes
    @SuppressWarnings({ "MethodWithMultipleReturnPoints", "OverlyComplexMethod", "OverlyLongMethod", "WeakerAccess" })
    public static int getLocalizedState(@NonNull String state) {
        switch (state) {
            case VpnStatus.ADD_ROUTES:
                return R.string.vpn_state_add_routes;
            case VpnStatus.ASSIGN_IP:
                return R.string.vpn_state_assign_ip;
            case VpnStatus.AUTH:
                return R.string.vpn_state_auth;
            case VpnStatus.AUTH_FAILED:
                return R.string.vpn_state_auth_failed;
            case VpnStatus.AUTH_PENDING:
                return R.string.vpn_state_auth_pending;
            case VpnStatus.CONNECTED:
                return R.string.vpn_state_connected;
            case VpnStatus.CONNECTING:
                return R.string.vpn_state_connecting;
            case VpnStatus.DISCONNECTED:
                return R.string.vpn_state_disconnected;
            case VpnStatus.EXITING:
                return R.string.vpn_state_exiting;
            case VpnStatus.GET_CONFIG:
                return R.string.vpn_state_get_config;
            case VpnStatus.RECONNECTING:
                return R.string.vpn_state_reconnecting;
            case VpnStatus.RESOLVE:
                return R.string.vpn_state_resolve;
            case VpnStatus.TCP_CONNECT:
                return R.string.vpn_state_tcp_connect;
            case VpnStatus.WAIT:
                return R.string.vpn_state_wait;
            default:
                return R.string.vpn_state_unknown;
        }
    }

    @CheckResult
    @NonNull
    @SuppressWarnings({ "OverlyComplexMethod", "MagicNumber", "ImplicitNumericConversion", "WeakerAccess" })
    public static String humanReadableByteCount(@NonNull Context context, long bytes, boolean speed) {
        Resources res = context.getResources();
        float unit = speed ? 1000.0F : 1024.0F;
        float result = speed ? (bytes << 3) : bytes;
        String units = speed ? res.getString(R.string.bits_per_second) : res.getString(R.string.volume_byte);
        int exp = 0;
        if (result > 900.0F) {
            units = speed ? res.getString(R.string.kbits_per_second) : res.getString(R.string.volume_kbyte);
            exp = 1;
            result /= unit;
        }
        if (result > 900.0F) {
            units = speed ? res.getString(R.string.mbits_per_second) : res.getString(R.string.volume_mbyte);
            exp = 2;
            result /= unit;
        }
        if (result > 900.0F) {
            units = speed ? res.getString(R.string.gbits_per_second) : res.getString(R.string.volume_gbyte);
            exp = 3;
            result /= unit;
        }
        @NonNls String roundFormat = "%.0f";
        if (exp != 0) {
            if (result < 1.0F) {
                roundFormat = "%.2f";
            } else if (result < 10.0F) {
                roundFormat = "%.1f";
            }
        }
        String roundedString = String.format(Locale.ROOT, roundFormat, result);
        return res.getString(R.string.byteSizeSuffix, roundedString, units);
    }

    @Override
    public void onCreate() {
        LOGGER.debug("onCreate");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannels(this);
        }

        mHandler = new Handler(Looper.getMainLooper());

        Connection connection = ManagementConnection.getInstance();
        connection.addOnByteCountChangedListener((in, out, diffIn, diffOut) -> {
            mHandler.post(() -> onByteCountChanged(in, out, diffIn, diffOut));
        });
        connection.addOnRecordChangedListener(record -> {
            mHandler.post(() -> onRecordChanged(record));
        });
        connection.addOnStateChangedListener(state -> {
            mHandler.post(() -> onStateChanged(state));
        });
        connection.setConnectionStateListener(this);
    }

    @SuppressWarnings({ "MethodWithMultipleReturnPoints", "OverlyLongMethod" })
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        LOGGER.debug("onStartCommand");
        if (intent == null) {
            throw new IllegalArgumentException("intent can't be null");
        }

        // Perform the action after return from here, make a delay, otherwise the system might try to restart the
        // service if the process dies before the system realize it's asking for START_NOT_STICKY.
        if (mHandler != null) {
            mHandler.postDelayed(() -> doAction(intent), Constants.WAIT_FOR_SETTLE_DOWN);
        }

        if (mStartId > 0) {
            return START_NOT_STICKY;
        }

        mStartId = startId;

        mPostByteCountNotification = intent.getBooleanExtra(Constants.EXTRA_POST_BYTE_COUNT_NOTIFICATION, false);
        mPostStateNotification = intent.getBooleanExtra(Constants.EXTRA_POST_STATE_NOTIFICATION, false);
        mSendStateBroadcast = intent.getBooleanExtra(Constants.EXTRA_SEND_STATE_BROADCAST, false);

        String host = StringUtils.defaultIfBlank(intent.getStringExtra(Constants.EXTRA_HOST), Constants.DEFAULT_SERVER_IP);
        int port = intent.getIntExtra(Constants.EXTRA_PORT, Constants.DEFAULT_SERVER_PORT);
        char[] password = intent.getCharArrayExtra(Constants.EXTRA_PASSWORD);

        String userName = StringUtils.defaultIfBlank(intent.getStringExtra(Constants.EXTRA_VPN_USERNAME), "");
        String userPass = StringUtils.defaultIfBlank(intent.getStringExtra(Constants.EXTRA_VPN_PASSWORD), "");

        // Always show notification here to avoid problem with startForeground timeout
        String title = getString(R.string.vpn_title_status, getString(getLocalizedState(VpnStatus.DISCONNECTED)));
        String text = getString(R.string.vpn_title_message, host, port);
        long when = mPostByteCountNotification ? 0L : mStartTime;
        int icon = getIconByConnectionStatus(VpnStatus.getLevel(VpnStatus.DISCONNECTED, null));
        Notification notification = createNotification(this, title, text, Constants.NEW_STATUS_CHANNEL_ID, when, icon);
        startForeground(Constants.NEW_STATUS_NOTIFICATION_ID, notification);

        // Start a background thread that handles incoming messages of the management interface
        mThread = new Thread(() -> {
            LOGGER.info("OpenVPN Management started in background thread: \"{}\"", Constants.THREAD_NAME);
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            if (DEBUG) {
                // When a socket is created, it inherits the tag of its creating thread
                TrafficStats.setThreadStatsTag(Constants.THREAD_STATS_TAG);
            }
            Connection connection = ManagementConnection.getInstance();
            connection.setSocketConnectTimeout(ManagementConnection.SOCKET_CONNECT_TIMEOUT);
            connection.setSocketReadTimeout(0);
            connection.setAuthenticationHandler(new OpenVpnHandler(userName, userPass));
            connection.start(host, port, password);
            LOGGER.info("OpenVPN Management stopped in background thread: \"{}\"", Constants.THREAD_NAME);
        }, Constants.THREAD_NAME);

        // Report death-by-uncaught-exception
        mThread.setUncaughtExceptionHandler((t, e) -> {
            mHandler.post(() -> uncaughtException(t, e));
        });
        mThread.start();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LOGGER.debug("onDestroy");

        if (DEBUG) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    long now = System.currentTimeMillis();
                    long start = now - (DateUtils.WEEK_IN_MILLIS << 2);
                    long end = now + (DateUtils.DAY_IN_MILLIS << 1);
                    int uid = Process.myUid();
                    long usage = Utils.getTotalUsage(this, start, end, uid, Constants.THREAD_STATS_TAG);
                    // long usage = Utils.getTotalUsage(this, start, end, uid, android.app.usage.NetworkStats.Bucket.TAG_NONE);
                    LOGGER.info("Usage: {}", humanReadableByteCount(this, usage, false));
                } catch (SecurityException e) {
                    LOGGER.error("Exception querying network detail.", e);
                }
            }
        }

        Connection connection = ManagementConnection.getInstance();
        connection.clearOnByteCountChangedListeners();
        connection.clearOnRecordChangedListeners();
        connection.clearOnStateChangedListeners();
        connection.setConnectionStateListener(null);

        if (mThread != null) {
            mThread.setUncaughtExceptionHandler((t, e) -> {
            });
            mThread.interrupt();
            mThread = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(@Nullable Intent intent) {
        LOGGER.debug("onBind");
        return (intent != null) ? mBinder : null;
    }

    @Override
    public boolean onUnbind(@Nullable Intent intent) {
        LOGGER.debug("onUnbind");
        return false;
    }

    @Override
    public void onConnect(@NonNull Thread t) {
        LOGGER.info("Connected");
    }

    @Override
    public void onDisconnect(@NonNull Thread t) {
        LOGGER.info("Disconnected");
    }

    @Override
    public void onByteCountChanged(long in, long out, long diffIn, long diffOut) {
        if (!mPostByteCountNotification) {
            return;
        }

        long byteCountInterval = ManagementConnection.BYTE_COUNT_INTERVAL.longValue();
        String strIn = humanReadableByteCount(this, in, false);
        String strDiffIn = humanReadableByteCount(this, diffIn / byteCountInterval, true);
        String strOut = humanReadableByteCount(this, out, false);
        String strDiffOut = humanReadableByteCount(this, diffOut / byteCountInterval, true);

        String title = getString(R.string.vpn_title_status, getString(R.string.vpn_state_connected));
        String text = getString(R.string.vpn_msg_byte_count, strIn, strDiffIn, strOut, strDiffOut);
        int icon = getIconByConnectionStatus(ConnectionStatus.LEVEL_CONNECTED);

        Notification notification = createNotification(this, title, text, Constants.BG_CHANNEL_ID, mStartTime, icon);
        startForeground(Constants.BG_NOTIFICATION_ID, notification);
    }

    private static void onRecordChanged(@NonNull OpenVpnLogRecord record) {
        @NonNls Logger logger = LoggerFactory.getLogger("OpenVPN");
        switch (record.getLevel()) {
            case ERROR:
                if (logger.isErrorEnabled()) {
                    logger.error(record.getMessage());
                }
                break;
            case WARNING:
                if (logger.isWarnEnabled()) {
                    logger.warn(record.getMessage());
                }
                break;
            case INFO:
                if (logger.isInfoEnabled()) {
                    logger.info(record.getMessage());
                }
                break;
            case DEBUG:
                if (logger.isDebugEnabled()) {
                    logger.debug(record.getMessage());
                }
                break;
            case VERBOSE:
            case UNKNOWN:
        }
    }

    @Override
    public void onStateChanged(@NonNull OpenVpnNetworkState state) {
        @NonNls String name = state.getName();
        @NonNls String message = state.getDescription();
        @NonNls String address = state.getRemoteAddress();
        @NonNls String port = state.getRemotePort();

        if (mSendStateBroadcast) {
            Intent intent = new Intent();
            intent.setAction(Constants.ACTION_VPN_STATE_CHANGED);
            intent.putExtra(Constants.EXTRA_STATE, name);
            intent.putExtra(Constants.EXTRA_MESSAGE, message);
            //noinspection UnnecessaryFullyQualifiedName
            ((Context) this).sendBroadcast(intent, android.Manifest.permission.ACCESS_NETWORK_STATE);
        }

        boolean isConnected = VpnStatus.CONNECTED.equals(name);
        boolean isDisconnected = VpnStatus.DISCONNECTED.equals(name);
        boolean isExiting = VpnStatus.EXITING.equals(name);

        if (isConnected) {
            mStartTime = state.getMillis();
        }

        if (isExiting) {
            mPostStateNotification = !"exit-with-notification".equals(message);
        }

        if (mPostStateNotification || isConnected || isDisconnected) {
            String title = getString(R.string.vpn_title_status, getString(getLocalizedState(name)));
            String text = message;
            long when = mPostByteCountNotification ? 0L : mStartTime;
            int icon = getIconByConnectionStatus(VpnStatus.getLevel(name, message));

            // (x) optional address of remote server (OpenVPN 2.1 or higher)
            // (y) optional port of remote server (OpenVPN 2.4 or higher)
            // (x) and (y) are shown for ASSIGN_IP and CONNECTED states
            if ((VpnStatus.ASSIGN_IP.equals(name) || VpnStatus.CONNECTED.equals(name)) && !address.isEmpty()) {
                @NonNls String prefix = null;
                if (!port.isEmpty()) {
                    prefix = "1194".equals(port) ? "UDP" : "TCP";
                    prefix += ": ";
                }
                text = prefix + address;
            }

            Notification notification = createNotification(this, title, text, Constants.NEW_STATUS_CHANNEL_ID, when, icon);
            startForeground(Constants.NEW_STATUS_NOTIFICATION_ID, notification);
        }
    }

    /**
     * Logs a message when a thread encounters an uncaught exception
     */
    @SuppressWarnings({ "HardCodedStringLiteral", "HardcodedLineSeparator", "StringBufferWithoutInitialCapacity", "unused" })
    private static void logUncaught(@NonNull String threadName, @Nullable String processName, int pid, @NonNull Throwable e) {
        StringBuilder message = new StringBuilder();
        // The "FATAL EXCEPTION" string is still used on Android even though apps can set a custom
        // UncaughtExceptionHandler that renders uncaught exceptions non-fatal
        message.append("FATAL EXCEPTION: ").append(threadName).append("\n");
        if (processName != null) {
            message.append("Process: ").append(processName).append(", ");
        }
        message.append("PID: ").append(pid);
        LOGGER.error(message.toString(), e);
    }

    @SuppressWarnings({ "OverlyLongMethod", "ImplicitNumericConversion" })
    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        // Apps can replace the default handler, but not the pre handler
        // logUncaught(t.getName(), getPackageName(), Process.myPid(), e);

        if ((e instanceof RuntimeException) && (e.getCause() instanceof IOException)) {
            e = e.getCause();
        }

        if (!(e instanceof ThreadDeath)) {
            Class<? extends Throwable> aClass = e.getClass();
            String title = getString(R.string.vpn_msg_error, aClass.getSimpleName());
            @Nullable @NonNls String bigText = e.getMessage();
            @Nullable @NonNls String text = Utils.getTopLevelCauseMessage(e);
            int icon = getIconByConnectionStatus(ConnectionStatus.LEVEL_UNKNOWN);

            if ((e instanceof SocketTimeoutException) && "Read timed out".equals(bigText)) {
                bigText += String.format(Locale.ROOT, " after %dms", ManagementConnection.SOCKET_READ_TIMEOUT);
                text = bigText;
            }

            if ((text != null) && (bigText != null)) {
                if (text.length() < bigText.length()) {
                    text = text.substring(0, 1).toUpperCase() + text.substring(1);
                    bigText = text + System.lineSeparator() + bigText.substring(0, 1).toUpperCase() + bigText
                            .substring(1, bigText.length() - text.length());
                    bigText = bigText.trim();
                    //noinspection MagicCharacter
                    if (bigText.charAt(bigText.length() - 1) == ':') {
                        bigText = bigText.substring(0, bigText.length() - 1);
                    }
                }
                if (text.length() == bigText.length()) {
                    bigText = null;
                }
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.NEW_STATUS_CHANNEL_ID);
            builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
            builder.setContentText(text);
            builder.setContentTitle(title);
            builder.setLocalOnly(true);
            builder.setOngoing(true);
            builder.setOnlyAlertOnce(true);
            builder.setShowWhen(false);
            builder.setSmallIcon(icon);
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            Intent intent = Utils.getGitHubIntent(this, e);
            if (intent != null) {
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
                builder.addAction(R.drawable.ic_close_white, getString(R.string.vpn_action_issue), pendingIntent);
            }

            intent = new Intent(this, OpenVpnService.class);
            intent.setAction(Constants.ACTION_EXIT);
            intent.putExtra(Constants.EXTRA_ACTION, Constants.TYPE_FINISH);
            PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(R.drawable.ic_close_white, getString(R.string.action_close), pendingIntent);

            Notification notification = builder.build();
            startForeground(Constants.NEW_STATUS_NOTIFICATION_ID, notification);
        }

        if (e instanceof ThreadDeath) {
            stopSelf();
        }
    }

    @NonNull
    @SuppressWarnings({ "TypeMayBeWeakened", "MethodWithTooManyParameters" })
    private static Notification createNotification(@NonNull Context context, @NonNull String title, @Nullable String text,
                                                   @NonNull String channel, long when, @DrawableRes int icon) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channel);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        builder.setContentText(text);
        builder.setContentTitle(title);
        builder.setLocalOnly(true);
        builder.setOngoing(true);
        builder.setOnlyAlertOnce(true);
        builder.setShowWhen(false);
        builder.setSmallIcon(icon);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setWhen(when);

        if ((when > 0L) || Constants.BG_CHANNEL_ID.equals(channel)) {
            Intent intent = new Intent(context, DisconnectActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            // The notification action icons are still required and continue to be used on older versions of Android
            builder.addAction(R.drawable.ic_close_white, context.getString(R.string.vpn_action_close), pendingIntent);
            builder.setUsesChronometer(true);
        }
        return builder.build();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void createNotificationChannels(@NonNull Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        // Real-time notification of OpenVPN bandwidth usage
        {
            String name = context.getString(R.string.vpn_channel_name_background);
            String description = context.getString(R.string.vpn_channel_description_background);
            NotificationChannelCompat.Builder builder = new NotificationChannelCompat.Builder(Constants.BG_CHANNEL_ID, 0);
            builder.setDescription(description);
            builder.setImportance(NotificationManagerCompat.IMPORTANCE_LOW);
            builder.setName(name);
            notificationManager.createNotificationChannel(builder.build());
        }
        // Real-time notification of OpenVPN state changes
        {
            String name = context.getString(R.string.vpn_channel_name_status);
            String description = context.getString(R.string.vpn_channel_description_status);
            NotificationChannelCompat.Builder builder = new NotificationChannelCompat.Builder(Constants.NEW_STATUS_CHANNEL_ID, 0);
            builder.setDescription(description);
            builder.setImportance(NotificationManagerCompat.IMPORTANCE_DEFAULT);
            builder.setName(name);
            notificationManager.createNotificationChannel(builder.build());
        }
    }

    private void doAction(Intent intent) {
        if (Constants.ACTION_EXIT.equals(intent.getAction())) {
            int action = intent.getIntExtra(Constants.EXTRA_ACTION, Constants.TYPE_NONE);
            switch (action) {
                case Constants.TYPE_FINISH:
                    stopSelf();
                    break;
                case Constants.TYPE_EXIT:
                    System.exit(0);
                    break; // Shouldn't be reachable
                case Constants.TYPE_KILL:
                    Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL);
                    break; // Shouldn't be reachable
                case Constants.TYPE_NONE:
                default:
                    break;
            }
        }
    }
}
